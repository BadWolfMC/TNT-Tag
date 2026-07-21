package nl.juriantech.tnttag.managers;

import dev.dejvokep.boostedyaml.YamlDocument;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.enums.PlayerType;
import nl.juriantech.tnttag.objects.PlayerInformation;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ScoreboardManager {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yy");

    private final Tnttag plugin;
    private final GameManager gameManager;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final YamlDocument config;
    private final BukkitTask updateTask;
    private boolean closed;

    public ScoreboardManager(Tnttag plugin, GameManager gameManager, YamlDocument config) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.config = config;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = scoreboard.registerNewObjective(
                "TNTTagStats",
                Criteria.DUMMY,
                ChatUtils.component(config.getString("scoreboard.title"))
        );
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.updateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::update, 20L, 20L);
    }

    public void apply() {
        for (Player player : gameManager.playerManager.getPlayers().keySet()) {
            player.setScoreboard(scoreboard);
        }
    }

    public void remove() {
        for (Player player : gameManager.playerManager.getPlayers().keySet()) {
            if (!player.getScoreboard().equals(scoreboard)) continue;
            PlayerInformation snapshot = plugin.getLobbyManager().getPlayerInformation(player);
            if (snapshot != null) {
                player.setScoreboard(snapshot.getScoreboard());
            }
        }
    }

    public void update() {
        scoreboard.getEntries().forEach(scoreboard::resetScores);

        int score = config.getStringList("scoreboard.lines").size();
        for (String configuredLine : config.getStringList("scoreboard.lines")) {
            String processedLine = replacePlaceholders(configuredLine);
            addLine(processedLine, score--);
        }
    }

    public void close() {
        if (closed) return;
        closed = true;
        remove();
        updateTask.cancel();
        objective.unregister();
    }

    private String replacePlaceholders(String text) {
        String replaced = text
                .replace("%state%", gameManager.getCustomizedState())
                .replace("%currentPlayers%", String.valueOf(gameManager.playerManager.getPlayerCount()))
                .replace("%maxPlayers%", String.valueOf(gameManager.arena.getMaxPlayers()))
                .replace("%taggers%", String.valueOf(count(PlayerType.TAGGER)))
                .replace("%survivors%", String.valueOf(count(PlayerType.SURVIVOR)))
                .replace("%spectators%", String.valueOf(count(PlayerType.SPECTATOR)))
                .replace("%time%", gameManager.round == null ? "N/A" : String.valueOf(gameManager.round.getRoundDuration()))
                .replace("%name%", gameManager.arena.getName())
                .replace("%date%", LocalDate.now().format(DATE_FORMAT));
        return ChatUtils.colorize(replaced);
    }

    private long count(PlayerType type) {
        return gameManager.playerManager.getPlayers().values().stream().filter(type::equals).count();
    }

    private void addLine(String text, int score) {
        if (text.isEmpty()) return;
        Score lineScore = objective.getScore(text);
        lineScore.setScore(score);
    }
}
