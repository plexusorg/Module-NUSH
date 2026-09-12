package dev.plex.listener;

import dev.plex.NUSHModule;
import dev.plex.nush.Quarantine.Kind;
import dev.plex.nush.Quarantine.LogEntry;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.time.Instant;
import java.util.UUID;

public class ChatListener implements Listener
{
    private final NUSHModule module;

    public ChatListener(NUSHModule module)
    {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event)
    {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!module.isEnabled() || !module.quarantine().isRestricted(uuid))
        {
            return;
        }

        // Cancelling stops every MONITOR consumer, such as DiscordSRV, from reading the raw message.
        event.setCancelled(true);
        Component rendered = event.renderer().render(player, player.displayName(), event.message(), player);
        player.sendMessage(rendered);

        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        module.quarantine().record(uuid, new LogEntry(Kind.CHAT, Instant.now(), text));
        module.raidDetector().chat(uuid);
        module.feed().line(player, Kind.CHAT, rendered, text);
    }
}
