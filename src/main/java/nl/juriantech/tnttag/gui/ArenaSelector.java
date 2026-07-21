package nl.juriantech.tnttag.gui;

import nl.juriantech.tnttag.Arena;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.gui.menu.Menu;
import nl.juriantech.tnttag.gui.menu.MenuLayout;
import nl.juriantech.tnttag.managers.ArenaManager;
import nl.juriantech.tnttag.utils.ChatUtils;
import nl.juriantech.tnttag.utils.ItemBuilder;
import nl.juriantech.tnttag.utils.RegistryUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArenaSelector {

    private static final int PAGE_SIZE = 45;
    private static final MenuLayout PAGED_LAYOUT = new MenuLayout(6, Map.of(
            "previous", 45,
            "page", 49,
            "next", 53
    ));

    private final Player player;
    private final Tnttag plugin;
    private final ArenaManager arenaManager;

    public ArenaSelector(Tnttag plugin, Player player) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
        this.player = player;
    }

    public void open() {
        open(0);
    }

    private void open(int requestedPage) {
        List<Arena> arenas = new ArrayList<>(arenaManager.getArenaObjects());
        int pageCount = Math.max(1, (int) Math.ceil(arenas.size() / (double) PAGE_SIZE));
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        boolean paged = pageCount > 1;
        int rows = paged ? PAGED_LAYOUT.rows() : Math.max(1, Math.min(5, (int) Math.ceil(Math.max(1, arenas.size()) / 9.0)));

        String configuredTitle = ChatUtils.getRaw("join-gui.title");
        String title = (configuredTitle == null ? "<gold>Arenas ({count})" : configuredTitle)
                .replace("{count}", String.valueOf(arenas.size()));
        Menu menu = new Menu(plugin, rows * 9, ChatUtils.component(title));

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, arenas.size());
        for (int index = start; index < end; index++) {
            Arena arena = arenas.get(index);
            int slot = index - start;
            String displayName = ChatUtils.getRaw("join-gui.arenaTitle")
                    .replace("{name}", arena.getName())
                    .replace("{state}", arena.getGameManager().getCustomizedState())
                    .replace("{current_players}", String.valueOf(arena.getGameManager().playerManager.getPlayerCount()));
            menu.setItem(slot, new ItemBuilder(RegistryUtils.material(ChatUtils.getRaw("join-gui.arenaMaterial")))
                    .displayName(displayName)
                    .lore(ChatUtils.getRaw("join-gui.arenaLore"))
                    .hideAttributes()
                    .build(), (clicker, event) -> {
                plugin.getJoinSubCommand().onJoin(clicker, arena.getName());
                plugin.getServer().getScheduler().runTask(plugin, clicker::closeInventory);
            });
        }

        if (paged) {
            if (page > 0) {
                menu.setItem(PAGED_LAYOUT.slot("previous"), new ItemBuilder(Material.ARROW)
                        .displayName("<yellow>Previous page")
                        .build(), (clicker, event) -> plugin.getServer().getScheduler().runTask(plugin, () -> open(page - 1)));
            }
            menu.setItem(PAGED_LAYOUT.slot("page"), new ItemBuilder(Material.PAPER)
                    .displayName("<gold>Page " + (page + 1) + " of " + pageCount)
                    .build());
            if (page + 1 < pageCount) {
                menu.setItem(PAGED_LAYOUT.slot("next"), new ItemBuilder(Material.ARROW)
                        .displayName("<yellow>Next page")
                        .build(), (clicker, event) -> plugin.getServer().getScheduler().runTask(plugin, () -> open(page + 1)));
            }
        }

        menu.fillEmpty(new ItemBuilder(RegistryUtils.material(ChatUtils.getRaw("join-gui.emptySlotMaterial"))).build());
        menu.open(player);
    }
}
