package dev.plex.listener;

import dev.plex.NUSHModule;
import dev.plex.nush.Quarantine;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

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
        if (!restricted && isVerified(uuid))
        {
            return;
        }
        boolean playedBefore = Bukkit.getOfflinePlayer(uuid).hasPlayedBefore();
        if (!restricted && playedBefore)
        {
            quarantine.markVerified(uuid);
            return;
        }
        if (module.bypassesRestriction(uuid))
        {
            return;
        }
        quarantine.markPending(uuid, !playedBefore);
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
