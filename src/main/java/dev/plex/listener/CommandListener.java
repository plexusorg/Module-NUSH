package dev.plex.listener;

import dev.plex.NUSHModule;
import dev.plex.nush.Quarantine.Kind;
import dev.plex.nush.Quarantine.LogEntry;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class CommandListener implements Listener
{
    // These commands change the world outside an edit session, so the shadow extent cannot fake them.
    private static final Set<String> BYPASS_COMMANDS =
            Set.of("regen", "butcher", "remove", "delchunks", "restore", "snapshot");

    private final NUSHModule module;

    public CommandListener(NUSHModule module)
    {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event)
    {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!module.isEnabled() || !module.quarantine().isRestricted(uuid))
        {
            return;
        }

        String message = event.getMessage();
        String label = message.replaceFirst("^/", "").replaceFirst("\\s.*", "").toLowerCase(Locale.ROOT);
        String baseLabel = label.contains(":") ? label.substring(label.indexOf(':') + 1) : label;
        Command command = Bukkit.getCommandMap().getCommand(label);
        if (command == null)
        {
            command = Bukkit.getCommandMap().getCommand(baseLabel);
        }

        // A leading slash survives the strip above only for WorldEdit's double slash commands.
        if (!label.startsWith("/") && !isWorldEditCommand(command))
        {
            return;
        }

        if (BYPASS_COMMANDS.contains(baseLabel.replaceFirst("^/", "")))
        {
            event.setCancelled(true);
        }
        module.quarantine().record(uuid, new LogEntry(Kind.COMMAND, Instant.now(), message));
        module.feed().line(player, Kind.COMMAND, Component.text(player.getName() + ": " + message), message);
    }

    private boolean isWorldEditCommand(Command command)
    {
        if (!(command instanceof PluginIdentifiableCommand identifiable))
        {
            return false;
        }
        Plugin owner = identifiable.getPlugin();
        return owner.getName().equalsIgnoreCase("WorldEdit") || owner.getName().equalsIgnoreCase("FastAsyncWorldEdit");
    }
}
