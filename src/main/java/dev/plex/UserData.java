package dev.plex;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserData
{
    private static final Map<UUID, ScheduledTask> TASKS = new ConcurrentHashMap<>();

    public static void queueNewPlayer(NUSHModule module, Player player)
    {
        UUID uuid = player.getUniqueId();
        ScheduledTask task = module.scheduler().runGlobalLater(
                scheduledTask ->
                {
                    TASKS.remove(uuid, scheduledTask);
                },
                20L * 60L * module.getTime());
        ScheduledTask previous = TASKS.put(uuid, task);
        if (previous != null) previous.cancel();
    }

    public static boolean isNewPlayer(Player player)
    {
        return TASKS.containsKey(player.getUniqueId());
    }

    public static void removePlayer(Player player)
    {
        ScheduledTask task = TASKS.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    public static void clear()
    {
        TASKS.values().forEach(ScheduledTask::cancel);
        TASKS.clear();
    }
}
