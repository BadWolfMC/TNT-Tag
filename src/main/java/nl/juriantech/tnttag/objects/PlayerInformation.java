package nl.juriantech.tnttag.objects;

import net.kyori.adventure.text.Component;
import nl.juriantech.tnttag.Tnttag;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Complete snapshot of player state before TNT-Tag takes control of it.
 */
public class PlayerInformation {

    private final Tnttag plugin;
    private final Player player;
    private final Location oldLocation;
    private final ItemStack[] storage;
    private final ItemStack[] armor;
    private final ItemStack offHand;
    private final ItemStack cursor;
    private final int level;
    private final int totalExperience;
    private final float experienceProgress;
    private final GameMode gameMode;
    private final int foodLevel;
    private final float saturation;
    private final Collection<PotionEffect> potionEffects;
    private final boolean allowFlight;
    private final boolean flying;
    private final boolean collidable;
    private final boolean invisible;
    private final Component displayName;
    private final Component playerListName;
    private final Scoreboard scoreboard;
    private boolean restored;

    public PlayerInformation(Tnttag plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.oldLocation = player.getLocation().clone();
        this.storage = cloneContents(player.getInventory().getStorageContents());
        this.armor = cloneContents(player.getInventory().getArmorContents());
        this.offHand = cloneItem(player.getInventory().getItemInOffHand());
        this.cursor = cloneItem(player.getItemOnCursor());
        this.level = player.getLevel();
        this.totalExperience = player.getTotalExperience();
        this.experienceProgress = player.getExp();
        this.gameMode = player.getGameMode();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.potionEffects = List.copyOf(player.getActivePotionEffects());
        this.allowFlight = player.getAllowFlight();
        this.flying = player.isFlying();
        this.collidable = player.isCollidable();
        this.invisible = player.isInvisible();
        this.displayName = player.displayName();
        this.playerListName = player.playerListName();
        this.scoreboard = player.getScoreboard();

        plugin.getItemManager().clearInventory(player);
        removeActivePotionEffects();
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0);
        player.setGameMode(GameMode.SURVIVAL);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
    }

    public void restorePresentation() {
        player.displayName(displayName);
        player.playerListName(playerListName);
        player.setScoreboard(scoreboard);
        if (plugin.getTabHook() != null) {
            plugin.getTabHook().resetPlayerPrefix(player.getUniqueId());
            plugin.getTabHook().showPlayerName(player.getUniqueId());
        }
    }

    public void restore() {
        if (restored) return;
        restored = true;

        removeActivePotionEffects();
        for (PotionEffect effect : potionEffects) {
            player.addPotionEffect(effect);
        }

        plugin.getItemManager().clearInventory(player);
        player.getInventory().setStorageContents(cloneContents(storage));
        player.getInventory().setArmorContents(cloneContents(armor));
        player.getInventory().setItemInOffHand(cloneItem(offHand));
        player.setItemOnCursor(cloneItem(cursor));

        player.setTotalExperience(totalExperience);
        player.setLevel(level);
        player.setExp(experienceProgress);

        if (!Tnttag.configfile.getBoolean("skip-location-restoral")) {
            player.teleport(oldLocation);
        }

        player.setGameMode(gameMode);
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setAllowFlight(allowFlight);
        player.setFlying(allowFlight && flying);
        player.setCollidable(collidable);
        player.setInvisible(invisible);
        restorePresentation();
    }

    private void removeActivePotionEffects() {
        new ArrayList<>(player.getActivePotionEffects())
                .forEach(effect -> player.removePotionEffect(effect.getType()));
    }

    private static ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] cloned = new ItemStack[contents.length];
        for (int index = 0; index < contents.length; index++) {
            cloned[index] = cloneItem(contents[index]);
        }
        return cloned;
    }

    private static ItemStack cloneItem(ItemStack item) {
        return item == null || item.getType().isAir() ? null : item.clone();
    }

    public Player getPlayer() {
        return player;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public Component getPlayerListName() {
        return playerListName == null ? Component.text(player.getName()) : playerListName;
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

}
