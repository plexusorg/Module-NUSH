package dev.plex.listener;

import dev.plex.NUSHModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

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
        if (event.getLoginResult() != Result.ALLOWED)
        {
            return;
        }

        module.quarantine().prepareLogin(event.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLoginResult(AsyncPlayerPreLoginEvent event)
    {
        if (event.getLoginResult() != Result.ALLOWED)
        {
            module.quarantine().clearPending(event.getUniqueId());
        }
    }
}
