package nl.juriantech.tnttag.objects;

import org.bukkit.inventory.ItemStack;

public class InventoryItem {

    private final String name;
    private final ItemStack item;
    private final String permission;
    private final String command;

    public InventoryItem(String name, ItemStack item, String permission, String command) {
        this.name = name;
        this.item = item.clone();
        this.permission = permission;
        this.command = command;
    }

    public String getName() {
        return name;
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public String getPermission() {
        return permission;
    }

    public String getCommand() {
        return command;
    }
}
