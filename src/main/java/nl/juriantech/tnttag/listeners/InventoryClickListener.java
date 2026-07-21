package nl.juriantech.tnttag.listeners;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.managers.LobbyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Protects player inventories for the full TNT-Tag session. Menu callbacks still run at a higher
 * priority, but neither menu items nor session items can be moved by inventory transactions.
 */
public class InventoryClickListener implements Listener {

    private final LobbyManager lobbyManager;

    public InventoryClickListener(Tnttag plugin) {
        this.lobbyManager = plugin.getLobbyManager();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && lobbyManager.playerIsInLobby(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && lobbyManager.playerIsInLobby(player)) {
            event.setCancelled(true);
        }
    }
}
