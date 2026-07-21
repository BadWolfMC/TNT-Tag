package nl.juriantech.tnttag.subcommands;

import nl.juriantech.tnttag.Arena;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.managers.ArenaManager;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.entity.Player;

public class RandomJoinSubCommand {

    private final ArenaManager arenaManager;

    public RandomJoinSubCommand(Tnttag plugin) {
        this.arenaManager = plugin.getArenaManager();
    }

    public void onJoin(Player player) {
        if (arenaManager.playerIsInArena(player)) {
            ChatUtils.sendMessage(player, "player.already-in-game");
            return;
        }

        Arena destination = arenaManager.getArenaObjects().stream()
                .filter(arena -> !arena.getGameManager().isRunning())
                .filter(arena -> arena.getGameManager().playerManager.canAcceptPlayers(1))
                .findFirst()
                .orElse(null);
        if (destination == null) {
            ChatUtils.sendMessage(player, "commands.no-available-arenas");
            return;
        }

        destination.getGameManager().playerManager.addPlayer(player);
    }
}
