package nl.juriantech.tnttag.gui;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.gui.menu.Menu;
import nl.juriantech.tnttag.gui.menu.MenuLayout;
import nl.juriantech.tnttag.objects.PlayerData;
import nl.juriantech.tnttag.utils.ChatUtils;
import nl.juriantech.tnttag.utils.ItemBuilder;
import nl.juriantech.tnttag.utils.RegistryUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;

public class Stats {

    private static final MenuLayout DEFAULT_LAYOUT = new MenuLayout(1, Map.of(
            "wins", 3,
            "timestagged", 4,
            "tags", 5
    ));

    private final Player player;
    private final Tnttag plugin;

    public Stats(Tnttag plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        PlayerData playerData = new PlayerData(player.getUniqueId());
        Menu menu = new Menu(plugin, DEFAULT_LAYOUT.size(), ChatUtils.component(ChatUtils.getRaw("stats-gui.title")));
        menu.setItem(DEFAULT_LAYOUT.slot("wins"), new ItemBuilder(Material.PAPER)
                .displayName(ChatUtils.getRaw("stats-gui.wins").replace("{wins}", String.valueOf(playerData.getWins())))
                .build());
        menu.setItem(DEFAULT_LAYOUT.slot("timestagged"), new ItemBuilder(Material.PAPER)
                .displayName(ChatUtils.getRaw("stats-gui.timestagged").replace("{timestagged}", String.valueOf(playerData.getTimesTagged())))
                .build());
        menu.setItem(DEFAULT_LAYOUT.slot("tags"), new ItemBuilder(Material.PAPER)
                .displayName(ChatUtils.getRaw("stats-gui.tags").replace("{tags}", String.valueOf(playerData.getTags())))
                .build());
        menu.fillEmpty(new ItemBuilder(RegistryUtils.material(ChatUtils.getRaw("stats-gui.emptySlotMaterial"))).build());
        menu.open(player);
    }
}
