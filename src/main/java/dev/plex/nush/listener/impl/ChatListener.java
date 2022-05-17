package dev.plex.nush.listener.impl;

import dev.plex.Plex;
import dev.plex.cache.DataUtils;
import dev.plex.listener.PlexListener;
import dev.plex.nush.Message;
import dev.plex.nush.NushAction;
import dev.plex.nush.NushModule;
import dev.plex.nush.handler.impl.ActionHandler;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.RankManager;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class ChatListener extends PlexListener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChat(AsyncChatEvent event) {
		if(event.isCancelled()) return;
		Player player = event.getPlayer();
		Instant firstJoined = Instant.ofEpochMilli(player.getFirstPlayed());
		Instant rightNow = Instant.now();
		long difference = (Duration.between(firstJoined, rightNow).getSeconds() / 60);
		if (difference >= 15) {
			PlexLog.debug("{0} has been on the server for {1} minutes, so Nush will skip them.", player.getName(), difference);
			return;
		}

		NushModule module = NushModule.getInstance();
		Plex plex = module.getPlex();
		PlexPlayer plexPlayer = DataUtils.getPlayer(player.getUniqueId());
		RankManager rankManager = plex.getRankManager();

		if (rankManager.isAdmin(plexPlayer)) {
			PlexLog.debug("{0} is an admin so Nush will skip them.", player.getName());
			return; // we needn't process the chat message if they're an admin
		}

		event.setCancelled(true);
		UUID key = UUID.randomUUID();
		Message message = new Message(event.getPlayer().getUniqueId(), event.originalMessage());
		ActionHandler.MAP.put(key, message);
		Component component = ActionHandler.getMessage(message);

		// Send the user the message so they think it got sent
		player.sendMessage(component);

		component = component.append(Component.text("\n"));

		for (NushAction value : NushAction.values()) {
			String command = String.format("/nush work %s %d", key, value.ordinal);
			component = component.append(
				Component.text(String.format("[%s] ", value.humanReadable))
					.clickEvent(ClickEvent.runCommand(command))
					.hoverEvent(
						Component.text(command, NamedTextColor.YELLOW)
					)
			);
		}

		PlexUtils.broadcastToAdmins(component);
	}
}
