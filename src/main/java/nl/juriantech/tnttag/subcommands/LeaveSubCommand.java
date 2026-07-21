package nl.juriantech.tnttag.subcommands;

import nl.juriantech.tnttag.Arena;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.managers.ArenaManager;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.entity.Player;

public class LeaveSubCommand {

    private final Tnttag plugin;
    private final ArenaManager arenaManager;

    public LeaveSubCommand(Tnttag plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
    }

    public void onLeave(Player player) {
        boolean wasInArena = arenaManager.playerIsInArena(player);
        boolean wasInLobby = plugin.getLobbyManager().playerIsInLobby(player);

        if (wasInArena) {
            Arena arena = arenaManager.getPlayerArena(player);
            if (arena != null) {
                arena.getGameManager().playerManager.removePlayer(player, true);
            }
        } else if (!wasInLobby) {
            ChatUtils.sendMessage(player, "commands.not-in-arena");
            return;
        }

        if (Tnttag.configfile.getBoolean("bungee-mode.enabled")) {
            // PlayerQuitEvent restores any still-active lobby snapshot after a successful proxy transfer.
            // With the global lobby disabled, removePlayer has already restored the snapshot locally.
            plugin.connectToServer(player, Tnttag.configfile.getString("bungee-mode.lobby-server"));
            return;
        }

        if (plugin.getLobbyManager().playerIsInLobby(player)) {
            plugin.getLobbyManager().leaveLobby(player);
        }
    }
}
