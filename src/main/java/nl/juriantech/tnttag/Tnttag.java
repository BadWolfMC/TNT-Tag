package nl.juriantech.tnttag;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import nl.juriantech.tnttag.api.API;
import nl.juriantech.tnttag.checkers.UpdateChecker;
import nl.juriantech.tnttag.commands.TntTagCommand;
import nl.juriantech.tnttag.gui.menu.MenuListener;
import nl.juriantech.tnttag.hooks.PlaceholderAPIExpansion;
import nl.juriantech.tnttag.hooks.TabHook;
import nl.juriantech.tnttag.listeners.EntityDamageByEntityListener;
import nl.juriantech.tnttag.listeners.InventoryClickListener;
import nl.juriantech.tnttag.listeners.ItemListener;
import nl.juriantech.tnttag.listeners.LeaveListener;
import nl.juriantech.tnttag.listeners.PlayerJoinListener;
import nl.juriantech.tnttag.listeners.ProtectionListener;
import nl.juriantech.tnttag.listeners.SignListener;
import nl.juriantech.tnttag.managers.ArenaManager;
import nl.juriantech.tnttag.managers.DumpManager;
import nl.juriantech.tnttag.managers.ItemManager;
import nl.juriantech.tnttag.managers.LobbyManager;
import nl.juriantech.tnttag.managers.PlayerDataManager;
import nl.juriantech.tnttag.managers.SignManager;
import nl.juriantech.tnttag.runnables.SignUpdateRunnable;
import nl.juriantech.tnttag.subcommands.JoinSubCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public class Tnttag extends JavaPlugin {

    private static Tnttag instance;
    private static API api;

    public static YamlDocument arenasfile;
    public static YamlDocument customizationfile;
    public static YamlDocument configfile;
    public static YamlDocument playerdatafile;
    public static YamlDocument signsdatafile;
    public static YamlDocument itemsfile;
    public static YamlDocument scoreboardFile;

    private ArenaManager arenaManager;
    private UpdateChecker updateChecker;
    private SignManager signManager;
    private LobbyManager lobbyManager;
    private ItemManager itemManager;
    private DumpManager dumpManager;
    private PlayerDataManager playerDataManager;
    private TabHook tabHook;
    private EntityDamageByEntityListener entityDamageByEntityListener;
    private PlaceholderAPIExpansion placeholderAPIExpansion;
    private JoinSubCommand joinSubCommand;
    private boolean initialized;

    @Override
    public void onEnable() {
        instance = this;

        updateChecker = new UpdateChecker(this);
        updateChecker.check();

        files();
        managers();
        commands();
        listeners();
        runnables();
        api = new API(this);
        hooks();
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // World-backed locations and signs must load after worlds and dependent managers are available.
        getServer().getScheduler().runTaskLater(this, () -> {
            try {
                arenaManager.loadArenasFromFile();
                if (configfile.getString("globalLobby") != null) {
                    lobbyManager.load();
                }
                signManager.loadSigns();
                initialized = true;
                getLogger().info("TNT-Tag has been enabled.");
            } catch (Exception exception) {
                getLogger().log(Level.SEVERE, "TNT-Tag could not finish loading its arena and lobby data.", exception);
                getServer().getPluginManager().disablePlugin(this);
            }
        }, 20L);

        for (Player player : Bukkit.getOnlinePlayers()) {
            itemManager.cleanupManagedItems(player);
        }
    }

    private void hooks() {
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().info("PlaceholderAPI detected; enabling the hook.");
            placeholderAPIExpansion = new PlaceholderAPIExpansion(this);
            placeholderAPIExpansion.register();
        }

        if (getServer().getPluginManager().isPluginEnabled("TAB")) {
            getLogger().info("TAB detected; enabling the hook.");
            tabHook = new TabHook(this);
        }
    }

    private void runnables() {
        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, new SignUpdateRunnable(this), 0L, 200L);
    }

    private void managers() {
        arenaManager = new ArenaManager(this);
        signManager = new SignManager(this);
        itemManager = new ItemManager(this);
        itemManager.load();
        lobbyManager = new LobbyManager(this);
        dumpManager = new DumpManager(this);
        playerDataManager = new PlayerDataManager(this);
    }

    private YamlDocument loadFile(String fileName) {
        try {
            return YamlDocument.create(new File(getDataFolder(), fileName), getResource(fileName));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load " + fileName, exception);
        }
    }

    private void files() {
        arenasfile = loadFile("arenas.yml");
        customizationfile = loadFile("customization.yml");
        configfile = loadFile("config.yml");
        playerdatafile = loadFile("playerdata.yml");
        signsdatafile = loadFile("signs.yml");
        itemsfile = loadFile("items.yml");
        scoreboardFile = loadFile("scoreboard.yml");
    }

    private void commands() {
        joinSubCommand = new JoinSubCommand(this);
        TntTagCommand command = new TntTagCommand(this, joinSubCommand);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        command.build(),
                        "A TNT-Tag minigame for Paper.",
                        List.of("tt")
                )
        );
    }

    private void listeners() {
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        entityDamageByEntityListener = new EntityDamageByEntityListener(this);
        getServer().getPluginManager().registerEvents(entityDamageByEntityListener, this);
        getServer().getPluginManager().registerEvents(updateChecker, this);
        getServer().getPluginManager().registerEvents(new LeaveListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new SignListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
    }

    @Override
    public void onDisable() {
        // Keep shutdown steps independent so one save or integration failure cannot prevent player restoration.
        shutdownStep("end active arenas", () -> {
            if (arenaManager != null) arenaManager.endAllArenas();
        });
        shutdownStep("restore lobby players", () -> {
            if (lobbyManager == null) return;
            for (Player player : List.copyOf(Bukkit.getOnlinePlayers())) {
                if (lobbyManager.playerIsInLobby(player)) {
                    lobbyManager.leaveLobby(player, false);
                }
            }
        });
        shutdownStep("save signs", () -> {
            if (signManager != null) signManager.saveSigns();
        });
        shutdownStep("save arenas", () -> {
            if (arenaManager != null) arenaManager.saveArenasToFile();
        });
        shutdownStep("remove managed items", () -> {
            if (itemManager == null) return;
            for (Player player : Bukkit.getOnlinePlayers()) {
                itemManager.cleanupManagedItems(player);
            }
        });
        shutdownStep("flush player data", () -> {
            if (playerDataManager != null) playerDataManager.close();
        });

        api = null;
        instance = null;

        if (initialized) {
            getLogger().info("TNT-Tag has been disabled.");
        } else {
            getLogger().warning("TNT-Tag was disabled before initialization completed.");
        }
    }

    private void shutdownStep(String description, Runnable action) {
        try {
            action.run();
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Failed to " + description + " while disabling TNT-Tag.", exception);
        }
    }

    public void connectToServer(Player player, String serverName) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeUTF("Connect");
            output.writeUTF(serverName);
            player.sendPluginMessage(this, "BungeeCord", bytes.toByteArray());
        } catch (IOException exception) {
            // ByteArrayOutputStream does not normally throw, but keep the failure visible if the contract changes.
            getLogger().log(Level.SEVERE, "Unable to create the BungeeCord transfer message.", exception);
        }
    }

    public static Tnttag getInstance() {
        return instance;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public SignManager getSignManager() {
        return signManager;
    }

    public static API getAPI() {
        return api;
    }

    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public DumpManager getDumpManager() {
        return dumpManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public TabHook getTabHook() {
        return tabHook;
    }

    public PlaceholderAPIExpansion getPlaceholderAPIExpansion() {
        return placeholderAPIExpansion;
    }

    public EntityDamageByEntityListener getEntityDamageByEntityListener() {
        return entityDamageByEntityListener;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public JoinSubCommand getJoinSubCommand() {
        return joinSubCommand;
    }
}
