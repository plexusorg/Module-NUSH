package dev.plex;

import dev.plex.api.config.ModuleConfiguration;
import dev.plex.command.NUSHCommand;
import dev.plex.listener.ChatListener;
import dev.plex.listener.CommandListener;
import dev.plex.listener.JoinListener;
import dev.plex.listener.LoginListener;
import dev.plex.module.PlexModule;
import dev.plex.nush.FaweHook;
import dev.plex.nush.Quarantine;
import dev.plex.nush.RaidDetector;
import dev.plex.nush.StaffFeed;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class NUSHModule extends PlexModule
{
    private static final String BYPASS_PERMISSION = "plex.nush.bypass";
    private static final String FAWE_PLUGIN = "FastAsyncWorldEdit";

    private ModuleConfiguration config;
    private ScheduledExecutorService executor;
    private Quarantine quarantine;
    private StaffFeed feed;
    private RaidDetector raidDetector;
    private FaweHook faweHook;
    private final AtomicBoolean missingPermissionsReported = new AtomicBoolean();
    private volatile boolean enabled;
    private volatile int time;
    private volatile boolean shadowActive;

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
        time = requireAtLeastOne("server.wait_time", config.getInt("server.wait_time", 5));
        int intervalSeconds = requireAtLeastOne("feed.interval_seconds", config.getInt("feed.interval_seconds", 10));
        int digestThreshold = requireAtLeastOne("feed.digest_threshold", config.getInt("feed.digest_threshold", 5));
        int logSize = requireAtLeastOne("log.size", config.getInt("log.size", 50));
        int windowSeconds = requireAtLeastOne("raid.window_seconds", config.getInt("raid.window_seconds", 60));
        int joinThreshold = requireAtLeastOne("raid.join_threshold", config.getInt("raid.join_threshold", 10));
        int chatThreshold = requireAtLeastOne("raid.chat_threshold", config.getInt("raid.chat_threshold", 10));

        executor = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().daemon().name("Plex-NUSH").factory());
        feed = new StaffFeed(this, executor, intervalSeconds, digestThreshold);
        quarantine = new Quarantine(this, feed, executor, logSize);
        raidDetector = new RaidDetector(this, feed, windowSeconds, joinThreshold, chatThreshold);

        registerListener(new LoginListener(this));
        registerListener(new JoinListener(this));
        registerListener(new ChatListener(this));
        registerListener(new CommandListener(this));
        feed.start();

        if (Bukkit.getPluginManager().isPluginEnabled(FAWE_PLUGIN))
        {
            faweHook = new FaweHook(this);
            shadowActive = faweHook.register();
        }
        else
        {
            getLogger().warn("FastAsyncWorldEdit is not enabled; WorldEdit commands of restricted players are cancelled");
        }
    }

    @Override
    public void disable()
    {
        shadowActive = false;
        if (faweHook != null)
        {
            faweHook.unregister();
            faweHook = null;
        }
        if (executor != null)
        {
            executor.shutdownNow();
            executor = null;
        }
        if (quarantine != null)
        {
            quarantine.clear();
        }
        if (feed != null)
        {
            feed.clear();
        }
        if (raidDetector != null)
        {
            raidDetector.clear();
        }
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public int getTime()
    {
        return time;
    }

    // True while FAWE routes restricted edits through the shadow extent; otherwise commands are cancelled instead.
    public boolean shadowActive()
    {
        return shadowActive;
    }

    public Quarantine quarantine()
    {
        return quarantine;
    }

    public StaffFeed feed()
    {
        return feed;
    }

    public RaidDetector raidDetector()
    {
        return raidDetector;
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

    public boolean bypassesRestriction(UUID uuid)
    {
        // Looked up per call because the permissions plugin may enable after Plex.
        RegisteredServiceProvider<Permission> provider = Bukkit.getServicesManager().getRegistration(Permission.class);
        if (provider == null)
        {
            if (missingPermissionsReported.compareAndSet(false, true))
            {
                getLogger().warn("No Vault permission provider is registered; {} cannot be checked", BYPASS_PERMISSION);
            }
            return false;
        }
        return provider.getProvider().playerHas((String) null, Bukkit.getOfflinePlayer(uuid), BYPASS_PERMISSION);
    }

    private int requireAtLeastOne(String key, int value)
    {
        if (value < 1)
        {
            throw new IllegalStateException("Invalid " + key + " '" + value + "'; the value must be at least 1");
        }
        return value;
    }
}
