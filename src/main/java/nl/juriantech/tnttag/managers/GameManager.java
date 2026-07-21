package nl.juriantech.tnttag.managers;

import nl.juriantech.tnttag.Arena;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.api.ArenaEndingEvent;
import nl.juriantech.tnttag.api.ArenaStartedEvent;
import nl.juriantech.tnttag.api.ArenaStartingEvent;
import nl.juriantech.tnttag.enums.GameState;
import nl.juriantech.tnttag.enums.PlayerType;
import nl.juriantech.tnttag.objects.PlayerData;
import nl.juriantech.tnttag.objects.Round;
import nl.juriantech.tnttag.runnables.StartRunnable;
import nl.juriantech.tnttag.utils.ChatUtils;
import nl.juriantech.tnttag.utils.ParticleUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameManager {

    private final Tnttag plugin;
    public final Arena arena;
    public GameState state = GameState.IDLE;
    public final PlayerManager playerManager;
    public final ScoreboardManager scoreboardManager;
    public final ItemManager itemManager;
    public StartRunnable startRunnable;
    public Round round;

    private final List<BukkitTask> finishCommandTasks = new ArrayList<>();
    private BukkitTask endingTask;
    private boolean closed;

    public GameManager(Tnttag plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
        this.playerManager = new PlayerManager(plugin, this);
        this.scoreboardManager = new ScoreboardManager(plugin, this, Tnttag.scoreboardFile);
        this.itemManager = plugin.getItemManager();
    }

    public void start() {
        setGameState(GameState.STARTING, false);
    }

    public void stop() {
        if (state != GameState.IDLE) {
            setGameState(GameState.ENDING, false);
        }
    }

    public void setGameState(GameState newState, boolean forceWinForTagger) {
        if (state == GameState.INGAME && newState == GameState.STARTING) return;
        if (state == GameState.ENDING && newState == GameState.STARTING) return;
        if (state == newState) return;

        switch (newState) {
            case IDLE -> enterIdleState();
            case STARTING -> enterStartingState();
            case INGAME -> enterInGameState();
            case ENDING -> enterEndingState(forceWinForTagger);
            default -> throw new IllegalStateException("Unexpected GameState value received: " + newState);
        }
    }

    private void enterIdleState() {
        cancelStartRunnable();
        cancelRound();
        cancelEndingTasks();
        scoreboardManager.remove();
        state = GameState.IDLE;
    }

    private void enterStartingState() {
        Bukkit.getPluginManager().callEvent(new ArenaStartingEvent(arena.getName()));
        cancelStartRunnable();
        state = GameState.STARTING;
        startRunnable = new StartRunnable(this);
        startRunnable.runTaskTimer(plugin, 20L, 20L);
    }

    private void enterInGameState() {
        Bukkit.getPluginManager().callEvent(new ArenaStartedEvent(arena.getName()));
        cancelStartRunnable();
        cancelRound();
        state = GameState.INGAME;

        for (Player player : playerManager.getPlayers().keySet()) {
            playerManager.setPlayerType(player, PlayerType.SURVIVOR);
            itemManager.giveGameItems(player);
        }

        playerManager.sendStartMessage();
        startRound();
        scoreboardManager.apply();
    }

    private void enterEndingState(boolean forceWinForTagger) {
        state = GameState.ENDING;
        cancelStartRunnable();
        if (round != null && !round.ended) {
            round.end(forceWinForTagger);
        } else {
            cancelRound();
        }
        scoreboardManager.remove();
        cancelEndingTasks();

        ArrayList<Player> winners = new ArrayList<>();
        HashMap<Player, PlayerType> playersCopy = new HashMap<>(playerManager.getPlayers());

        for (Map.Entry<Player, PlayerType> entry : playersCopy.entrySet()) {
            Player player = entry.getKey();
            if (entry.getValue() != PlayerType.SURVIVOR) continue;

            BukkitTask commandTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                for (String command : Tnttag.configfile.getStringList("arena-finish-commands")) {
                    if (!command.contains("[PLAYER]")) {
                        ConsoleCommandSender console = Bukkit.getConsoleSender();
                        Bukkit.dispatchCommand(console, command.replace("%winner%", player.getName()));
                    } else {
                        player.performCommand(command.replace("[PLAYER]", ""));
                    }
                }
            }, Tnttag.configfile.getInt("arena-finish-commands-delay") * 20L);
            finishCommandTasks.add(commandTask);

            PlayerData playerData = new PlayerData(player.getUniqueId());
            playerData.setWins(playerData.getWins() + 1);
            playerData.setWinstreak(playerData.getWinstreak() + 1);

            ParticleUtils.firework(player.getLocation(), 0);
            playerManager.broadcast(ChatUtils.getRaw("arena.player-win").replace("{player}", player.getName()));
            playerManager.broadcast(ChatUtils.getRaw("arena.returning-to-lobby")
                    .replace("%seconds%", String.valueOf(Tnttag.configfile.getInt("delay.after-game"))));
            ChatUtils.sendTitle(player, "titles.win", 20L, 20L, 20L);
            winners.add(player);
        }

        endingTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            endingTask = null;
            playersCopy.forEach((player, playerType) -> {
                if (playerManager.isIn(player)) {
                    playerManager.removePlayer(player, false);
                }
            });
            Bukkit.getPluginManager().callEvent(new ArenaEndingEvent(arena.getName(), playersCopy, winners));

            state = GameState.IDLE;
            round = null;
            startRunnable = null;

            String restartCommand = Tnttag.configfile.getString("bungee-mode.restart-command");
            if (Tnttag.configfile.getBoolean("bungee-mode.enabled")
                    && Tnttag.configfile.getBoolean("bungee-mode.enter-arena-instantly")
                    && restartCommand != null) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), restartCommand);
            }
        }, Tnttag.configfile.getInt("delay.after-game") * 20L);
    }

    public void startRound() {
        cancelRound();
        playerManager.teleportToStart();
        playerManager.pickPlayers(Tnttag.configfile.getBoolean("use-taggers-percentage"));
        round = new Round(plugin, this);
        round.start();
        playerManager.broadcast(ChatUtils.getRaw("arena.tagger-released"));
    }

    public void close() {
        if (closed) return;
        closed = true;
        cancelStartRunnable();
        cancelRound();
        cancelEndingTasks();
        scoreboardManager.close();
        state = GameState.IDLE;
    }

    public boolean hasActiveSession() {
        return state != GameState.IDLE || !playerManager.getPlayers().isEmpty();
    }

    private void cancelStartRunnable() {
        if (startRunnable != null) {
            if (!startRunnable.isCancelled()) {
                startRunnable.cancel();
            }
            startRunnable = null;
        }
    }

    private void cancelRound() {
        if (round != null) {
            round.cancel();
            round = null;
        }
    }

    private void cancelEndingTasks() {
        if (endingTask != null) {
            endingTask.cancel();
            endingTask = null;
        }
        for (BukkitTask task : finishCommandTasks) {
            task.cancel();
        }
        finishCommandTasks.clear();
    }

    public String getCustomizedState() {
        return ChatUtils.getRaw("state." + state.toString().toUpperCase());
    }

    public boolean isRunning() {
        return state == GameState.INGAME;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}
