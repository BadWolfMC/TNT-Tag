package nl.juriantech.tnttag.hooks;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import nl.juriantech.tnttag.Tnttag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TabHook {

    private final Tnttag plugin;
    private final TabAPI tabAPI;

    public TabHook(Tnttag plugin) {
        this.plugin = plugin;
        this.tabAPI = TabAPI.getInstance();
    }

    public void hidePlayerName(UUID playerUUID) {
        TabPlayer tabPlayer = getLoadedPlayer(playerUUID);
        if (tabPlayer != null && tabAPI.getNameTagManager() != null) {
            tabAPI.getNameTagManager().hideNameTag(tabPlayer);
        }
    }

    public void showPlayerName(UUID playerUUID) {
        TabPlayer tabPlayer = getLoadedPlayer(playerUUID);
        if (tabPlayer != null && tabAPI.getNameTagManager() != null) {
            tabAPI.getNameTagManager().showNameTag(tabPlayer);
        }
    }

    public void setPlayerPrefix(UUID playerUUID, String prefix) {
        if (prefix == null) return;

        TabPlayer tabPlayer = getLoadedPlayer(playerUUID);
        if (tabPlayer == null) return;

        Player player = Bukkit.getPlayer(playerUUID);
        if (plugin.getPlaceholderAPIExpansion() != null && player != null) {
            prefix = plugin.getPlaceholderAPIExpansion().parse(player, prefix);
        }

        if (tabAPI.getNameTagManager() != null) {
            tabAPI.getNameTagManager().setPrefix(tabPlayer, prefix);
        }

        if (tabAPI.getTabListFormatManager() != null) {
            tabAPI.getTabListFormatManager().setPrefix(tabPlayer, prefix);
        }
    }

    /**
     * Removes TNT-Tag's temporary TAB API overrides so TAB can resume using
     * its configured, potentially dynamic, prefix values.
     */
    public void resetPlayerPrefix(UUID playerUUID) {
        TabPlayer tabPlayer = getLoadedPlayer(playerUUID);
        if (tabPlayer == null) return;

        if (tabAPI.getNameTagManager() != null) {
            tabAPI.getNameTagManager().setPrefix(tabPlayer, null);
        }

        if (tabAPI.getTabListFormatManager() != null) {
            tabAPI.getTabListFormatManager().setPrefix(tabPlayer, null);
        }
    }

    private TabPlayer getLoadedPlayer(UUID playerUUID) {
        TabPlayer tabPlayer = tabAPI.getPlayer(playerUUID);
        return tabPlayer != null && tabPlayer.isLoaded() ? tabPlayer : null;
    }
}
