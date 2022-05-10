package dev.plex.nush.listener.impl;

import dev.plex.Plex;
import dev.plex.cache.DataUtils;
import dev.plex.listener.PlexListener;
import dev.plex.nush.NushModule;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.RankManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener extends PlexListener {

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		NushModule module = NushModule.getInstance();
		Plex plex = module.getPlex();
		PlexPlayer plexPlayer = DataUtils.getPlayer(player.getUniqueId());
		RankManager rankManager = plex.getRankManager();

		if (!rankManager.isAdmin(plexPlayer)) {
			return; // we only want to add admins
		}
        /*if (ChatListener.work.containsKey())
        {

        }*/
	}
}
