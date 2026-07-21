package nl.juriantech.tnttag.subcommands;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.gui.ArenaSelector;
import org.bukkit.entity.Player;

public class JoinGUISubCommand {

    private final Tnttag plugin;

    public JoinGUISubCommand(Tnttag plugin) {
        this.plugin = plugin;
    }

    public void onGUIJoin(Player player) {
        new ArenaSelector(plugin, player).open();
    }
}
