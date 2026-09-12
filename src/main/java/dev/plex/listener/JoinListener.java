package dev.plex.listener;

import dev.plex.NUSHModule;
import dev.plex.nush.Quarantine;
import dev.plex.nush.StaffFeed;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

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
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (player.hasPermission(StaffFeed.VIEW_PERMISSION))
        {
            module.feed().addStaff(uuid);
        }

        Quarantine quarantine = module.quarantine();
        Boolean firstJoin = quarantine.consumePending(uuid);
        if (firstJoin == null)
        {
            return;
        }

        if (!quarantine.isRestricted(uuid))
        {
            quarantine.restrict(player, firstJoin);
        }
        module.raidDetector().join();
        module.feed().alert(module.messageComponent("newPlayerMarked",
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("minutes", String.valueOf(module.getTime()))));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        module.feed().removeStaff(event.getPlayer().getUniqueId());
    }
}
