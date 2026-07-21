package nl.juriantech.tnttag.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Creates diagnostic uploads without performing file or network I/O on the server thread.
 */
public class DumpManager {

    private final Tnttag plugin;

    public DumpManager(Tnttag plugin) {
        this.plugin = plugin;
    }

    public void dumpLog(CommandSender sender) {
        String uploadedMessage = Tnttag.customizationfile.getString("dump-log.uploaded");
        String failedMessage = Tnttag.customizationfile.getString("dump-log.failed");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpURLConnection connection = null;
            try {
                String logContents = getLatestServerLog();
                String boundary = UUID.randomUUID().toString();
                String lineBreak = "\r\n";

                connection = openPostConnection(
                        "https://paste.juriantech.nl/api/create.php",
                        "multipart/form-data; boundary=" + boundary
                );

                try (OutputStream outputStream = connection.getOutputStream();
                     PrintWriter writer = new PrintWriter(
                             new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true)) {
                    writer.append("--").append(boundary).append(lineBreak);
                    writer.append("Content-Disposition: form-data; name=\"contents\"").append(lineBreak);
                    writer.append(lineBreak).append(logContents).append(lineBreak);
                    writer.append("--").append(boundary).append(lineBreak);
                    writer.append("Content-Disposition: form-data; name=\"expiry\"").append(lineBreak);
                    writer.append(lineBreak).append(getExpiryDate()).append(lineBreak);
                    writer.append("--").append(boundary).append("--").append(lineBreak);
                }

                String identifier = readResponse(connection).trim();
                if (identifier.isEmpty()) {
                    throw new IOException("The paste service returned an empty identifier.");
                }
                send(sender, uploadedMessage.replace(
                        "{link}", "https://paste.juriantech.nl/view.php?id=" + identifier));
            } catch (Exception exception) {
                plugin.getLogger().log(Level.SEVERE, "Error while uploading the latest server log.", exception);
                send(sender, failedMessage);
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    public void dumpAll(CommandSender sender) {
        // Bukkit/plugin-manager access is captured on the primary thread before the asynchronous work starts.
        DumpContext context = new DumpContext(
                plugin.getPluginMeta().getVersion(),
                Bukkit.getName(),
                Bukkit.getVersion(),
                getServerPlugins(),
                Tnttag.customizationfile.getString("dump-all.uploaded"),
                Tnttag.customizationfile.getString("dump-all.failed")
        );

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> uploadAll(sender, context));
    }

    private void uploadAll(CommandSender sender, DumpContext context) {
        HttpURLConnection connection = null;
        try {
            String latestLog = getLatestServerLog();
            String boundary = "---boundary";
            String lineBreak = "\r\n";
            String postData = multipartField(boundary, "tnttag_version", context.tnttagVersion())
                    + multipartField(boundary, "server_software", context.serverSoftware())
                    + multipartField(boundary, "server_version", context.serverVersion())
                    + multipartField(boundary, "support_status", hasLeakMessages(latestLog) ? "NONE" : "FULL")
                    + multipartField(boundary, "server_plugins", context.serverPlugins())
                    + multipartField(boundary, "tnttag_files", getTnttagFiles())
                    + multipartField(boundary, "latest_log", latestLog)
                    + "--" + boundary + "--" + lineBreak;

            connection = openPostConnection(
                    "https://dumps.juriantech.nl/api.php",
                    "multipart/form-data; boundary=" + boundary
            );
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(postData.getBytes(StandardCharsets.UTF_8));
            }

            JsonObject jsonObject = JsonParser.parseString(readResponse(connection)).getAsJsonObject();
            if (jsonObject.has("error") && !jsonObject.get("error").isJsonNull()) {
                throw new IOException("Dump service error: " + jsonObject.get("error").getAsString());
            }
            if (!jsonObject.has("identifier") || jsonObject.get("identifier").isJsonNull()) {
                throw new IOException("The dump service response did not contain an identifier.");
            }

            send(sender, context.uploadedMessage().replace(
                    "{identifier}", jsonObject.get("identifier").getAsString()));
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Error while sending the dump data to the server.", exception);
            send(sender, context.failedMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private HttpURLConnection openPostConnection(String url, String contentType) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(5_000);
        connection.setReadTimeout(10_000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", contentType);
        return connection;
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        if (stream == null) {
            throw new IOException("HTTP " + status + " returned no response body.");
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        }
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + ": " + response);
        }
        return response.toString();
    }

    private String multipartField(String boundary, String name, String value) {
        String lineBreak = "\r\n";
        return "--" + boundary + lineBreak
                + "Content-Disposition: form-data; name=\"" + name + "\"" + lineBreak
                + lineBreak + value + lineBreak;
    }

    private void send(CommandSender sender, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ChatUtils.component(message)));
    }

    private boolean hasLeakMessages(String serverLog) {
        List<String> leakSites = Arrays.asList("directleaks", "spigotunlocked", "blackspigot");
        return leakSites.stream().anyMatch(serverLog::contains);
    }

    private String getServerPlugins() {
        Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
        StringJoiner pluginList = new StringJoiner(", ");
        for (Plugin installedPlugin : plugins) {
            pluginList.add(installedPlugin.getName());
        }
        return pluginList.toString();
    }

    private String getTnttagFiles() {
        File pluginDir = plugin.getDataFolder();
        StringBuilder fileContents = new StringBuilder(50_000);
        File[] files = pluginDir.isDirectory() ? pluginDir.listFiles() : null;
        if (files == null) return fileContents.toString();

        for (File file : files) {
            if (!file.isFile() || !file.getName().endsWith(".yml")) continue;
            try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                fileContents.append("-----").append(file.getName()).append("-----\n");
                String line;
                while ((line = reader.readLine()) != null) fileContents.append(line).append('\n');
                fileContents.append("-----END ").append(file.getName()).append("-----\n");
            } catch (IOException exception) {
                plugin.getLogger().log(Level.SEVERE, "Error while reading TNT-Tag file: " + file.getName(), exception);
            }
        }
        return fileContents.toString();
    }

    private String getLatestServerLog() {
        File serverLog = new File("logs/latest.log");
        StringBuilder logContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(serverLog, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) logContent.append(line).append('\n');
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Error while reading latest server log.", exception);
        }
        return logContent.toString();
    }

    private String getExpiryDate() {
        return LocalDate.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private record DumpContext(
            String tnttagVersion,
            String serverSoftware,
            String serverVersion,
            String serverPlugins,
            String uploadedMessage,
            String failedMessage
    ) {
    }
}
