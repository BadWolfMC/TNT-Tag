package nl.juriantech.tnttag.gui.menu;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.logging.Level;

public final class MenuListener implements Listener {

    private final Tnttag plugin;

    public MenuListener(Tnttag plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof Menu menu)) {
            return;
        }

        // Cancel the entire transaction, including shift-clicks, number-key swaps, double-click collection,
        // and creative inventory actions. Only explicit menu callbacks are allowed to perform work.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        try {
            menu.handleClick(player, event);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "A TNT-Tag menu action failed for " + player.getName() + ".", exception);
            player.sendMessage(ChatUtils.component(
                    "<red>The menu action could not be completed. Check the server console for details."
            ));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof Menu) {
            event.setCancelled(true);
        }
    }
}
