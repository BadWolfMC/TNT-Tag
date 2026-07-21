package nl.juriantech.tnttag.subcommands;

import nl.juriantech.tnttag.Arena;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.managers.ArenaManager;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ListSubCommand {

    private final ArenaManager arenaManager;

    public ListSubCommand(Tnttag plugin) {
        this.arenaManager = plugin.getArenaManager();
    }

    public void onList(Player player) {
        List<Arena> arenas = arenaManager.getArenaObjects();
        if (arenas.toArray().length == 0) {
            ChatUtils.sendMessage(player, "commands.no-available-arenas");
            return;
        }

        String arenaNames = arenas.stream()
                .map(Arena::getName)
                .collect(Collectors.joining(", "));
        ChatUtils.sendCustomMessage(player, ChatUtils.getRaw("commands.available-arenas").replace("{arenas}", arenaNames));
    }
}
