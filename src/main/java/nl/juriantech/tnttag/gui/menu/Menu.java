package nl.juriantech.tnttag.gui.menu;

import net.kyori.adventure.text.Component;
import nl.juriantech.tnttag.Tnttag;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class Menu implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, MenuAction> actions = new HashMap<>();

    public Menu(Tnttag plugin, int size, Component title) {
        if (size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("Menu size must be a multiple of 9 between 9 and 54.");
        }
        this.inventory = plugin.getServer().createInventory(this, size, title);
    }

    public Menu setItem(int slot, ItemStack item) {
        return setItem(slot, item, null);
    }

    public Menu setItem(int slot, ItemStack item, MenuAction action) {
        inventory.setItem(slot, item == null ? null : item.clone());
        if (action == null) {
            actions.remove(slot);
        } else {
            actions.put(slot, action);
        }
        return this;
    }

    public Menu fillEmpty(ItemStack item) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, item.clone());
            }
        }
        return this;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public void handleClick(Player player, InventoryClickEvent event) {
        MenuAction action = actions.get(event.getRawSlot());
        if (action != null) {
            action.handle(player, event);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
