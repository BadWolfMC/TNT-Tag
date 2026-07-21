package nl.juriantech.tnttag.subcommands;

import nl.juriantech.tnttag.Arena;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.managers.ArenaManager;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.entity.Player;

public class JoinSubCommand {

    private final Tnttag plugin;
    private final ArenaManager arenaManager;

    public JoinSubCommand(Tnttag plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
    }

    public void onJoin(Player player, String arenaName) {
        if (arenaName == null) {
            if (!Tnttag.configfile.getBoolean("global-lobby")) {
                ChatUtils.sendConfiguredMessage(player, "general.specify-arena");
                return;
            }
            if (plugin.getLobbyManager().playerIsInLobby(player)) {
                ChatUtils.sendMessage(player, "player.already-in-lobby");
                return;
            }
            plugin.getLobbyManager().enterLobby(player, true);
            return;
        }

        if (arenaManager.playerIsInArena(player)) {
            ChatUtils.sendMessage(player, "player.already-in-game");
            return;
        }

        // Validate the requested destination before TNT-Tag snapshots or clears player state.
        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) {
            ChatUtils.sendMessage(player, "commands.invalid-arena");
            return;
        }
        if (arena.getGameManager().isRunning()) {
            ChatUtils.sendMessage(player, "arena.active");
            return;
        }
        if (!arena.getGameManager().playerManager.canAcceptPlayers(1)) {
            ChatUtils.sendMessage(player, "arena.full");
            return;
        }

        arena.getGameManager().playerManager.addPlayer(player);
    }
}
