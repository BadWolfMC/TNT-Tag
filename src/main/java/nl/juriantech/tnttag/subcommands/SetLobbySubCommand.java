package nl.juriantech.tnttag.subcommands;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Objects;

public class SetLobbySubCommand {

    private final Tnttag plugin;

    public SetLobbySubCommand(Tnttag plugin) {
        this.plugin = plugin;
    }

    //setlobby command for the global lobby
    public void onSetLobby(Player player) throws IOException {
        Location loc = player.getLocation();

        Tnttag.configfile.set(
                "globalLobby",
                Objects.requireNonNull(loc.getWorld()).getName() + ","
                        + loc.getX() + ","
                        + loc.getY() + ","
                        + loc.getZ() + ","
                        + loc.getYaw() + ","
                        + loc.getPitch()
        );

        Tnttag.configfile.save();
        plugin.getLobbyManager().load();

        ChatUtils.sendMessage(player, "commands.global-lobby-set");
    }
}
