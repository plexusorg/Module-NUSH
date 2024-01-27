package dev.plex.listener;

import dev.plex.NUSHModule;
import dev.plex.UserData;
import dev.plex.util.PlexLog;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class ChatListener extends PlexListener
{
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event)
    {
        final Player player = event.getPlayer();

        if (!NUSHModule.isEnabled() || event.isCancelled() || !UserData.isNewPlayer(player))
        {
            PlexLog.debug("NUSH is disabled, event is cancelled or {0} is not on the list", player.getName());
            return;
        }

        PlexLog.debug("Handling event for player {0}", player.getName());
        event.setCancelled(true);
        player.sendMessage(event.renderer().render(player, player.displayName(), event.message(), player));

        Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("plex.nush.view")).forEach(p ->
        {
            Component message = Component.empty().append(Component.text("[NUSH] ").color(NamedTextColor.YELLOW))
                    .append(event.renderer().render(player, player.displayName(), event.message(), p));
            p.sendMessage(message);
        });
    }
}
