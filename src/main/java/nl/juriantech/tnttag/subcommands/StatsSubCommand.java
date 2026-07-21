package nl.juriantech.tnttag.subcommands;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.gui.Stats;
import org.bukkit.entity.Player;

public class StatsSubCommand {

    private final Tnttag plugin;

    public StatsSubCommand(Tnttag plugin) {
        this.plugin = plugin;
    }

    public void onStats(Player player) {
        new Stats(plugin, player).open();
    }
}
