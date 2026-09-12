package dev.plex.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.plex.NUSHModule;
import dev.plex.NUSHModule.KickMode;
import dev.plex.nush.Quarantine.LogEntry;
import dev.plex.nush.Quarantine.Restriction;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class NUSHCommand extends SimplePlexCommand
{
    private static final List<String> ACTIONS = List.of("on", "off", "status", "time", "kick", "allow", "revoke",
            "list", "log", "feed");
    private static final List<String> KICK_MODES = List.of("off", "new", "recent");
    private static final String ALLOW_PERMISSION = "plex.nush.allow";

    private final NUSHModule module;

    public NUSHCommand(NUSHModule module)
    {
        super(command("nush")
                .description("The main command to manage the NUSH module")
                .usage("/<command> <on | off | status | time <minutes> | kick [off | new | recent] "
                        + "| allow <player> | revoke <player> | list | log <player> | feed>")
                .permission("plex.nush.use")
                .build());
        this.module = module;
    }

    @Override
    protected void configureCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, (sender, player) -> usage()));
        command.then(word("action")
                .suggests((context, builder) -> suggestMatching(builder, ACTIONS))
                .executes(context -> executeCommand(context,
                        (sender, player) -> executeAction(sender, string(context, "action"), null)))
                .then(word("value")
                        .suggests((context, builder) -> suggestValues(builder, string(context, "action")))
                        .executes(context -> executeCommand(context, (sender, player) -> executeAction(sender,
                                string(context, "action"), string(context, "value"))))
                        .then(greedyString("extra").executes(context ->
                                executeCommand(context, (sender, player) -> usage())))));
    }

    private CompletableFuture<Suggestions> suggestValues(SuggestionsBuilder builder, String action)
    {
        String normalized = action.toLowerCase(Locale.ROOT);
        if (normalized.equals("kick"))
        {
            return suggestMatching(builder, KICK_MODES);
        }
        if (normalized.equals("allow"))
        {
            Set<String> names = new LinkedHashSet<>(onlinePlayerNames());
            names.addAll(restrictedNames());
            return suggestMatching(builder, names);
        }
        if (normalized.equals("revoke"))
        {
            return suggestMatching(builder, onlinePlayerNames());
        }
        if (normalized.equals("log"))
        {
            return suggestMatching(builder, restrictedNames());
        }
        return builder.buildFuture();
    }

    private Component executeAction(CommandSender sender, String action, @Nullable String value)
    {
        if (value == null)
        {
            switch (action.toLowerCase(Locale.ROOT))
            {
                case "on" ->
                {
                    module.toggle(true);
                    return messageComponent("nushEnabled");
                }

                case "off" ->
                {
                    module.toggle(false);
                    module.quarantine().clear();
                    return messageComponent("nushDisabled");
                }

                case "status" ->
                {
                    return messageComponent("nushStatus",
                            Placeholder.parsed("status", module.isEnabled() ? "<green>enabled</green>" : "<red>disabled</red>"),
                            Placeholder.unparsed("kick", kickModeName()),
                            Placeholder.unparsed("restricted", String.valueOf(module.quarantine().entries().size())));
                }

                case "kick" ->
                {
                    return messageComponent("kickModeStatus", Placeholder.unparsed("mode", kickModeName()));
                }

                case "list" ->
                {
                    return list(sender);
                }

                case "feed" ->
                {
                    return toggleFeed(sender);
                }

                default ->
                {
                    return usage();
                }
            }
        }

        switch (action.toLowerCase(Locale.ROOT))
        {
            case "time" ->
            {
                return setTime(value);
            }

            case "kick" ->
            {
                if (!KICK_MODES.contains(value.toLowerCase(Locale.ROOT)))
                {
                    return messageComponent("kickModeInvalid");
                }
                module.setKickMode(KickMode.valueOf(value.toUpperCase(Locale.ROOT)));
                return messageComponent("kickModeSet", Placeholder.unparsed("mode", kickModeName()));
            }

            case "allow" ->
            {
                return allow(sender, value);
            }

            case "revoke" ->
            {
                return revoke(sender, value);
            }

            case "log" ->
            {
                return log(sender, value);
            }

            default ->
            {
                return usage();
            }
        }
    }

    private Component setTime(String value)
    {
        int minutes;
        try
        {
            minutes = Integer.parseInt(value);
        }
        catch (NumberFormatException ex)
        {
            return messageComponent("timeMustBeNumber");
        }

        if (minutes < 1)
        {
            return messageComponent("timeMinimum");
        }
        module.setTime(minutes);
        return messageComponent("waitTimeSet", Placeholder.unparsed("minutes", String.valueOf(minutes)));
    }

    private Component allow(CommandSender sender, String name)
    {
        checkPermission(sender, ALLOW_PERMISSION);
        Restriction restriction = module.quarantine().byName(name);
        if (restriction == null)
        {
            return messageComponent("playerNotNushed");
        }

        String admin = sender.getName();
        module.quarantine().verify(restriction.uuid(), admin)
                .whenComplete((ignored, failure) -> sender.sendMessage(messageComponent("playerAllowed",
                        Placeholder.unparsed("player", restriction.name()),
                        Placeholder.unparsed("admin", admin))));
        return null;
    }

    private Component revoke(CommandSender sender, String name)
    {
        checkPermission(sender, ALLOW_PERMISSION);
        Player target = getNonNullPlayer(name);
        module.quarantine().revoke(target.getUniqueId());
        Component revoked = messageComponent("playerRevoked", Placeholder.unparsed("player", target.getName()),
                Placeholder.unparsed("admin", sender.getName()));
        module.feed().alert(revoked);
        return revoked;
    }

    private Component list(CommandSender sender)
    {
        Collection<Restriction> restrictions = module.quarantine().entries();
        if (restrictions.isEmpty())
        {
            return messageComponent("listEmpty");
        }

        sender.sendMessage(messageComponent("listHeader"));
        for (Restriction restriction : restrictions)
        {
            long minutes = (restriction.remainingSeconds() + 59) / 60;
            sender.sendMessage(messageComponent("listEntry",
                    Placeholder.unparsed("player", restriction.name()),
                    Placeholder.unparsed("state", Bukkit.getPlayer(restriction.uuid()) == null ? "offline" : "online"),
                    Placeholder.unparsed("minutes", String.valueOf(minutes)),
                    Placeholder.unparsed("messages", String.valueOf(restriction.messages())),
                    Placeholder.unparsed("commands", String.valueOf(restriction.blockedCommands()))));
        }
        return null;
    }

    private Component log(CommandSender sender, String name)
    {
        Restriction restriction = module.quarantine().byName(name);
        if (restriction == null)
        {
            return messageComponent("playerNotNushed");
        }

        sender.sendMessage(messageComponent("logHeader", Placeholder.unparsed("player", restriction.name())));
        Instant now = Instant.now();
        for (LogEntry entry : restriction.log())
        {
            sender.sendMessage(messageComponent("logEntry",
                    Placeholder.unparsed("ago", String.valueOf(Duration.between(entry.time(), now).toSeconds())),
                    Placeholder.unparsed("kind", entry.kind().name().toLowerCase(Locale.ROOT)),
                    Placeholder.unparsed("text", entry.text())));
        }
        return null;
    }

    private Component toggleFeed(CommandSender sender)
    {
        if (isConsole(sender))
        {
            return messageComponent("noPermissionConsole");
        }
        return messageComponent(module.feed().toggleMute(getUUID(sender)) ? "feedMuted" : "feedUnmuted");
    }

    private Set<String> restrictedNames()
    {
        Set<String> names = new LinkedHashSet<>();
        for (Restriction restriction : module.quarantine().entries())
        {
            names.add(restriction.name());
        }
        return names;
    }

    private String kickModeName()
    {
        return module.getKickMode().name().toLowerCase(Locale.ROOT);
    }
}
