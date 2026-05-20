package dev.plex.command;

import dev.plex.NUSHModule;
import dev.plex.UserData;
import dev.plex.command.SimplePlexCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    protected Component execute(@NotNull CommandSender sender, @Nullable Player player, @NotNull String[] args)
    {
        if (args.length == 1)
        {
            switch (args[0].toLowerCase())
            {
                case "on" ->
                {
                    module.toggle(true);
                    return messageComponent("nushEnabled");
                }

                case "off" ->
                {
                    module.toggle(false);
                    UserData.clear();
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
        else if (args.length == 2)
        {
            switch (args[0].toLowerCase())
            {
                case "time" ->
                {
                    int time;
                    try
                    {
                        time = Integer.parseInt(args[1]);
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
                    final Player target = getNonNullPlayer(args[1]);
                    if (UserData.isNewPlayer(target))
                    {
                        UserData.removePlayer(target);
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

        return usage();
    }

    @Override
    protected @NotNull List<String> suggestions(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args)
    {
        if (args.length == 1 && silentCheckPermission(sender, this.getPermission()))
        {
            return Arrays.asList("on", "off", "status", "time", "remove");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("remove") && silentCheckPermission(sender, this.getPermission()))
        {
            return onlinePlayerNames();
        }
        return Collections.emptyList();
    }
}
