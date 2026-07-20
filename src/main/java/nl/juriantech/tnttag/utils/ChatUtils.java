package nl.juriantech.tnttag.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import nl.juriantech.tnttag.Arena;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.objects.PlayerData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.regex.Pattern;

public final class ChatUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final Pattern LEGACY_AMPERSAND_FORMATTING = Pattern.compile("(?i)&[0-9A-FK-ORX]");
    private static final Pattern LEGACY_SECTION_FORMATTING = Pattern.compile("(?i)§[0-9A-FK-ORX]");

    private ChatUtils() {
    }

    /**
     * Parses modern MiniMessage while retaining compatibility with existing configs that still use ampersand color codes.
     */
    public static Component component(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        if (LEGACY_SECTION_FORMATTING.matcher(message).find()) {
            return LEGACY_SECTION.deserialize(message);
        }
        if (LEGACY_AMPERSAND_FORMATTING.matcher(message).find()) {
            return LEGACY_AMPERSAND.deserialize(message);
        }
        try {
            return MINI_MESSAGE.deserialize(message);
        } catch (IllegalArgumentException ignored) {
            // Preserve user-provided text rather than failing an entire message because of one malformed tag.
            return Component.text(message);
        }
    }

    /**
     * Compatibility bridge for third-party APIs that still require a legacy String.
     */
    public static String colorize(String message) {
        return LEGACY_SECTION.serialize(component(message));
    }

    public static String plain(Component component) {
        if (component == null) return "";
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static void sendMessage(Player player, String path) {
        String configured = Tnttag.customizationfile.getString(path);
        if (configured == null || configured.isEmpty()) return;

        PlayerData playerData = new PlayerData(player.getUniqueId());
        String message = configured
                .replace("{player}", player.getName())
                .replace("{wins}", String.valueOf(playerData.getWins()))
                .replace("{timestagged}", String.valueOf(playerData.getTimesTagged()))
                .replace("{tags}", String.valueOf(playerData.getTags()));
        player.sendMessage(component(message));
    }

    public static void sendMessage(Arena arena, Player player, String path) {
        String configured = Tnttag.customizationfile.getString(path);
        if (configured == null || configured.isEmpty()) return;

        String message = configured
                .replace("{player}", player.getName())
                .replace("{arena}", arena.getName());
        player.sendMessage(component(message));
    }

    public static void sendConfiguredMessage(CommandSender sender, String path) {
        String configured = Tnttag.customizationfile.getString(path);
        if (configured == null || configured.isEmpty()) return;
        sender.sendMessage(component(configured));
    }

    public static void sendTitle(Player player, String path, long fadeIn, long stay, long fadeOut) {
        sendTitle(player, path, fadeIn, stay, fadeOut, null);
    }

    public static void sendTitle(Player player, String path, long fadeIn, long stay, long fadeOut, int seconds) {
        sendTitle(player, path, fadeIn, stay, fadeOut, Integer.valueOf(seconds));
    }

    private static void sendTitle(Player player, String path, long fadeIn, long stay, long fadeOut, Integer seconds) {
        String titleText = Tnttag.customizationfile.getString(path + ".title");
        String subtitleText = Tnttag.customizationfile.getString(path + ".subtitle");
        if ((titleText == null || titleText.isEmpty()) && (subtitleText == null || subtitleText.isEmpty())) return;

        if (seconds != null) {
            titleText = titleText == null ? "" : titleText.replace("{seconds}", String.valueOf(seconds));
            subtitleText = subtitleText == null ? "" : subtitleText.replace("{seconds}", String.valueOf(seconds));
        }

        Title.Times times = Title.Times.times(ticks(fadeIn), ticks(stay), ticks(fadeOut));
        player.showTitle(Title.title(component(titleText), component(subtitleText), times));
    }

    public static void sendActionBarMessage(Player player, String message) {
        player.sendActionBar(component(message));
    }

    public static void sendCustomMessage(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        player.sendMessage(component(message.replace("{player}", player.getName())));
    }

    public static String getRaw(String path) {
        return Tnttag.customizationfile.getString(path);
    }

    private static Duration ticks(long ticks) {
        return Duration.ofMillis(Math.max(0L, ticks) * 50L);
    }
}
