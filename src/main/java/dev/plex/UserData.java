package dev.plex;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserData
{
    private static final Map<UUID, UserData> USERS_MAP = new ConcurrentHashMap<>();
    private ScheduledTask task = null;

    public UserData(UUID uuid)
    {
        USERS_MAP.put(uuid, this);
    }

    public static void queueNewPlayer(NUSHModule module, Player player)
    {
        UUID uuid = player.getUniqueId();
        UserData data = new UserData(uuid);
        data.task = module.api().scheduler().runGlobalLater(
                scheduledTask ->
                {
                    if (data.isValid())
                    {
                        data.task = null;
                        USERS_MAP.remove(uuid, data);
                    }
                },
                20L * 60L * module.getTime());
    }

    public static boolean isNewPlayer(Player player)
    {
        return USERS_MAP.containsKey(player.getUniqueId());
    }

    public static void removePlayer(Player player)
    {
        UserData data = USERS_MAP.remove(player.getUniqueId());
        if (data != null && data.isValid())
        {
            data.task.cancel();
        }
    }

    public static void clear()
    {
        USERS_MAP.values().stream().filter(UserData::isValid).forEach(data -> data.task.cancel());
        USERS_MAP.clear();
    }

    public boolean isValid()
    {
        return task != null;
    }
}
