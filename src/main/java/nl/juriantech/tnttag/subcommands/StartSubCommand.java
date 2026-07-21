package nl.juriantech.tnttag.subcommands;

import nl.juriantech.tnttag.Arena;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.enums.GameState;
import nl.juriantech.tnttag.managers.ArenaManager;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.entity.Player;

public class StartSubCommand {

    private final ArenaManager arenaManager;

    public StartSubCommand(Tnttag plugin) {
        this.arenaManager = plugin.getArenaManager();
    }

    public void onStart(Player player, String arenaName, boolean forced) {
        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) {
            ChatUtils.sendMessage(player, "commands.invalid-arena");
            return;
        }

        GameState state = arena.getGameManager().state;
        if (state == GameState.INGAME || state == GameState.ENDING) {
            ChatUtils.sendMessage(player, "arena.active");
            return;
        }

        int currentPlayers = arena.getGameManager().playerManager.getPlayerCount();
        int requiredPlayers = forced ? 2 : arena.getMinPlayers();
        if (currentPlayers < requiredPlayers) {
            String message = ChatUtils.getRaw("commands.not-enough-players");
            if (message != null) {
                ChatUtils.sendCustomMessage(player, message
                        .replace("{current}", String.valueOf(currentPlayers))
                        .replace("{required}", String.valueOf(requiredPlayers)));
            }
            return;
        }

        if (forced) {
            arena.getGameManager().setGameState(GameState.INGAME, false);
        } else {
            arena.getGameManager().start();
        }

        ChatUtils.sendMessage(player, "commands.arena-started");
    }
}
