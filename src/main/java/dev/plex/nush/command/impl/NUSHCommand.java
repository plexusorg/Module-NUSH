package dev.plex.nush.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.nush.Message;
import dev.plex.nush.NushAction;
import dev.plex.nush.NushModule;
import dev.plex.nush.handler.impl.ActionHandler;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static dev.plex.nush.NushAction.ACCEPT;
import static dev.plex.nush.NushAction.CANCEL;

@CommandParameters(name = "nush", aliases = "raidmode", description = "Toggle NUSH on or off.", usage = "/<command> [on | enable | off | disable | toggle]")
@CommandPermissions(level = Rank.ADMIN, permission = "plex.nush.command")
public class NUSHCommand extends PlexCommand {

	@Override
	protected Component execute(@NotNull CommandSender commandSender, @Nullable Player player,
		@NotNull String[] args) {
		if (args.length == 0) {
			NushModule.enabled = !NushModule.enabled;
		} else if (args.length == 1) {
			switch (args[0].toLowerCase()) {
				case "on", "enable" -> NushModule.enabled = true;
				case "off", "disable" -> NushModule.enabled = false;
				case "toggle" -> NushModule.enabled = !NushModule.enabled;
			}
		} else {
			if (args[0].equalsIgnoreCase("work")) {
				try {
					UUID nushIdentifier = UUID.fromString(args[1]);
					Message nushMessage = ActionHandler.MAP.get(nushIdentifier);

					if (nushMessage == null) {
						return null;
					}

					NushAction action = NushAction.fromOrdinal(Integer.parseInt(args[2]));
					if (action == null) {
						return null;
					}

					if (action == ACCEPT || action == CANCEL) {
						ActionHandler.resolve(nushIdentifier, action);
						return Component.text(action.humanReadable, NamedTextColor.YELLOW);
					}

					StringBuilder command = new StringBuilder();

					command.append(action.name().toLowerCase());
					command.append(" ");
					command.append(nushMessage.getSender());

					if (!command.toString().trim().isEmpty()) {
						PlexLog.debug("Dispatching command: {0}", command.toString());
						Bukkit.dispatchCommand(commandSender, command.toString());
					}

					ActionHandler.resolve(nushIdentifier, action);
					return Component.text(action.humanReadable, NamedTextColor.YELLOW);
				} catch (Exception ignored) {
					return null;
				}
			}

			return null;
		}

		PlexUtils.broadcastToAdmins(messageComponent("nushToggled", commandSender.getName(),
			NushModule.enabled ? "Enabling" : "Disabling"));
		return null;
	}
}
