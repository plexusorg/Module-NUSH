package dev.plex.nush;

import dev.plex.nush.handler.impl.CommandHandler;
import dev.plex.nush.handler.impl.ListenerHandler;
import dev.plex.module.PlexModule;

public class NushModule extends PlexModule
{
    public static boolean enabled = false;
    private static NushModule INSTANCE;

    @Override
    public void enable()
    {
        INSTANCE = this;
        getPlex().messages.addDefault("nushToggled", "<aqua>{0} - {1} NUSH.");
        new CommandHandler().init(this);
        new ListenerHandler().init(this);
    }

    @Override
    public void disable()
    {
        // Unregistering listeners / commands is handled by Plex
    }

    public static NushModule getInstance() {
        return INSTANCE;
    }
}
