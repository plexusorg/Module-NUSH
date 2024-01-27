package dev.plex;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserData
{

    private static final Map<UUID, UserData> USERS_MAP = new HashMap<>();
    private BukkitTask task = null;

    public UserData(Player player)
    {
        USERS_MAP.put(player.getUniqueId(), this);
    }

    public static void queueNewPlayer(Player player)
    {
        UserData data = new UserData(player);
        data.task = Bukkit.getScheduler().runTaskLater(NUSHModule.getModule().getPlex(),
                () ->
                {
                    if (data.isValid())
                    {
                        data.task.cancel();
                        USERS_MAP.remove(player.getUniqueId());
                    }
                },
                20L * 60L * NUSHModule.getTime());
    }

    public static boolean isNewPlayer(Player player)
    {
        return USERS_MAP.containsKey(player.getUniqueId());
    }

    public static void removePlayer(Player player)
    {
        USERS_MAP.get(player.getUniqueId()).task.cancel();
        USERS_MAP.remove(player.getUniqueId());
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
