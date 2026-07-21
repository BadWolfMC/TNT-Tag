package nl.juriantech.tnttag.subcommands;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.gui.TopStats;
import org.bukkit.entity.Player;

public class TopSubCommand {

    private final Tnttag plugin;

    public TopSubCommand(Tnttag plugin) {
        this.plugin = plugin;
    }

    public void onTop(Player player, String type) {
        new TopStats(plugin, player, type).open();
    }
}
