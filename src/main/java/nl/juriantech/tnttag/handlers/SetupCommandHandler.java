package nl.juriantech.tnttag.handlers;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import nl.juriantech.tnttag.Arena;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.managers.ArenaManager;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;

public class SetupCommandHandler implements Listener {

    private final Tnttag plugin;
    private final ArenaManager arenaManager;
    private String currentStep = "";
    private Player currentPlayer;
    private String arenaName = "";
    private int minPlayers;
    private int maxPlayers;
    private Location lobbyLocation;
    private Location startLocation;

    public SetupCommandHandler(Tnttag plugin) {
        this.plugin = plugin;
        this.arenaManager = plugin.getArenaManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void start(Player player) {
        clearChat(player);
        ChatUtils.sendMessage(player, "setup.start");
        ChatUtils.sendMessage(player, "setup.enter-name");
        currentStep = "enter-name";
        currentPlayer = player;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (currentPlayer == null || !currentPlayer.equals(player)) return;

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Runnable handler = () -> handleInput(player, message);
        if (event.isAsynchronous()) {
            Bukkit.getScheduler().runTask(plugin, handler);
        } else {
            handler.run();
        }
    }

    private void handleInput(Player player, String message) {
        if (message.equalsIgnoreCase("cancel")) {
            cancel(player);
            return;
        }

        switch (currentStep) {
            case "enter-name" -> {
                if (message.isBlank() || arenaManager.getArena(message) != null) {
                    ChatUtils.sendMessage(player, "setup.enter-name");
                    return;
                }
                arenaName = message;
                clearChat(player);
                ChatUtils.sendMessage(player, "setup.name-entered");
                ChatUtils.sendMessage(player, "setup.enter-minPlayers");
                currentStep = "enter-minPlayers";
            }
            case "enter-minPlayers" -> {
                Integer parsed = parseInteger(player, message);
                if (parsed == null) return;
                if (parsed < 2) {
                    ChatUtils.sendMessage(player, "setup.minPlayers-too-low");
                    return;
                }
                minPlayers = parsed;
                clearChat(player);
                ChatUtils.sendMessage(player, "setup.minPlayers-entered");
                ChatUtils.sendMessage(player, "setup.enter-maxPlayers");
                currentStep = "enter-maxPlayers";
            }
            case "enter-maxPlayers" -> {
                Integer parsed = parseInteger(player, message);
                if (parsed == null) return;
                if (parsed <= minPlayers) {
                    ChatUtils.sendMessage(player, "setup.maxPlayers-too-low");
                    return;
                }
                maxPlayers = parsed;
                clearChat(player);
                ChatUtils.sendMessage(player, "setup.maxPlayers-entered");
                ChatUtils.sendMessage(player, "setup.set-lobbyLocation");
                currentStep = "set-lobbyLocation";
            }
            case "set-lobbyLocation" -> {
                if (!message.equalsIgnoreCase("setlobby")) return;
                lobbyLocation = player.getLocation().clone();
                clearChat(player);
                ChatUtils.sendMessage(player, "setup.lobbyLocation-set");
                ChatUtils.sendMessage(player, "setup.set-startLocation");
                currentStep = "set-startLocation";
            }
            case "set-startLocation" -> {
                if (!message.equalsIgnoreCase("setstart")) return;
                startLocation = player.getLocation().clone();
                clearChat(player);
                ChatUtils.sendMessage(player, "setup.startLocation-set");
                createArena(player);
                HandlerList.unregisterAll(this);
            }
            default -> plugin.getLogger().warning("Unknown setup step: " + currentStep);
        }
    }

    private Integer parseInteger(Player player, String message) {
        try {
            return Integer.parseInt(message);
        } catch (NumberFormatException exception) {
            ChatUtils.sendMessage(player, "general.invalid-number");
            return null;
        }
    }

    public void cancel(Player player) {
        clearChat(player);
        ChatUtils.sendMessage(player, "setup.cancelled");
        HandlerList.unregisterAll(this);
    }

    public void clearChat(Player player) {
        for (int count = 0; count < 20; count++) {
            player.sendMessage(net.kyori.adventure.text.Component.empty());
        }
    }

    public void createArena(Player player) {
        ArrayList<String> defaultPotionEffects = new ArrayList<>();
        defaultPotionEffects.add("SPEED:2:SURVIVORS");
        defaultPotionEffects.add("SPEED:3:TAGGERS");
        defaultPotionEffects.add("HEALTH_BOOST:1:TAGGERS");
        defaultPotionEffects.add("HEALTH_BOOST:1:SURVIVORS");

        Arena arena = new Arena(plugin, arenaName, startLocation, lobbyLocation, maxPlayers, minPlayers,
                defaultPotionEffects, 60, 50);
        if (!arenaManager.saveArenaToFile(arena)) {
            player.sendMessage(ChatUtils.component("<red>The arena could not be saved. Check the server console for details."));
            return;
        }
        ChatUtils.sendMessage(arena, player, "setup.finished");
        arenaManager.arenaObjects.add(arena);
    }
}
