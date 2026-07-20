package nl.juriantech.tnttag.signs;

import nl.juriantech.tnttag.Arena;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.objects.SimpleLocation;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public class JoinSign implements SignInterface {

    private final Tnttag plugin;
    private final String arena;
    private final Location loc;
    private final List<String> signLines;

    public JoinSign(Tnttag plugin, String arena, Location loc) {
        this.plugin = plugin;
        this.arena = arena;
        this.loc = loc;
        this.signLines = Tnttag.customizationfile.getStringList("join-sign.lines");
    }

    @Override
    public void onClick(Player player) {
        player.getInventory().setHeldItemSlot(0);
        player.performCommand("tnttag join " + arena);
    }

    @Override
    public void update() {
        if (loc == null || !(loc.getBlock().getState() instanceof Sign sign)) return;

        Arena arenaObject = plugin.getArenaManager().getArena(arena);
        if (arenaObject == null || signLines.size() < 4) return;

        int currentPlayers = arenaObject.getGameManager().playerManager.getPlayerCount();
        int maxPlayers = arenaObject.getMaxPlayers();

        for (int i = 0; i < 4; i++) {
            String line = signLines.get(i)
                    .replace("{arena}", arena)
                    .replace("{state}", arenaObject.getGameManager().getCustomizedState())
                    .replace("{current_players}", String.valueOf(currentPlayers))
                    .replace("{max_players}", String.valueOf(maxPlayers));
            sign.getSide(Side.FRONT).line(i, ChatUtils.component(line));
        }
        sign.update(true);
    }

    @Override
    public String toString() {
        return arena + ";" + SimpleLocation.fromLocation(loc);
    }

    public static JoinSign fromString(Tnttag plugin, String str) {
        String[] parts = str.split(";", 2);
        if (parts.length != 2) return null;

        Location location = Objects.requireNonNull(SimpleLocation.fromString(parts[1])).toLocation();
        return new JoinSign(plugin, parts[0], location);
    }

    @Override
    public Location getLoc() {
        return loc;
    }
}
