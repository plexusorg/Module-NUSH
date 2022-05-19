package dev.plex.nush;

import dev.plex.Plex;
import dev.plex.module.PlexModule;
import dev.plex.nush.handler.impl.ActionHandler;
import dev.plex.nush.handler.impl.CommandHandler;
import dev.plex.nush.handler.impl.ListenerHandler;
import java.util.Map;
import java.util.Map.Entry;

public class NushModule extends PlexModule {

	public static final Map<String, String> messages = Map.ofEntries(
		Map.entry("nushToggled", "<aqua>{0} - {1} NUSH."),
		Map.entry("nushApply", "<yellow>Applying {0} to {1}!"));
	public static boolean enabled = false;
	private static NushModule INSTANCE;

	public static NushModule getInstance() {
		return INSTANCE;
	}

	@Override
	public void enable() {
		INSTANCE = this;
		Plex plex = getPlex();
		for (Entry<String, String> entry : messages.entrySet()) {
			plex.messages.addDefault(entry.getKey(), entry.getValue());
		}

		new ActionHandler().init(this);
		new CommandHandler().init(this);
		new ListenerHandler().init(this);
	}

	@Override
	public void disable() {
		// Unregistering listeners / commands is handled by Plex
	}
}
