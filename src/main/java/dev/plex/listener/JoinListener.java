package dev.plex.listener;

import dev.plex.NUSHModule;
import dev.plex.UserData;
import dev.plex.util.PlexLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener extends PlexListener
{
    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        final Player player = event.getPlayer();
        if (!player.hasPlayedBefore() && NUSHModule.isEnabled())
        {
            PlexLog.debug("Adding {0} to the new player list", player.getName());
            UserData.queueNewPlayer(player);
            Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("plex.nush.view")).forEach(p ->
                    p.sendMessage(Component.text("[NUSH] " + player.getName()
                                    + " has been marked as a new player and won't be able to chat normally for " + NUSHModule.getTime() + " minutes.")
                            .color(NamedTextColor.LIGHT_PURPLE)));
        }
    }
}
