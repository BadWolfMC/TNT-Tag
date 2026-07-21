package nl.juriantech.tnttag.listeners;

import nl.juriantech.tnttag.Arena;
import nl.juriantech.tnttag.Tnttag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final Tnttag plugin;

    public PlayerJoinListener(Tnttag plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getItemManager().cleanupManagedItems(player);

        if (!Tnttag.configfile.getBoolean("bungee-mode.enabled")) return;

        routeBungeePlayerWhenReady(player, 0);
    }

    private void routeBungeePlayerWhenReady(Player player, int attempt) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            if (!plugin.isInitialized()) {
                if (attempt < 20) routeBungeePlayerWhenReady(player, attempt + 1);
                return;
            }

            if (Tnttag.configfile.getBoolean("bungee-mode.enter-arena-instantly")
                    && plugin.getArenaManager().getArenaObjectsSize() == 1) {
                Arena arena = plugin.getArenaManager().getArenaObjects().getFirst();
                plugin.getJoinSubCommand().onJoin(player, arena.getName());
            } else {
                plugin.getJoinSubCommand().onJoin(player, null);
            }
        }, attempt == 0 ? 2L : 5L);
    }
}
