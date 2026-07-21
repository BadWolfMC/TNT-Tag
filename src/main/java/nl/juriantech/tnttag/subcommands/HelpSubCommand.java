package nl.juriantech.tnttag.subcommands;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.entity.Player;

import java.util.List;

public class HelpSubCommand {

    public void execute(Player player) {
        List<String> help_menu = Tnttag.customizationfile.getStringList("help-menu");

        for (String message : help_menu) {
            ChatUtils.sendCustomMessage(player, message);
        }
    }
}
