package nl.juriantech.tnttag.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {
    private final ItemStack item;
    private Component name;
    private boolean hideAttributes;
    private final List<Component> lore = new ArrayList<>();
    private UUID skullOwner;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
    }

    public ItemBuilder setAmount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder displayName(String name) {
        if (name == null || name.isEmpty()) return this;
        this.name = ChatUtils.component(name);
        return this;
    }

    public ItemBuilder addLoreSpacer() {
        lore.add(Component.empty());
        return this;
    }

    public ItemBuilder lore(String loreText) {
        if (loreText == null || loreText.isEmpty()) return this;
        for (String line : loreText.split("\\n", -1)) {
            lore.add(ChatUtils.component(line));
        }
        return this;
    }

    public ItemBuilder setSkullOwner(String owner) {
        try {
            OfflinePlayer offlinePlayer;
            try {
                UUID uuid = UUID.fromString(owner);
                offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                skullOwner = uuid;
            } catch (IllegalArgumentException ignored) {
                offlinePlayer = Bukkit.getOfflinePlayer(owner);
                skullOwner = offlinePlayer.getUniqueId();
            }

            if (item.getItemMeta() instanceof SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(offlinePlayer);
                item.setItemMeta(skullMeta);
            }
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unable to resolve skull owner: " + owner, exception);
        }
        return this;
    }

    public ItemBuilder hideAttributes() {
        this.hideAttributes = true;
        return this;
    }

    public ItemStack build() {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) meta.displayName(name);
            if (!lore.isEmpty()) meta.lore(List.copyOf(lore));
            if (hideAttributes) meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        if (skullOwner != null && item.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(skullOwner));
            item.setItemMeta(skullMeta);
        }
        return item.clone();
    }

    public static ItemBuilder from(String serialized) {
        String[] parts = serialized.split(":", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Expected material:displayName:lore, got: " + serialized);
        }
        return new ItemBuilder(RegistryUtils.material(parts[0]))
                .displayName(parts[1])
                .lore(parts[2]);
    }
}
