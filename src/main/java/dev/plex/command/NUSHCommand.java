package dev.plex.command;

import dev.plex.NUSHModule;
import dev.plex.UserData;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CommandParameters(name = "nush", usage = "/<command> <on | off | status | time <minutes> | remove <player>>", description = "The main command to manage the NUSH module")
@CommandPermissions(permission = "plex.nush.use")
public class NUSHCommand extends PlexCommand
{

    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player player, @NotNull String[] args)
    {
        if (args.length == 1)
        {
            switch (args[0].toLowerCase())
            {
                case "on" ->
                {
                    NUSHModule.toggle(true);
                    return MiniMessage.miniMessage().deserialize("<gray>The status for NUSH is now <green>enabled</green>.");
                }

                case "off" ->
                {
                    NUSHModule.toggle(false);
                    UserData.clear();
                    return MiniMessage.miniMessage().deserialize("<gray>The status for NUSH is now <red>disabled</red>.");
                }

                case "status" ->
                {
                    return MiniMessage.miniMessage().deserialize("<gray>The status for NUSH is currently " + (NUSHModule.isEnabled() ? "<green>enabled</green>" : "<red>disabled</red>") + ".");
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
                        return MiniMessage.miniMessage().deserialize("<red>The time must be a number!");
                    }

                    NUSHModule.getConfig().set("server.wait_time", time);
                    return MiniMessage.miniMessage().deserialize("<gray>The wait time for new players before they can chat is now set to <yellow>" + time + "</yellow> minutes.");
                }

                case "remove" ->
                {
                    final Player target = getNonNullPlayer(args[1]);
                    if (UserData.isNewPlayer(target))
                    {
                        UserData.removePlayer(target);
                        return MiniMessage.miniMessage().deserialize("<yellow>" + target.getName() + " <gray>has been removed.");
                    }
                    else
                    {
                        return MiniMessage.miniMessage().deserialize("<red>That player is currently not NUSH'd");
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
    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args)
    {
        if (args.length == 1 && silentCheckPermission(sender, this.getPermission()))
        {
            return Arrays.asList("on", "off", "status", "time", "remove");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("remove") && silentCheckPermission(sender, this.getPermission()))
        {
            return PlexUtils.getPlayerNameList();
        }
        return Collections.emptyList();
    }
}
