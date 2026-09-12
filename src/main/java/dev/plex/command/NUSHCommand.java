package dev.plex.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.NUSHModule;
import dev.plex.NUSHModule.KickMode;
import dev.plex.command.SimplePlexCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import io.papermc.paper.command.brigadier.CommandSourceStack;

public class NUSHCommand extends SimplePlexCommand
{
    private static final List<String> KICK_MODES = List.of("off", "new", "recent");

    private final NUSHModule module;

    public NUSHCommand(NUSHModule module)
    {
        super(command("nush")
                .description("The main command to manage the NUSH module")
                .usage("/<command> <on | off | status | time <minutes> | remove <player> | kick [off | new | recent]>")
                .permission("plex.nush.use")
                .build());
        this.module = module;
    }

    @Override
    protected void configureCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, (sender, player) -> usage()));
        command.then(word("action")
                .suggests((context, builder) -> suggestMatching(builder, List.of("on", "off", "status", "time", "remove", "kick")))
                .executes(context -> executeCommand(context,
                        (sender, player) -> executeAction(sender, string(context, "action"), null)))
                .then(word("value")
                        .suggests((context, builder) ->
                        {
                            String action = string(context, "action").toLowerCase(Locale.ROOT);
                            if (action.equals("remove"))
                            {
                                return suggestMatching(builder, onlinePlayerNames());
                            }
                            if (action.equals("kick"))
                            {
                                return suggestMatching(builder, KICK_MODES);
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> executeCommand(context, (sender, player) -> executeAction(sender,
                                string(context, "action"), string(context, "value"))))
                        .then(greedyString("extra").executes(context ->
                                executeCommand(context, (sender, player) -> usage())))));
    }

    private Component executeAction(CommandSender sender, String action, @Nullable String value)
    {
        if (value == null)
        {
            switch (action.toLowerCase())
            {
                case "on" ->
                {
                    module.toggle(true);
                    return messageComponent("nushEnabled");
                }

                case "off" ->
                {
                    module.toggle(false);
                    module.clearNewPlayers();
                    return messageComponent("nushDisabled");
                }

                case "status" ->
                {
                    return messageComponent("nushStatus", Placeholder.parsed("status", module.isEnabled() ? "<green>enabled</green>" : "<red>disabled</red>"),
                            Placeholder.unparsed("kick", kickModeName()));
                }

                case "kick" ->
                {
                    return messageComponent("kickModeStatus", Placeholder.unparsed("mode", kickModeName()));
                }

                default ->
                {
                    return usage();
                }
            }
        }
        else
        {
            switch (action.toLowerCase())
            {
                case "time" ->
                {
                    int time;
                    try
                    {
                        time = Integer.parseInt(value);
                    }
                    catch (NumberFormatException ex)
                    {
                        return messageComponent("timeMustBeNumber");
                    }

                    module.setTime(time);
                    return messageComponent("waitTimeSet", Placeholder.unparsed("minutes", String.valueOf(time)));
                }

                case "remove" ->
                {
                    final Player target = getNonNullPlayer(value);
                    if (module.isNewPlayer(target.getUniqueId()))
                    {
                        module.removePlayer(target);
                        return messageComponent("playerRemoved", Placeholder.parsed("player", target.getName()));
                    }
                    else
                    {
                        return messageComponent("playerNotNushed");
                    }
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

                default ->
                {
                    return usage();
                }
            }
        }
    }

    private String kickModeName()
    {
        return module.getKickMode().name().toLowerCase(Locale.ROOT);
    }
}
