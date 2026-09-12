package dev.plex.listener;

import dev.plex.NUSHModule;
import dev.plex.NUSHModule.KickMode;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Locale;
import java.util.UUID;

public class LoginListener implements Listener
{
    private final NUSHModule module;

    public LoginListener(NUSHModule module)
    {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event)
    {
        KickMode mode = module.getKickMode();
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED || !module.isEnabled() || mode == KickMode.OFF)
        {
            return;
        }
        UUID uuid = event.getUniqueId();
        if (module.bypassesKick(uuid))
        {
            return;
        }
        boolean firstJoin = !Bukkit.getOfflinePlayer(uuid).hasPlayedBefore();
        if (!firstJoin && !(mode == KickMode.RECENT && module.isNewPlayer(uuid)))
        {
            return;
        }
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, module.messageComponent("kickDenied"));
        Bukkit.broadcast(module.messageComponent("newPlayerKicked", Placeholder.parsed("player", event.getName()),
                Placeholder.unparsed("mode", mode.name().toLowerCase(Locale.ROOT))), "plex.nush.view");
    }
}
