package nl.juriantech.tnttag.objects;

import net.kyori.adventure.text.Component;
import nl.juriantech.tnttag.Tnttag;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

public class PlayerInformation {

    private final Player player;
    private final Location oldLocation;
    private final ItemStack[] inventory;
    private final ItemStack[] armor;
    private final int level;
    private final int totalExperience;
    private final GameMode gameMode;
    private final int foodLevel;
    private final Component displayName;
    private final Component playerListName;
    private String tabPrefix;

    public PlayerInformation(Tnttag plugin, Player player) {
        this.player = player;
        this.oldLocation = player.getLocation().clone();
        this.inventory = player.getInventory().getContents();
        this.armor = player.getInventory().getArmorContents();
        this.level = player.getLevel();
        this.totalExperience = player.getTotalExperience();
        this.gameMode = player.getGameMode();
        this.foodLevel = player.getFoodLevel();
        this.displayName = player.displayName();
        Component currentListName = player.playerListName();
        this.playerListName = currentListName == null ? Component.text(player.getName()) : currentListName;

        if (plugin.getTabHook() != null) {
            tabPrefix = plugin.getTabHook().getPlayerPrefix(player.getUniqueId());
        }

        player.getInventory().clear();
        player.setExp(0);
        player.setGameMode(GameMode.SURVIVAL);
        player.setFoodLevel(20);
    }

    public void restore() {
        List<PotionEffect> activeEffects = new ArrayList<>(player.getActivePotionEffects());
        activeEffects.forEach(effect -> player.removePotionEffect(effect.getType()));

        player.getInventory().clear();
        player.getInventory().setContents(inventory);
        player.getInventory().setArmorContents(armor);
        player.setTotalExperience(totalExperience);
        player.setLevel(level);

        if (!Tnttag.configfile.getBoolean("skip-location-restoral")) player.teleport(oldLocation);

        player.setGameMode(gameMode);
        player.setFoodLevel(foodLevel);
        player.displayName(displayName);
        player.playerListName(playerListName);
    }

    public Player getPlayer() {
        return player;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public Component getPlayerListName() {
        return playerListName;
    }

    public String getTabPrefix() {
        return tabPrefix;
    }
}
