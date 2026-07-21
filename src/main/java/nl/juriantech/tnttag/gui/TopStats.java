package nl.juriantech.tnttag.gui;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.gui.menu.Menu;
import nl.juriantech.tnttag.gui.menu.MenuLayout;
import nl.juriantech.tnttag.utils.ChatUtils;
import nl.juriantech.tnttag.utils.ItemBuilder;
import nl.juriantech.tnttag.utils.RegistryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TopStats {

    private static final MenuLayout DEFAULT_LAYOUT = new MenuLayout(1, Map.of(
            "first", 2,
            "second", 4,
            "third", 6
    ));
    private static final List<String> POSITION_KEYS = List.of("first", "second", "third");

    private final Player player;
    private final String type;
    private final Tnttag plugin;

    public TopStats(Tnttag plugin, Player player, String type) {
        this.plugin = plugin;
        this.player = player;
        this.type = type;
    }

    public void open() {
        Map<UUID, Integer> topData;
        String topMessage;
        switch (type) {
            case "wins" -> {
                topData = Tnttag.getAPI().getWinsData();
                topMessage = ChatUtils.getRaw("top-gui.wins");
            }
            case "timestagged" -> {
                topData = Tnttag.getAPI().getTimesTaggedData();
                topMessage = ChatUtils.getRaw("top-gui.timestagged");
            }
            case "tags" -> {
                topData = Tnttag.getAPI().getTagsData();
                topMessage = ChatUtils.getRaw("top-gui.tags");
            }
            default -> {
                ChatUtils.sendMessage(player, "general.invalid-stat-type");
                return;
            }
        }

        List<Map.Entry<UUID, Integer>> topThreePlayers = topData.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .toList();
        if (topThreePlayers.isEmpty()) {
            ChatUtils.sendMessage(player, "general.not-enough-stats");
            return;
        }

        Menu menu = new Menu(plugin, DEFAULT_LAYOUT.size(), ChatUtils.component(topMessage));
        for (int index = 0; index < topThreePlayers.size(); index++) {
            Map.Entry<UUID, Integer> entry = topThreePlayers.get(index);
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = entry.getKey().toString();
            menu.setItem(DEFAULT_LAYOUT.slot(POSITION_KEYS.get(index)), new ItemBuilder(Material.PLAYER_HEAD)
                    .setSkullOwner(entry.getKey().toString())
                    .displayName("<aqua>" + name + "<gold> - <aqua>" + entry.getValue())
                    .build());
        }
        menu.fillEmpty(new ItemBuilder(RegistryUtils.material(ChatUtils.getRaw("top-gui.emptySlotMaterial"))).build());
        menu.open(player);
    }
}
