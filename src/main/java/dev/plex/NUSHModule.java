package dev.plex;

import dev.plex.command.NUSHCommand;
import dev.plex.api.config.ModuleConfiguration;
import dev.plex.listener.ChatListener;
import dev.plex.listener.JoinListener;
import dev.plex.module.PlexModule;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class NUSHModule extends PlexModule
{
    private ModuleConfiguration config;
    private final Map<UUID, ScheduledTask> newPlayers = new ConcurrentHashMap<>();
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
        config.load();
        enabled = config.getBoolean("server.enabled", false);
        time = config.getInt("server.wait_time", 2);
        registerListener(new JoinListener(this));
        registerListener(new ChatListener(this));
    }

    @Override
    public void disable()
    {
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
        ScheduledTask task = scheduler().runAsyncLater(
                scheduledTask -> newPlayers.remove(uuid, scheduledTask), time, TimeUnit.MINUTES);
        ScheduledTask previous = newPlayers.put(uuid, task);
        if (previous != null)
        {
            previous.cancel();
        }
    }

    public boolean isNewPlayer(Player player)
    {
        return newPlayers.containsKey(player.getUniqueId());
    }

    public void removePlayer(Player player)
    {
        ScheduledTask task = newPlayers.remove(player.getUniqueId());
        if (task != null)
        {
            task.cancel();
        }
    }

    public void clearNewPlayers()
    {
        newPlayers.values().forEach(ScheduledTask::cancel);
        newPlayers.clear();
    }
}
