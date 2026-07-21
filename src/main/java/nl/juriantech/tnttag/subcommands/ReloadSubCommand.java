package nl.juriantech.tnttag.subcommands;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.managers.ArenaManager;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.entity.Player;

import java.io.IOException;

public class ReloadSubCommand {

    private final Tnttag plugin;
    private final ArenaManager arenaManager;

    public ReloadSubCommand(Tnttag plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
    }

    public void onReload(Player player) throws IOException {
        if (arenaManager.hasActiveSessions()) {
            ChatUtils.sendMessage(player, "commands.reload-active-arena");
            return;
        }

        Tnttag.customizationfile.reload();
        Tnttag.configfile.reload();
        Tnttag.scoreboardFile.reload();
        plugin.getLobbyManager().load();
        arenaManager.reload();
        plugin.getItemManager().reload();

        ChatUtils.sendMessage(player, "commands.files-reloaded");
    }
}
