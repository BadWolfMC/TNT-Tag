package nl.juriantech.tnttag.signs;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.api.API;
import nl.juriantech.tnttag.enums.StatType;
import nl.juriantech.tnttag.objects.SimpleLocation;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

public class TopSign implements SignInterface {

    private final Location loc;
    private final int position;
    private final StatType statType;
    private final List<String> signLines;
    private final String formattedStatType;

    public TopSign(Location loc, int position, StatType statType) {
        this.loc = loc;
        this.position = position;
        this.statType = statType;
        this.signLines = Tnttag.customizationfile.getStringList("top-sign.lines");
        this.formattedStatType = Tnttag.customizationfile.getString("top-sign.types." + statType);
    }

    @Override
    public void onClick(Player player) {
        // Informational sign only.
    }

    @Override
    public void update() {
        if (loc == null || !(loc.getBlock().getState() instanceof Sign sign) || signLines.size() < 4) return;

        API api = Tnttag.getAPI();
        TreeMap<UUID, Integer> data = switch (statType) {
            case WINS -> api.getWinsData();
            case TIMESTAGGED -> api.getTimesTaggedData();
            case TAGS -> api.getTagsData();
        };

        List<Map.Entry<UUID, Integer>> topPlayers = data.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .toList();

        String playerName = Tnttag.customizationfile.getString("top-sign.no-data");
        if (position <= topPlayers.size()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(topPlayers.get(position - 1).getKey());
            if (player.getName() != null) playerName = player.getName();
        }

        for (int i = 0; i < 4; i++) {
            String line = signLines.get(i)
                    .replace("{top_type}", formattedStatType)
                    .replace("{top_position}", String.valueOf(position))
                    .replace("{player}", playerName);
            sign.getSide(Side.FRONT).line(i, ChatUtils.component(line));
        }
        sign.update(true);
    }

    @Override
    public String toString() {
        return SimpleLocation.fromLocation(loc) + ";" + position + ";" + statType;
    }

    public static TopSign fromString(String str) {
        String[] parts = str.split(";", 3);
        if (parts.length != 3) return null;

        Location location = Objects.requireNonNull(SimpleLocation.fromString(parts[0])).toLocation();
        int position = Integer.parseInt(parts[1]);
        StatType statType = StatType.valueOf(parts[2]);
        return new TopSign(location, position, statType);
    }

    @Override
    public Location getLoc() {
        return loc;
    }
}
