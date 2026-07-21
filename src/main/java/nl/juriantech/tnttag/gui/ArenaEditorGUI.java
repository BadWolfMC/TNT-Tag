package nl.juriantech.tnttag.gui;

import nl.juriantech.tnttag.Arena;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.gui.menu.Menu;
import nl.juriantech.tnttag.gui.menu.MenuLayout;
import nl.juriantech.tnttag.gui.menu.MenuAction;
import nl.juriantech.tnttag.managers.ArenaManager;
import nl.juriantech.tnttag.utils.ChatUtils;
import nl.juriantech.tnttag.utils.ItemBuilder;
import nl.juriantech.tnttag.utils.RegistryUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;

public class ArenaEditorGUI {

    private static final MenuLayout DEFAULT_LAYOUT = new MenuLayout(3, Map.ofEntries(
            Map.entry("min.decrement", 1),
            Map.entry("min.value", 10),
            Map.entry("min.increment", 19),
            Map.entry("max.decrement", 2),
            Map.entry("max.value", 11),
            Map.entry("max.increment", 20),
            Map.entry("lobby", 12),
            Map.entry("start", 13),
            Map.entry("countdown.decrement", 5),
            Map.entry("countdown.value", 14),
            Map.entry("countdown.increment", 23),
            Map.entry("duration.decrement", 6),
            Map.entry("duration.value", 15),
            Map.entry("duration.increment", 24),
            Map.entry("apply", 16)
    ));

    private final Player player;
    private final Arena arena;
    private final Tnttag plugin;
    private final ArenaManager arenaManager;
    private final MenuLayout layout;

    public ArenaEditorGUI(Tnttag plugin, Player player, Arena arena) {
        this(plugin, player, arena, DEFAULT_LAYOUT);
    }

    ArenaEditorGUI(Tnttag plugin, Player player, Arena arena, MenuLayout layout) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
        this.player = player;
        this.arena = arena;
        this.layout = layout;
    }

    public void open() {
        String configuredTitle = ChatUtils.getRaw("editor-gui.title");
        String title = (configuredTitle == null ? "<gold>Arena editor: {arena}" : configuredTitle)
                .replace("{arena}", arena.getName());
        Menu menu = new Menu(plugin, layout.size(), ChatUtils.component(title));

        menu.setItem(layout.slot("min.decrement"), button(Material.GRAY_DYE, "editor-gui.decrement"), editable((clicker, event) -> {
            int newAmount = arena.getMinPlayers() - 1;
            if (newAmount < 2) {
                ChatUtils.sendMessage(clicker, "setup.minPlayers-too-low");
                return;
            }
            arena.setMinPlayers(newAmount);
            saveAndRefresh(clicker);
        }));
        menu.setItem(layout.slot("min.value"), new ItemBuilder(Material.LIGHT_WEIGHTED_PRESSURE_PLATE)
                .displayName("<gold>Minimum players: " + arena.getMinPlayers())
                .lore(ChatUtils.getRaw("editor-gui.minPlayersLore"))
                .setAmount(displayAmount(arena.getMinPlayers()))
                .hideAttributes()
                .build());
        menu.setItem(layout.slot("min.increment"), button(Material.LIME_DYE, "editor-gui.increment"), editable((clicker, event) -> {
            int newAmount = arena.getMinPlayers() + 1;
            if (newAmount >= arena.getMaxPlayers()) {
                ChatUtils.sendMessage(clicker, "setup.maxPlayers-too-low");
                return;
            }
            arena.setMinPlayers(newAmount);
            saveAndRefresh(clicker);
        }));

        menu.setItem(layout.slot("max.decrement"), button(Material.GRAY_DYE, "editor-gui.decrement"), editable((clicker, event) -> {
            int newAmount = arena.getMaxPlayers() - 1;
            if (newAmount <= arena.getMinPlayers()) {
                ChatUtils.sendMessage(clicker, "setup.maxPlayers-too-low");
                return;
            }
            arena.setMaxPlayers(newAmount);
            saveAndRefresh(clicker);
        }));
        menu.setItem(layout.slot("max.value"), new ItemBuilder(Material.HEAVY_WEIGHTED_PRESSURE_PLATE)
                .displayName("<gold>Maximum players: " + arena.getMaxPlayers())
                .lore(ChatUtils.getRaw("editor-gui.maxPlayersLore"))
                .setAmount(displayAmount(arena.getMaxPlayers()))
                .hideAttributes()
                .build());
        menu.setItem(layout.slot("max.increment"), button(Material.LIME_DYE, "editor-gui.increment"), editable((clicker, event) -> {
            arena.setMaxPlayers(arena.getMaxPlayers() + 1);
            saveAndRefresh(clicker);
        }));

        menu.setItem(layout.slot("lobby"), new ItemBuilder(Material.ITEM_FRAME)
                .displayName("<gold>Lobby location")
                .lore(ChatUtils.getRaw("editor-gui.lobbyLocationLore"))
                .hideAttributes()
                .build(), editable((clicker, event) -> {
            arena.setLobbyLocation(clicker.getLocation());
            saveAndRefresh(clicker);
        }));

        menu.setItem(layout.slot("start"), new ItemBuilder(Material.BEACON)
                .displayName("<gold>Start location")
                .lore(ChatUtils.getRaw("editor-gui.startLocationLore"))
                .hideAttributes()
                .build(), editable((clicker, event) -> {
            arena.setStartLocation(clicker.getLocation());
            saveAndRefresh(clicker);
        }));

        menu.setItem(layout.slot("countdown.decrement"), button(Material.GRAY_DYE, "editor-gui.decrement"), editable((clicker, event) -> {
            int decrement = event.isShiftClick() ? 10 : 1;
            int newAmount = arena.getCountdown() - decrement;
            if (newAmount <= 0) {
                ChatUtils.sendMessage(clicker, "general.negative-error");
                return;
            }
            arena.setCountdown(newAmount);
            saveAndRefresh(clicker);
        }));
        menu.setItem(layout.slot("countdown.value"), new ItemBuilder(Material.DAYLIGHT_DETECTOR)
                .displayName("<gold>Countdown: " + arena.getCountdown())
                .lore(ChatUtils.getRaw("editor-gui.countdownLore"))
                .setAmount(displayAmount(arena.getCountdown()))
                .hideAttributes()
                .build());
        menu.setItem(layout.slot("countdown.increment"), button(Material.LIME_DYE, "editor-gui.increment"), editable((clicker, event) -> {
            arena.setCountdown(arena.getCountdown() + (event.isShiftClick() ? 10 : 1));
            saveAndRefresh(clicker);
        }));

        menu.setItem(layout.slot("duration.decrement"), button(Material.GRAY_DYE, "editor-gui.decrement"), editable((clicker, event) -> {
            int decrement = event.isShiftClick() ? 10 : 1;
            int newAmount = arena.getRoundDuration() - decrement;
            if (newAmount <= 0) {
                ChatUtils.sendMessage(clicker, "general.negative-error");
                return;
            }
            arena.setRoundDuration(newAmount);
            saveAndRefresh(clicker);
        }));
        menu.setItem(layout.slot("duration.value"), new ItemBuilder(Material.HOPPER)
                .displayName("<gold>Round duration: " + arena.getRoundDuration())
                .lore(ChatUtils.getRaw("editor-gui.roundDurationLore"))
                .setAmount(displayAmount(arena.getRoundDuration()))
                .hideAttributes()
                .build());
        menu.setItem(layout.slot("duration.increment"), button(Material.LIME_DYE, "editor-gui.increment"), editable((clicker, event) -> {
            arena.setRoundDuration(arena.getRoundDuration() + (event.isShiftClick() ? 10 : 1));
            saveAndRefresh(clicker);
        }));

        menu.setItem(layout.slot("apply"), new ItemBuilder(Material.LEVER)
                .displayName("<gold>Apply changes")
                .build(), editable((clicker, event) -> {
            if (!arenaManager.saveArenaToFile(arena)) {
                clicker.sendMessage(ChatUtils.component("<red>The arena could not be saved. Check the server console for details."));
                return;
            }
            ChatUtils.sendMessage(clicker, "editor-gui.settings-applied");
            plugin.getServer().getScheduler().runTask(plugin, clicker::closeInventory);
        }));

        menu.fillEmpty(new ItemBuilder(RegistryUtils.material(ChatUtils.getRaw("editor-gui.emptySlotMaterial"))).build());
        menu.open(player);
    }

    private MenuAction editable(MenuAction action) {
        return (clicker, event) -> {
            if (arena.getGameManager().hasActiveSession()) {
                ChatUtils.sendMessage(clicker, "commands.arena-in-use");
                plugin.getServer().getScheduler().runTask(plugin, clicker::closeInventory);
                return;
            }
            action.handle(clicker, event);
        };
    }

    private org.bukkit.inventory.ItemStack button(Material material, String path) {
        return new ItemBuilder(material)
                .displayName(ChatUtils.getRaw(path))
                .hideAttributes()
                .build();
    }

    private void saveAndRefresh(Player clicker) {
        if (!arenaManager.saveArenaToFile(arena)) {
            clicker.sendMessage(ChatUtils.component("<red>The arena could not be saved. Check the server console for details."));
            return;
        }
        ChatUtils.sendMessage(clicker, "editor-gui.hint");
        plugin.getServer().getScheduler().runTask(plugin, this::open);
    }

    private int displayAmount(int value) {
        return Math.max(1, Math.min(64, value));
    }
}
