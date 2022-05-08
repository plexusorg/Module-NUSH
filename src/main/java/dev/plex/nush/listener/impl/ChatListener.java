package dev.plex.nush.listener.impl;

import dev.plex.Plex;
import dev.plex.admin.Admin;
import dev.plex.cache.DataUtils;
import dev.plex.listener.PlexListener;
import dev.plex.nush.NushModule;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.RankManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class ChatListener extends PlexListener {
	public static final Map<Admin, Integer> work = new HashMap<>();

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChat(AsyncChatEvent event) {
		Player player = event.getPlayer();
		Instant firstJoined = Instant.ofEpochMilli(player.getFirstPlayed());
		Instant rightNow = Instant.now();
		long difference = (Duration.between(firstJoined, rightNow).getSeconds() / 60);
		if(difference >= 15) return;

		NushModule module = NushModule.getInstance();
		Plex plex = module.getPlex();
		PlexPlayer plexPlayer = DataUtils.getPlayer(player.getUniqueId());
		RankManager rankManager = plex.getRankManager();

		if(rankManager.isAdmin(plexPlayer)) return; // we needn't process the chat message if they're an admin
		Entry<Admin, Integer> leastWork = null;

		for (Entry<Admin, Integer> adminIntegerEntry : work.entrySet()) {
			if(leastWork == null) {
				leastWork = adminIntegerEntry;
				return;
			} else {
				if(leastWork.getValue() > adminIntegerEntry.getValue()) {
					leastWork = adminIntegerEntry;
				}
			}
		}
	}
}
