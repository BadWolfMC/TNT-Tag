package nl.juriantech.tnttag.subcommands;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.handlers.SetupCommandHandler;
import org.bukkit.entity.Player;

public class CreateSubCommand {

    private final Tnttag plugin;

    public CreateSubCommand(Tnttag plugin) {
        this.plugin = plugin;
    }

    public void onCreate(Player player) {
        new SetupCommandHandler(plugin).start(player);
    }
}
