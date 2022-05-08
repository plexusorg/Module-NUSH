package dev.plex.nush.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.nush.NushModule;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "nush", aliases = "raidmode", description = "Toggle NUSH on or off.", usage = "/<command> [on | enable | off | disable | toggle]")
@CommandPermissions(level = Rank.ADMIN, permission = "plex.nush.command")
public class NUSHCommand extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender commandSender, @Nullable Player player, @NotNull String[] args)
    {
        if(args.length == 0) {
            NushModule.enabled = !NushModule.enabled;
        } else if (args.length > 1) {
            switch (args[0].toLowerCase()) {
                case "on", "enable" -> NushModule.enabled = true;
                case "off", "disable" -> NushModule.enabled = false;
                case "toggle" -> NushModule.enabled = !NushModule.enabled;
            }
        }

        PlexUtils.broadcastToAdmins(messageComponent("nushToggled", commandSender.getName(), NushModule.enabled ? "Enabling" : "Disabling"));
        return null;
    }
}
