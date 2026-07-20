package nl.juriantech.tnttag.subcommands;

import nl.juriantech.tnttag.Arena;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.managers.ArenaManager;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.List;

@Command({"tnttag", "tt"})
public class ForceJoinSubCommand {

    private final Tnttag plugin;
    private final ArenaManager arenaManager;

    public ForceJoinSubCommand(Tnttag plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
    }

    @Subcommand("forcejoin")
    @CommandPermission("tnttag.forcejoin")
    public void onJoin(Player executor, @Optional String arenaName) {
        if (arenaName == null && !Tnttag.configfile.getBoolean("global-lobby")) {
            ChatUtils.sendConfiguredMessage(executor, "general.specify-arena");
            return;
        }

        Arena arena = arenaName == null ? null : arenaManager.getArena(arenaName);
        if (arenaName != null && arena == null) {
            ChatUtils.sendConfiguredMessage(executor, "commands.invalid-arena");
            return;
        }
        if (arena != null && arena.getGameManager().isRunning()) {
            ChatUtils.sendConfiguredMessage(executor, "arena.active");
            return;
        }

        List<? extends Player> eligiblePlayers = plugin.getServer().getOnlinePlayers().stream()
                .filter(target -> !target.hasPermission("tnttag.bypass-forcejoin"))
                .filter(target -> arena == null || !arenaManager.playerIsInArena(target))
                .toList();

        if (arena != null && !arena.getGameManager().playerManager.canAcceptPlayers(eligiblePlayers.size())) {
            ChatUtils.sendConfiguredMessage(executor, "arena.full");
            return;
        }

        for (Player target : eligiblePlayers) {
            if (arena == null) {
                if (!plugin.getLobbyManager().playerIsInLobby(target)) {
                    plugin.getLobbyManager().enterLobby(target, true);
                }
            } else {
                arena.getGameManager().playerManager.addPlayerDirect(target);
            }
        }

        ChatUtils.sendConfiguredMessage(executor, "commands.forcejoin.success");
    }
}
