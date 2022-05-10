package dev.plex.nush.handler.impl;

import dev.plex.Plex;
import dev.plex.cache.DataUtils;
import dev.plex.nush.Message;
import dev.plex.nush.NushAction;
import dev.plex.nush.NushModule;
import dev.plex.nush.handler.Handler;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.Bukkit;

public class ActionHandler implements Handler {

	public static final Map<UUID, Message> MAP = new HashMap<>();
	private final static TextReplacementConfig URL_REPLACEMENT_CONFIG = TextReplacementConfig.builder()
		.match("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
		.replacement(
			(matchResult, builder) -> Component.empty().content(matchResult.group()).clickEvent(
				ClickEvent.openUrl(matchResult.group()))).build();
	private static NushModule MODULE;

	public static void resolve(UUID uuid, NushAction action) {
		Message message = MAP.get(uuid);
		if (message == null) {
			return;
		}
		if (action == NushAction.ACCEPT) {
			Audience.audience(Bukkit.getServer())
				.sendMessage(Identity.identity(message.getSender()), getMessage(message));
		}
		MAP.remove(uuid);
	}

	public static Component getMessage(Message message) {
		String text = PlexUtils.getTextFromComponent(message.getMessage());
		Plex plex = MODULE.getPlex();
		PlexPlayer plexPlayer = DataUtils.getPlayer(message.getSender());
		Component prefix = plex.getRankManager().getPrefix(plexPlayer);
		Component component = Component.empty();

		if (prefix != null) {
			component = component.append(prefix);
		}

		return component.append(Component.space()).append(
				PlexUtils.mmDeserialize(plex.config.getString("chat.name-color", "<white>") +
					MiniMessage.builder().tags(
						TagResolver.resolver(StandardTags.color(), StandardTags.rainbow(),
							StandardTags.decorations(), StandardTags.gradient(),
							StandardTags.transition()
						)).build().serialize(plexPlayer.getPlayer().displayName())))
			.append(Component.space())
			.append(Component.text("»").color(NamedTextColor.GRAY)).append(Component.space())
			.append(
				SafeMiniMessage.mmDeserializeWithoutEvents(text))
			.replaceText(URL_REPLACEMENT_CONFIG);
	}

	@Override
	public void init(NushModule module) {
		MODULE = NushModule.getInstance();
	}
}
