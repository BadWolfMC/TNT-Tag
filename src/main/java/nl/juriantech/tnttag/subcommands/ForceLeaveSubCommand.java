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

@Command({"tnttag", "tt"})
public class ForceLeaveSubCommand {

    private final Tnttag plugin;
    private final ArenaManager arenaManager;

    public ForceLeaveSubCommand(Tnttag plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
    }

    @Subcommand("forceleave")
    @CommandPermission("tnttag.forceleave")
    public void onLeave(Player executor, @Optional String arenaName) {
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
