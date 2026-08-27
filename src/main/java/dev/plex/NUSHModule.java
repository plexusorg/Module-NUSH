package dev.plex;

import dev.plex.command.NUSHCommand;
import dev.plex.api.config.ModuleConfiguration;
import dev.plex.listener.ChatListener;
import dev.plex.listener.JoinListener;
import dev.plex.module.PlexModule;

public class NUSHModule extends PlexModule
{
    private ModuleConfiguration config;
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
        UserData.clear();
    }

    public ModuleConfiguration getConfig()
    {
        return config;
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
}
