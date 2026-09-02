package dev.plex.listener;

import dev.plex.NUSHModule;
import dev.plex.UserData;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.chat.ChatRenderer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener
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
        ChatRenderer renderer = event.renderer();
        event.viewers().removeIf(viewer -> !(viewer instanceof Player recipient)
                || (!recipient.equals(player) && !recipient.hasPermission("plex.nush.view")));
        event.viewers().add(player);
        event.renderer((source, displayName, message, viewer) -> render(renderer, source, displayName, message, viewer));
    }

    private Component render(ChatRenderer renderer, Player source, Component displayName, Component message, Audience viewer)
    {
        Component rendered = renderer.render(source, displayName, message, viewer);
        return source.equals(viewer) ? rendered : module.messageComponent("newPlayerChatPrefix").append(rendered);
    }
}
