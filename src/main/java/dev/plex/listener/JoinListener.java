package dev.plex.listener;

import dev.plex.NUSHModule;
import dev.plex.UserData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener
{
    private final NUSHModule module;

    public JoinListener(NUSHModule module)
    {
        this.module = module;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        final Player player = event.getPlayer();
        if (!player.hasPlayedBefore() && module.isEnabled())
        {
            module.api().logging().debug("Adding {0} to the new player list", player.getName());
            UserData.queueNewPlayer(module, player);
            Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("plex.nush.view")).forEach(p ->
                    p.sendMessage(module.messageComponent("newPlayerMarked", player.getName(), module.getTime())));
        }
    }
}
