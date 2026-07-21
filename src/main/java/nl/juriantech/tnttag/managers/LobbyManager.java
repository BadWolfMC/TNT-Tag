package nl.juriantech.tnttag.managers;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.objects.PlayerInformation;
import nl.juriantech.tnttag.utils.ChatUtils;
import nl.juriantech.tnttag.utils.RegistryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Owns the complete player session that starts when TNT-Tag takes control of a player and ends when
 * the player leaves the global lobby. A player receives exactly one state snapshot per session.
 */
public class LobbyManager {

    private final Tnttag plugin;
    private final ItemManager itemManager;
    private final Set<UUID> players = new HashSet<>();
    private final Map<UUID, PlayerInformation> playerInformation = new HashMap<>();
    private Location globalLobbyLocation;

    public LobbyManager(Tnttag plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
    }

    public boolean enterLobby(Player player, boolean teleport) {
        UUID playerId = player.getUniqueId();
        if (players.contains(playerId)) {
            if (teleport) teleportToLobby(player);
            return true;
        }

        if (globalLobbyLocation == null) {
            ChatUtils.sendMessage(player, "player.global-lobby-not-set");
            return false;
        }

        // Close external containers before TNT-Tag replaces the player inventory.
        player.closeInventory();
        // Remove only marked leftovers from an interrupted older session before taking the real snapshot.
        itemManager.cleanupManagedItems(player);
        PlayerInformation snapshot = new PlayerInformation(plugin, player);
        playerInformation.put(playerId, snapshot);
        players.add(playerId);

        itemManager.giveGlobalLobbyItems(player);
        if (teleport) {
            teleportToLobby(player);
            ChatUtils.sendMessage(player, "player.joined-lobby");
            player.playSound(player.getLocation(), RegistryUtils.sound(ChatUtils.getRaw("sounds.lobby-join")), 1, 1);
        }
        return true;
    }

    public void leaveLobby(Player player) {
        leaveLobby(player, true);
    }

    public void leaveLobby(Player player, boolean notify) {
        UUID playerId = player.getUniqueId();
        boolean wasInLobby = players.remove(playerId);
        PlayerInformation snapshot = playerInformation.remove(playerId);
        if (!wasInLobby && snapshot == null) {
            itemManager.cleanupManagedItems(player);
            return;
        }

        player.closeInventory();
        itemManager.clearInventory(player);
        if (snapshot != null) {
            snapshot.restore();
        } else {
            plugin.getLogger().warning("Missing player-state snapshot while removing " + player.getName()
                    + " from the TNT-Tag lobby; managed items were removed, but the prior inventory could not be restored.");
        }

        if (notify && player.isOnline()) {
            ChatUtils.sendMessage(player, "player.leaved-lobby");
            player.playSound(player.getLocation(), RegistryUtils.sound(ChatUtils.getRaw("sounds.lobby-leave")), 1, 1);
        }
    }

    public boolean playerIsInLobby(Player player) {
        return players.contains(player.getUniqueId());
    }

    public void teleportToLobby(Player player) {
        if (globalLobbyLocation == null) {
            ChatUtils.sendMessage(player, "player.global-lobby-not-set");
            return;
        }
        player.teleport(globalLobbyLocation);
    }

    public void load() {
        String serializedLocation = Tnttag.configfile.getString("globalLobby");
        if (serializedLocation == null || serializedLocation.isBlank()) {
            globalLobbyLocation = null;
            return;
        }

        String[] parts = serializedLocation.split(",");
        if (parts.length != 6) {
            plugin.getLogger().severe("Invalid globalLobby location in config.yml: expected 6 comma-separated values.");
            globalLobbyLocation = null;
            return;
        }

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            plugin.getLogger().severe("Unable to load globalLobby because world '" + parts[0] + "' is not loaded.");
            globalLobbyLocation = null;
            return;
        }

        try {
            Location location = new Location(
                    world,
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Float.parseFloat(parts[4]),
                    Float.parseFloat(parts[5])
            );
            globalLobbyLocation = location;
        } catch (NumberFormatException exception) {
            plugin.getLogger().severe("Invalid numeric value in globalLobby location: " + serializedLocation);
            globalLobbyLocation = null;
        }
    }

    public PlayerInformation getPlayerInformation(Player player) {
        return playerInformation.get(player.getUniqueId());
    }

    public Map<UUID, PlayerInformation> getPlayerInformation() {
        return Map.copyOf(playerInformation);
    }
}
