package nl.juriantech.tnttag.subcommands;

import nl.juriantech.tnttag.Arena;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.managers.ArenaManager;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.logging.Level;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;


@Command({"tnttag", "tt"})
public class DeleteSubCommand {

    private final Tnttag plugin;
    private final ArenaManager arenaManager;

    public DeleteSubCommand(Tnttag plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
    }

    @Subcommand("delete")
    @CommandPermission("tnttag.delete")
    public void onDelete(Player player, Arena arena) {
        try {
            arenaManager.deleteArena(arena.getName());
            ChatUtils.sendMessage(arena, player, "commands.arena-deleted");
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete TNT-Tag arena '" + arena.getName() + "'.", exception);
            player.sendMessage(ChatUtils.component("<red>The arena could not be deleted because its data file could not be saved. Check the console for details."));
        }
    }
}
