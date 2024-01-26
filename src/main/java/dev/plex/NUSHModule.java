package dev.plex;

import dev.plex.command.NUSHCommand;
import dev.plex.config.ModuleConfig;
import dev.plex.listener.ChatListener;
import dev.plex.listener.JoinListener;
import dev.plex.module.PlexModule;
import lombok.Getter;

public class NUSHModule extends PlexModule
{

    @Getter
    private static NUSHModule module;
    @Getter
    private static ModuleConfig config;
    @Getter
    private static boolean enabled;
    @Getter
    private static int time;

    @Override
    public void load()
    {
        config = new ModuleConfig(this, "nush/config.yml", "config.yml");
    }

    @Override
    public void enable()
    {
        module = this;
        config.load();
        enabled = config.getBoolean("server.enabled", false);
        time = config.getInt("server.wait_time", 2);
        registerCommand(new NUSHCommand());
        registerListener(new JoinListener());
        registerListener(new ChatListener());
    }

    @Override
    public void disable()
    {
        module = null;
    }

    public static void toggle(boolean toggle)
    {
        enabled = toggle;
        config.set("server.enabled", toggle);
    }

    public static void setTime(int minutes)
    {
        time = minutes;
        config.set("server.wait_time", minutes);
    }
}
