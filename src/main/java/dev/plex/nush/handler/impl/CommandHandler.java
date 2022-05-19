package dev.plex.nush.handler.impl;

import dev.plex.nush.NushModule;
import dev.plex.nush.command.impl.NUSHCommand;
import dev.plex.nush.handler.Handler;

public class CommandHandler implements Handler
{

    @Override
    public void init(NushModule module)
    {
        module.registerCommand(new NUSHCommand());
    }
}
