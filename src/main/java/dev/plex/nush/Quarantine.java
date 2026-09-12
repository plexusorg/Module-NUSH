package dev.plex.nush;

import dev.plex.NUSHModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Quarantine
{
    public enum Kind
    {
        CHAT, COMMAND
    }

    public record LogEntry(Kind kind, Instant time, String text)
    {
    }

    private static final String VERIFIED_KEY = "verified";

    private final NUSHModule module;
    private final StaffFeed feed;
    private final ScheduledExecutorService executor;
    private final int logSize;
    private final Map<UUID, Restriction> restrictions = new ConcurrentHashMap<>();
    // Value: whether the pending login is the player's first ever join.
    private final Map<UUID, Boolean> pendingUnverified = new ConcurrentHashMap<>();

    public Quarantine(NUSHModule module, StaffFeed feed, ScheduledExecutorService executor, int logSize)
    {
        this.module = module;
        this.feed = feed;
        this.executor = executor;
        this.logSize = logSize;
    }

    public void restrict(Player player, boolean firstJoin)
    {
        UUID uuid = player.getUniqueId();
        Restriction restriction = new Restriction(uuid, player.getName(), firstJoin, logSize);
        // Start the timer before publishing the restriction so a concurrent verify always cancels a real task.
        restriction.expiry(executor.schedule(() -> verify(uuid, null), module.getTime(), TimeUnit.MINUTES));
        Restriction previous = restrictions.put(uuid, restriction);
        if (previous != null)
        {
            previous.cancelExpiry();
        }
    }

    public CompletableFuture<Void> verify(UUID uuid, @Nullable String byName)
    {
        Restriction restriction = restrictions.remove(uuid);
        if (restriction != null)
        {
            restriction.cancelExpiry();
        }
        pendingUnverified.remove(uuid);
        String name = restriction == null ? nameOf(uuid) : restriction.name();
        return write(uuid).whenComplete((ignored, failure) -> feed.alert(byName == null
                ? module.messageComponent("quarantineExpired", Placeholder.unparsed("player", name))
                : module.messageComponent("playerAllowed", Placeholder.unparsed("player", name),
                        Placeholder.unparsed("admin", byName))));
    }

    // Used by the grandfather rule at pre-login, which stores the flag without releasing a restriction.
    public void markVerified(UUID uuid)
    {
        write(uuid);
    }

    public void revoke(UUID uuid)
    {
        module.api().players().moduleData(module, uuid).remove(VERIFIED_KEY)
                .whenComplete((ignored, failure) ->
                {
                    if (failure != null)
                    {
                        module.getLogger().error("Unable to remove the NUSH verification of {}", uuid, failure);
                    }
                });
        Player player = Bukkit.getPlayer(uuid);
        if (player != null)
        {
            restrict(player, false);
        }
    }

    public boolean isRestricted(UUID uuid)
    {
        return restrictions.containsKey(uuid);
    }

    @Nullable
    public Restriction entry(UUID uuid)
    {
        return restrictions.get(uuid);
    }

    @Nullable
    public Restriction byName(String name)
    {
        for (Restriction restriction : restrictions.values())
        {
            if (restriction.name().equalsIgnoreCase(name))
            {
                return restriction;
            }
        }
        return null;
    }

    public Collection<Restriction> entries()
    {
        return Collections.unmodifiableCollection(restrictions.values());
    }

    public void record(UUID uuid, LogEntry logEntry)
    {
        Restriction restriction = entry(uuid);
        if (restriction != null)
        {
            restriction.record(logEntry);
        }
    }

    public void markPending(UUID uuid, boolean firstJoin)
    {
        pendingUnverified.put(uuid, firstJoin);
    }

    public void clearPending(UUID uuid)
    {
        pendingUnverified.remove(uuid);
    }

    @Nullable
    public Boolean consumePending(UUID uuid)
    {
        return pendingUnverified.remove(uuid);
    }

    // Kicks online restricted players: first-join accounts when firstJoin is true, reconnected ones otherwise.
    public int kick(boolean firstJoin, Component message)
    {
        int kicked = 0;
        for (Restriction restriction : restrictions.values())
        {
            Player player = Bukkit.getPlayer(restriction.uuid());
            if (player == null || restriction.firstJoin() != firstJoin)
            {
                continue;
            }
            player.getScheduler().run(module.plugin(), task -> player.kick(message), null);
            kicked++;
        }
        return kicked;
    }

    public void clear()
    {
        restrictions.values().forEach(Restriction::cancelExpiry);
        restrictions.clear();
        pendingUnverified.clear();
    }

    private CompletableFuture<Void> write(UUID uuid)
    {
        return module.api().players().moduleData(module, uuid).set(VERIFIED_KEY, true)
                .whenComplete((ignored, failure) ->
                {
                    if (failure != null)
                    {
                        module.getLogger().error("Unable to store the NUSH verification of {}", uuid, failure);
                    }
                });
    }

    private String nameOf(UUID uuid)
    {
        Player player = Bukkit.getPlayer(uuid);
        return player == null ? uuid.toString() : player.getName();
    }

    public static final class Restriction
    {
        private final UUID uuid;
        private final String name;
        private final boolean firstJoin;
        private final int logSize;
        private final Deque<LogEntry> log = new ArrayDeque<>();
        private volatile ScheduledFuture<?> expiry;
        private int messages;
        private int blockedCommands;

        private Restriction(UUID uuid, String name, boolean firstJoin, int logSize)
        {
            this.uuid = uuid;
            this.name = name;
            this.firstJoin = firstJoin;
            this.logSize = logSize;
        }

        public boolean firstJoin()
        {
            return firstJoin;
        }

        public UUID uuid()
        {
            return uuid;
        }

        public String name()
        {
            return name;
        }

        public long remainingSeconds()
        {
            ScheduledFuture<?> task = expiry;
            return task == null ? 0 : Math.max(0, task.getDelay(TimeUnit.SECONDS));
        }

        public List<LogEntry> log()
        {
            synchronized (log)
            {
                return new ArrayList<>(log);
            }
        }

        public int messages()
        {
            synchronized (log)
            {
                return messages;
            }
        }

        public int blockedCommands()
        {
            synchronized (log)
            {
                return blockedCommands;
            }
        }

        private void record(LogEntry entry)
        {
            synchronized (log)
            {
                log.addLast(entry);
                if (log.size() > logSize)
                {
                    log.pollFirst();
                }
                if (entry.kind() == Kind.CHAT)
                {
                    messages++;
                }
                else
                {
                    blockedCommands++;
                }
            }
        }

        private void expiry(ScheduledFuture<?> task)
        {
            expiry = task;
        }

        private void cancelExpiry()
        {
            ScheduledFuture<?> task = expiry;
            if (task != null)
            {
                task.cancel(false);
            }
        }
    }
}
