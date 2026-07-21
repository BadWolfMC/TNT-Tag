package nl.juriantech.tnttag.gui.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

@FunctionalInterface
public interface MenuAction {

    void handle(Player player, InventoryClickEvent event);
}
