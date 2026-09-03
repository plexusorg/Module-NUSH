package dev.plex;

import dev.plex.command.NUSHCommand;
import dev.plex.api.config.ModuleConfiguration;
import dev.plex.listener.ChatListener;
import dev.plex.listener.JoinListener;
import dev.plex.module.PlexModule;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

public class NUSHModule extends PlexModule
{
    private ModuleConfiguration config;
    private final Map<UUID, ScheduledFuture<?>> newPlayers = new ConcurrentHashMap<>();
    private ScheduledExecutorService expiryExecutor;
    private boolean enabled;
    private int time;

    @Override
    public void load()
    {
        config = api().moduleConfigs().create(this, "config.yml");
        loadMessages("messages.yml");
        registerCommand(new NUSHCommand(this));
    }

    @Override
    public void enable()
    {
        expiryExecutor = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().daemon().name("Plex-NUSH-Expiry").factory());
        config.load();
        enabled = config.getBoolean("server.enabled", false);
        time = config.getInt("server.wait_time", 2);
        registerListener(new JoinListener(this));
        registerListener(new ChatListener(this));
    }

    @Override
    public void disable()
    {
        if (expiryExecutor != null)
        {
            expiryExecutor.shutdownNow();
            expiryExecutor = null;
        }
        clearNewPlayers();
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public int getTime()
    {
        return time;
    }

    public void toggle(boolean toggle)
    {
        enabled = toggle;
        config.set("server.enabled", toggle);
        config.save();
    }

    public void setTime(int minutes)
    {
        time = minutes;
        config.set("server.wait_time", minutes);
        config.save();
    }

    public void queueNewPlayer(Player player)
    {
        UUID uuid = player.getUniqueId();
        AtomicReference<ScheduledFuture<?>> taskReference = new AtomicReference<>();
        ScheduledFuture<?> task = expiryExecutor.schedule(
                () -> newPlayers.remove(uuid, taskReference.get()), time, TimeUnit.MINUTES);
        taskReference.set(task);
        ScheduledFuture<?> previous = newPlayers.put(uuid, task);
        if (previous != null)
        {
            previous.cancel(false);
        }
    }

    public boolean isNewPlayer(Player player)
    {
        return newPlayers.containsKey(player.getUniqueId());
    }

    public void removePlayer(Player player)
    {
        ScheduledFuture<?> task = newPlayers.remove(player.getUniqueId());
        if (task != null)
        {
            task.cancel(false);
        }
    }

    public void clearNewPlayers()
    {
        newPlayers.values().forEach(task -> task.cancel(false));
        newPlayers.clear();
    }
}
