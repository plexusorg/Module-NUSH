package dev.plex.listener;

import dev.plex.NUSHModule;
import dev.plex.UserData;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class ChatListener extends PlexListener
{
    private final NUSHModule module;

    public ChatListener(NUSHModule module)
    {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event)
    {
        final Player player = event.getPlayer();

        if (!module.isEnabled() || event.isCancelled() || !UserData.isNewPlayer(player))
        {
            module.api().logging().debug("NUSH is disabled, event is cancelled or {0} is not on the list", player.getName());
            return;
        }

        module.api().logging().debug("Handling event for player {0}", player.getName());
        event.setCancelled(true);
        player.sendMessage(event.renderer().render(player, player.displayName(), event.message(), player));

        Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("plex.nush.view")).forEach(p ->
        {
            Component message = module.messageComponent("newPlayerChatPrefix")
                    .append(event.renderer().render(player, player.displayName(), event.message(), p));
            p.sendMessage(message);
        });
    }
}
