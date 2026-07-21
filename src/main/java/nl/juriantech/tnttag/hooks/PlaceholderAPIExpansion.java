package nl.juriantech.tnttag.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import nl.juriantech.tnttag.Arena;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.enums.PlayerType;
import nl.juriantech.tnttag.objects.PlayerData;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderAPIExpansion extends PlaceholderExpansion {

    private static final Pattern TOP_PLACEHOLDER = Pattern.compile(
            "^top_(wins|timestagged|tags|winstreak)_([1-9]\\d*)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final List<String> ARENA_TYPES = List.of(
            "currentPlayers", "minPlayers", "maxPlayers", "survivors", "taggers", "spectators", "state"
    );

    private final Tnttag plugin;

    public PlaceholderAPIExpansion(Tnttag plugin) {
        this.plugin = plugin;
    }

    @NotNull
    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "tnttag";
    }

    @NotNull
    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String normalized = params.toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "wins" -> playerStat(player, PlayerData::getWins);
            case "timestagged" -> playerStat(player, PlayerData::getTimesTagged);
            case "tags" -> playerStat(player, PlayerData::getTags);
            case "winstreak" -> playerStat(player, PlayerData::getWinstreak);
            case "team" -> resolveTeam(player == null ? null : player.getPlayer());
            default -> resolveStructuredPlaceholder(player, params);
        };
    }

    private String playerStat(OfflinePlayer player, java.util.function.ToIntFunction<PlayerData> getter) {
        if (player == null) return "N/A";
        return String.valueOf(getter.applyAsInt(new PlayerData(player.getUniqueId())));
    }

    private String resolveStructuredPlaceholder(OfflinePlayer player, String params) {
        Matcher topMatcher = TOP_PLACEHOLDER.matcher(params);
        if (topMatcher.matches()) {
            try {
                int position = Integer.parseInt(topMatcher.group(2));
                return resolveTop(topMatcher.group(1).toLowerCase(Locale.ROOT), position);
            } catch (NumberFormatException ignored) {
                return "N/A";
            }
        }
        if (params.regionMatches(true, 0, "arena_", 0, "arena_".length())) {
            return resolveArena(player == null ? null : player.getPlayer(), params.substring("arena_".length()));
        }
        return null;
    }

    private String resolveTeam(Player player) {
        if (player == null) return "N/A";
        Arena arena = plugin.getArenaManager().getPlayerArena(player);
        if (arena == null) return "N/A";
        PlayerType type = arena.getGameManager().playerManager.getPlayerType(player);
        return type == null ? "N/A" : type.name();
    }

    private String resolveTop(String type, int position) {
        Map<UUID, Integer> data = switch (type) {
            case "wins" -> Tnttag.getAPI().getWinsData();
            case "timestagged" -> Tnttag.getAPI().getTimesTaggedData();
            case "tags" -> Tnttag.getAPI().getTagsData();
            case "winstreak" -> Tnttag.getAPI().getWinstreakData();
            default -> Map.of();
        };

        List<Map.Entry<UUID, Integer>> sortedEntries = data.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(entry -> entry.getKey().toString()))
                .toList();
        if (position > sortedEntries.size()) return "N/A";

        Map.Entry<UUID, Integer> entry = sortedEntries.get(position - 1);
        OfflinePlayer rankedPlayer = Bukkit.getOfflinePlayer(entry.getKey());
        String playerName = rankedPlayer.getName() == null ? entry.getKey().toString() : rankedPlayer.getName();
        String format = Tnttag.customizationfile.getString("top-placeholder-formatting." + type);
        if (format == null) return playerName + " - " + entry.getValue();
        return ChatUtils.colorize(format
                .replace("%player%", playerName)
                .replace("%amount%", String.valueOf(entry.getValue())));
    }

    private String resolveArena(Player player, String payload) {
        String type = ARENA_TYPES.stream()
                .filter(candidate -> payload.toLowerCase(Locale.ROOT)
                        .endsWith("_" + candidate.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
        if (type == null) return null;

        String arenaName = payload.substring(0, payload.length() - type.length() - 1);
        Arena arena;
        if (arenaName.equalsIgnoreCase("current")) {
            if (player == null) return "Player not in arena.";
            arena = plugin.getArenaManager().getPlayerArena(player);
            if (arena == null) return "Player not in arena.";
        } else {
            arena = plugin.getArenaManager().getArenaObjects().stream()
                    .filter(candidate -> candidate.getName().equalsIgnoreCase(arenaName))
                    .findFirst()
                    .orElse(null);
            if (arena == null) return "Invalid arena";
        }

        return switch (type.toLowerCase(Locale.ROOT)) {
            case "currentplayers" -> String.valueOf(arena.getGameManager().playerManager.getPlayerCount());
            case "minplayers" -> String.valueOf(arena.getMinPlayers());
            case "maxplayers" -> String.valueOf(arena.getMaxPlayers());
            case "survivors" -> countPlayers(arena, PlayerType.SURVIVOR);
            case "taggers" -> countPlayers(arena, PlayerType.TAGGER);
            case "spectators" -> countPlayers(arena, PlayerType.SPECTATOR);
            case "state" -> arena.getGameManager().state.name();
            default -> null;
        };
    }

    private String countPlayers(Arena arena, PlayerType type) {
        if (!arena.getGameManager().isRunning()) return "0";
        long count = arena.getGameManager().playerManager.getPlayers().values().stream()
                .filter(type::equals)
                .count();
        return String.valueOf(count);
    }

    public String parse(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
