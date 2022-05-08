package dev.plex.nush.handler.impl;

import dev.plex.nush.NushModule;
import dev.plex.nush.handler.Handler;
import dev.plex.nush.listener.impl.JoinListener;

public class ListenerHandler implements Handler {

	@Override
	public void init(NushModule module) {
		module.registerListener(new JoinListener());
	}
}
