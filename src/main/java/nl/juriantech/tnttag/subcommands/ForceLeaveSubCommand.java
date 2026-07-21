package nl.juriantech.tnttag.subcommands;

import nl.juriantech.tnttag.Arena;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.managers.ArenaManager;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.entity.Player;

public class ForceLeaveSubCommand {

    private final Tnttag plugin;
    private final ArenaManager arenaManager;

    public ForceLeaveSubCommand(Tnttag plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
    }

    public void onLeave(Player executor, String arenaName) {
        if (arenaName == null && !Tnttag.configfile.getBoolean("global-lobby")) {
            ChatUtils.sendConfiguredMessage(executor, "general.specify-arena");
            return;
        }

        Arena arena = arenaName == null ? null : arenaManager.getArena(arenaName);
        if (arenaName != null && arena == null) {
            ChatUtils.sendConfiguredMessage(executor, "commands.invalid-arena");
            return;
        }

        for (Player target : plugin.getServer().getOnlinePlayers()) {
            if (target.hasPermission("tnttag.bypass-forceleave")) continue;

            if (arena == null) {
                if (arenaManager.playerIsInArena(target)) {
                    arenaManager.getPlayerArena(target).getGameManager().playerManager.removePlayer(target, false);
                }
                if (plugin.getLobbyManager().playerIsInLobby(target)) {
                    plugin.getLobbyManager().leaveLobby(target);
                }
            } else if (arena.getGameManager().playerManager.isIn(target)) {
                arena.getGameManager().playerManager.removePlayer(target, false);
            }
        }

        ChatUtils.sendConfiguredMessage(executor, "commands.forceleave.success");
    }
}
