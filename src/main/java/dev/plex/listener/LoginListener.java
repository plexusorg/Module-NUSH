package dev.plex.listener;

import dev.plex.NUSHModule;
import dev.plex.NUSHModule.KickMode;
import dev.plex.nush.Quarantine;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletionException;

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
        if (event.getLoginResult() != Result.ALLOWED || !module.isEnabled())
        {
            return;
        }

        UUID uuid = event.getUniqueId();
        Quarantine quarantine = module.quarantine();
        boolean restricted = quarantine.isRestricted(uuid);
        boolean unverified = restricted || !isVerified(uuid);
        boolean playedBefore = Bukkit.getOfflinePlayer(uuid).hasPlayedBefore();

        if (unverified && !restricted && playedBefore)
        {
            quarantine.markVerified(uuid);
            unverified = false;
        }
        if (unverified && module.bypassesKick(uuid))
        {
            unverified = false;
        }
        if (!unverified)
        {
            return;
        }

        KickMode mode = module.getKickMode();
        if (mode == KickMode.RECENT || (mode == KickMode.NEW && !playedBefore))
        {
            event.disallow(Result.KICK_OTHER, module.messageComponent("kickDenied"));
            module.feed().alert(module.messageComponent("newPlayerKicked",
                    Placeholder.unparsed("player", event.getName()),
                    Placeholder.unparsed("mode", mode.name().toLowerCase(Locale.ROOT))));
            return;
        }

        quarantine.markPending(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLoginResult(AsyncPlayerPreLoginEvent event)
    {
        if (event.getLoginResult() != Result.ALLOWED)
        {
            module.quarantine().clearPending(event.getUniqueId());
        }
    }

    private boolean isVerified(UUID uuid)
    {
        try
        {
            return module.api().players().moduleData(module, uuid).getBoolean("verified", false).join();
        }
        catch (CompletionException failure)
        {
            module.getLogger().error("Unable to read the NUSH verification of {}", uuid, failure);
            return false;
        }
    }
}
