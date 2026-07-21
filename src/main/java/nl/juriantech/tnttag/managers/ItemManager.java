package nl.juriantech.tnttag.managers;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.objects.InventoryItem;
import nl.juriantech.tnttag.utils.ItemBuilder;
import nl.juriantech.tnttag.utils.RegistryUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemManager {

    private static final String TAGGER_HELMET_ID = "__tagger_helmet";

    private final Tnttag plugin;
    private final NamespacedKey managedItemKey;
    private final ArrayList<InventoryItem> items = new ArrayList<>();
    private final HashMap<Integer, InventoryItem> globalLobbyItems = new HashMap<>();
    private final HashMap<Integer, InventoryItem> waitingItems = new HashMap<>();
    private final HashMap<Integer, InventoryItem> gameItems = new HashMap<>();
    private final HashMap<Integer, InventoryItem> taggerItems = new HashMap<>();

    public ItemManager(Tnttag plugin) {
        this.plugin = plugin;
        this.managedItemKey = new NamespacedKey(plugin, "managed_item");
    }

    public void giveGlobalLobbyItems(Player player) {
        replaceInventory(player, globalLobbyItems);
    }

    public void giveWaitingItems(Player player) {
        replaceInventory(player, waitingItems);
    }

    public void giveGameItems(Player player) {
        replaceInventory(player, gameItems);
    }

    public void giveTaggerItems(Player tagger) {
        replaceInventory(tagger, gameItems);
        tagger.getInventory().setHelmet(tag(new ItemStack(Material.TNT), TAGGER_HELMET_ID));
        giveItems(tagger, taggerItems);
    }

    private void replaceInventory(Player player, Map<Integer, InventoryItem> configuredItems) {
        clearInventory(player);
        giveItems(player, configuredItems);
    }

    private void giveItems(Player player, Map<Integer, InventoryItem> configuredItems) {
        for (Map.Entry<Integer, InventoryItem> entry : configuredItems.entrySet()) {
            InventoryItem item = entry.getValue();
            if (item == null) continue;
            if (!item.getPermission().equalsIgnoreCase("NONE") && !player.hasPermission(item.getPermission())) continue;
            player.getInventory().setItem(entry.getKey(), item.getItem());
        }
    }

    /**
     * Clears every player inventory compartment used by TNT-Tag, including armor, offhand, and cursor.
     */
    public void clearInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);
        inventory.setItemInOffHand(null);
        player.setItemOnCursor(null);
    }

    /**
     * Removes only items carrying TNT-Tag's PDC marker. This safely cleans leftovers without matching
     * on display names or materials that another plugin may also use.
     */
    public void cleanupManagedItems(Player player) {
        PlayerInventory inventory = player.getInventory();

        ItemStack[] storage = inventory.getStorageContents();
        for (int index = 0; index < storage.length; index++) {
            if (isManagedItem(storage[index])) storage[index] = null;
        }
        inventory.setStorageContents(storage);

        ItemStack[] armor = inventory.getArmorContents();
        for (int index = 0; index < armor.length; index++) {
            if (isManagedItem(armor[index])) armor[index] = null;
        }
        inventory.setArmorContents(armor);

        if (isManagedItem(inventory.getItemInOffHand())) inventory.setItemInOffHand(null);
        if (isManagedItem(player.getItemOnCursor())) player.setItemOnCursor(null);
    }

    public void load() {
        for (String route : Tnttag.itemsfile.getRoutesAsStrings(true)) {
            if (!route.startsWith("items.") || route.substring("items.".length()).contains(".")) continue;

            String name = route.replace("items.", "");
            if (getItemByName(name) != null) continue;

            ItemStack item = new ItemBuilder(RegistryUtils.material(Tnttag.itemsfile.getString(route + ".material")))
                    .displayName(Tnttag.itemsfile.getString(route + ".display_name"))
                    .lore(Tnttag.itemsfile.getString(route + ".lore"))
                    .build();
            items.add(new InventoryItem(
                    name,
                    tag(item, name),
                    valueOrDefault(Tnttag.itemsfile.getString(route + ".permission"), "NONE"),
                    valueOrDefault(Tnttag.itemsfile.getString(route + ".command"), "NONE")
            ));
        }

        loadItemsForSection("globalLobbyItems", globalLobbyItems);
        loadItemsForSection("waitingItems", waitingItems);
        loadItemsForSection("gameItems", gameItems);
        loadItemsForSection("taggerItems", taggerItems);
    }

    private void loadItemsForSection(String sectionName, HashMap<Integer, InventoryItem> targetMap) {
        List<String> itemStrings = Tnttag.itemsfile.getStringList(sectionName);
        for (String itemString : itemStrings) {
            String[] parts = itemString.split(":", 2);
            if (parts.length != 2) {
                plugin.getLogger().severe("Invalid " + sectionName + " entry: " + itemString);
                continue;
            }

            int slot;
            try {
                slot = Integer.parseInt(parts[0]);
            } catch (NumberFormatException exception) {
                plugin.getLogger().severe("Invalid inventory slot in " + sectionName + ": " + itemString);
                continue;
            }
            if (slot < 0 || slot > 35) {
                plugin.getLogger().severe("Inventory slot must be between 0 and 35 in " + sectionName + ": " + itemString);
                continue;
            }

            InventoryItem inventoryItem = getItemByName(parts[1]);
            if (inventoryItem == null) {
                plugin.getLogger().severe("Failed to find InventoryItem for " + sectionName + ": " + itemString);
            } else {
                targetMap.put(slot, inventoryItem);
            }
        }
    }

    public void reload() throws IOException {
        items.clear();
        globalLobbyItems.clear();
        waitingItems.clear();
        gameItems.clear();
        taggerItems.clear();
        Tnttag.itemsfile.reload();
        load();
    }

    public boolean isManagedItem(ItemStack item) {
        return getManagedItemName(item) != null;
    }

    public String getManagedItemName(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(managedItemKey, PersistentDataType.STRING);
    }

    public String getCommand(ItemStack item) {
        String name = getManagedItemName(item);
        if (name == null) return null;
        InventoryItem inventoryItem = getItemByName(name);
        return inventoryItem == null ? null : inventoryItem.getCommand();
    }

    private ItemStack tag(ItemStack item, String name) {
        ItemStack tagged = item.clone();
        var meta = tagged.getItemMeta();
        meta.getPersistentDataContainer().set(managedItemKey, PersistentDataType.STRING, name);
        tagged.setItemMeta(meta);
        return tagged;
    }

    private InventoryItem getItemByName(String name) {
        return items.stream()
                .filter(item -> item.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public List<InventoryItem> getItems() {
        return List.copyOf(items);
    }
}
