package dev.plex;

import dev.plex.command.NUSHCommand;
import dev.plex.api.config.ModuleConfiguration;
import dev.plex.listener.ChatListener;
import dev.plex.listener.JoinListener;
import dev.plex.listener.LoginListener;
import dev.plex.module.PlexModule;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Locale;
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
    public enum KickMode
    {
        OFF, NEW, RECENT
    }

    private static final String BYPASS_PERMISSION = "plex.nush.bypass";

    private ModuleConfiguration config;
    private final Map<UUID, ScheduledFuture<?>> newPlayers = new ConcurrentHashMap<>();
    private ScheduledExecutorService expiryExecutor;
    private Permission permissions;
    private volatile boolean enabled;
    private volatile int time;
    private volatile KickMode kickMode;

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
        RegisteredServiceProvider<Permission> provider = Bukkit.getServicesManager().getRegistration(Permission.class);
        if (provider == null)
        {
            throw new IllegalStateException("NUSH requires a Vault permission provider");
        }
        permissions = provider.getProvider();
        expiryExecutor = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().daemon().name("Plex-NUSH-Expiry").factory());
        config.load();
        enabled = config.getBoolean("server.enabled", false);
        time = config.getInt("server.wait_time", 2);
        String configuredKickMode = config.getString("server.kick_mode", "off");
        try
        {
            kickMode = KickMode.valueOf(configuredKickMode.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ex)
        {
            throw new IllegalStateException("Invalid server.kick_mode '" + configuredKickMode + "'; expected off, new or recent", ex);
        }
        registerListener(new LoginListener(this));
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

    public KickMode getKickMode()
    {
        return kickMode;
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

    public void setKickMode(KickMode mode)
    {
        kickMode = mode;
        config.set("server.kick_mode", mode.name().toLowerCase(Locale.ROOT));
        config.save();
    }

    public boolean bypassesKick(UUID uuid)
    {
        return permissions.playerHas((String) null, Bukkit.getOfflinePlayer(uuid), BYPASS_PERMISSION);
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

    public boolean isNewPlayer(UUID uuid)
    {
        return newPlayers.containsKey(uuid);
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
