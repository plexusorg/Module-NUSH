package dev.plex.nush;

import dev.plex.NUSHModule;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RaidDetector
{
    private final NUSHModule module;
    private final StaffFeed feed;
    private final int windowSeconds;
    private final int joinThreshold;
    private final int chatThreshold;
    private final Deque<Long> joins = new ArrayDeque<>();
    private final Map<UUID, Long> chatters = new HashMap<>();
    private long lastAlert;

    public RaidDetector(NUSHModule module, StaffFeed feed, int windowSeconds, int joinThreshold, int chatThreshold)
    {
        this.module = module;
        this.feed = feed;
        this.windowSeconds = windowSeconds;
        this.joinThreshold = joinThreshold;
        this.chatThreshold = chatThreshold;
    }

    public synchronized void join()
    {
        long now = System.currentTimeMillis();
        joins.addLast(now);
        evaluate(now);
    }

    public synchronized void chat(UUID uuid)
    {
        long now = System.currentTimeMillis();
        chatters.put(uuid, now);
        evaluate(now);
    }

    public synchronized void clear()
    {
        joins.clear();
        chatters.clear();
        lastAlert = 0;
    }

    private void evaluate(long now)
    {
        long cutoff = now - windowSeconds * 1000L;
        while (!joins.isEmpty() && joins.peekFirst() < cutoff)
        {
            joins.pollFirst();
        }
        chatters.values().removeIf(last -> last < cutoff);

        if (joins.size() < joinThreshold && chatters.size() < chatThreshold)
        {
            return;
        }
        if (lastAlert != 0 && now - lastAlert < windowSeconds * 1000L)
        {
            return;
        }

        lastAlert = now;
        feed.alert(module.messageComponent("raidDetected",
                Placeholder.unparsed("joins", String.valueOf(joins.size())),
                Placeholder.unparsed("chatters", String.valueOf(chatters.size())),
                Placeholder.unparsed("seconds", String.valueOf(windowSeconds))));
        module.getLogger().warn("Possible raid: {} unverified joins and {} restricted players chatting within {} seconds",
                joins.size(), chatters.size(), windowSeconds);
    }
}
