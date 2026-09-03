package dev.plex.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.NUSHModule;
import dev.plex.command.SimplePlexCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import io.papermc.paper.command.brigadier.CommandSourceStack;

public class NUSHCommand extends SimplePlexCommand
{
    private final NUSHModule module;

    public NUSHCommand(NUSHModule module)
    {
        super(command("nush")
                .description("The main command to manage the NUSH module")
                .usage("/<command> <on | off | status | time <minutes> | remove <player>>")
                .permission("plex.nush.use")
                .build());
        this.module = module;
    }

    @Override
    protected void configureCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, (sender, player) -> usage()));
        command.then(word("action")
                .suggests((context, builder) -> suggestMatching(builder, List.of("on", "off", "status", "time", "remove")))
                .executes(context -> executeCommand(context,
                        (sender, player) -> executeAction(sender, string(context, "action"), null)))
                .then(word("value")
                        .suggests((context, builder) ->
                        {
                            return string(context, "action").equalsIgnoreCase("remove")
                                    ? suggestMatching(builder, onlinePlayerNames()) : builder.buildFuture();
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
                    return messageComponent("nushStatus", module.isEnabled() ? "<green>enabled</green>" : "<red>disabled</red>");
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
                    return messageComponent("waitTimeSet", time);
                }

                case "remove" ->
                {
                    final Player target = getNonNullPlayer(value);
                    if (module.isNewPlayer(target))
                    {
                        module.removePlayer(target);
                        return messageComponent("playerRemoved", target.getName());
                    }
                    else
                    {
                        return messageComponent("playerNotNushed");
                    }
                }

                default ->
                {
                    return usage();
                }
            }
        }
    }
}
