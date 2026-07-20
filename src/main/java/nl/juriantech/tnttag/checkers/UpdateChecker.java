package nl.juriantech.tnttag.checkers;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class UpdateChecker implements Listener {

    private static final String UPDATE_URL = "https://api.spigotmc.org/legacy/update.php?resource=105832";

    private final Tnttag plugin;
    private final String localVersion;
    private volatile String onlineVersion;
    private volatile boolean available;

    public UpdateChecker(Tnttag plugin) {
        this.plugin = plugin;
        this.localVersion = plugin.getPluginMeta().getVersion();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("tnttag.update") || !available) return;

        player.sendMessage(ChatUtils.component("<aqua>================<red>TNT-Tag<aqua>================"));
        player.sendMessage(ChatUtils.component("<white>New version available: <aqua>" + onlineVersion));
        player.sendMessage(ChatUtils.component("<white>Current version: <aqua>" + localVersion));
        player.sendMessage(ChatUtils.component("<red>Always read the changelog; updates may be breaking!"));
        player.sendMessage(ChatUtils.component("<aqua>========================================"));
    }

    public void check() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> available = checkUpdate());
    }

    private boolean checkUpdate() {
        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) URI.create(UPDATE_URL).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(5_000);
            connection.setRequestProperty("User-Agent", "TNT-Tag/" + localVersion);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String raw = reader.readLine();
                if (raw == null || raw.isBlank()) return false;
                onlineVersion = raw.split("-", 2)[0].trim();
                return compareVersions(onlineVersion, localVersion) > 0;
            }
        } catch (IOException exception) {
            plugin.getLogger().fine("Unable to check for TNT-Tag updates: " + exception.getMessage());
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private int compareVersions(String left, String right) {
        String[] leftParts = left.split("[-+.]", -1);
        String[] rightParts = right.split("[-+.]", -1);
        int length = Math.max(leftParts.length, rightParts.length);

        for (int index = 0; index < length; index++) {
            String leftPart = index < leftParts.length ? leftParts[index] : "0";
            String rightPart = index < rightParts.length ? rightParts[index] : "0";
            int comparison = compareVersionPart(leftPart, rightPart);
            if (comparison != 0) return comparison;
        }
        return 0;
    }

    private int compareVersionPart(String left, String right) {
        try {
            return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        } catch (NumberFormatException ignored) {
            return left.compareToIgnoreCase(right);
        }
    }
}
