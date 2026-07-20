package nl.juriantech.tnttag.managers;

import com.alessiodp.parties.api.interfaces.Party;
import de.simonsator.partyandfriends.spigot.api.party.PlayerParty;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.api.PlayerJoinArenaEvent;
import nl.juriantech.tnttag.api.PlayerLeaveArenaEvent;
import nl.juriantech.tnttag.enums.GameState;
import nl.juriantech.tnttag.enums.PlayerType;
import nl.juriantech.tnttag.hooks.PartiesHook;
import nl.juriantech.tnttag.hooks.PartyAndFriendsHook;
import nl.juriantech.tnttag.objects.PlayerData;
import nl.juriantech.tnttag.objects.PlayerInformation;
import nl.juriantech.tnttag.utils.ChatUtils;
import nl.juriantech.tnttag.utils.RegistryUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class PlayerManager {

    private final Tnttag plugin;
    private final GameManager gameManager;
    private final HashMap<Player, PlayerType> players;
    private final LobbyManager lobbyManager;

    public PlayerManager(Tnttag plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.players = new HashMap<>();
        this.lobbyManager = plugin.getLobbyManager();
    }
    public synchronized void addPlayer(Player player) {
        if (players.containsKey(player) || !canAcceptPlayers(1)) return;

        if (!ensureLobbyState(player, true)) return;

        int minPlayers = gameManager.arena.getMinPlayers();

        PartyAndFriendsHook pafHook = plugin.getPartyAndFriendsHook();
        if (pafHook != null && pafHook.playerIsInParty(player.getUniqueId())) {
            PlayerParty party = pafHook.getPlayerParty(player.getUniqueId());
            if (party == null || !party.getLeader().getUniqueId().equals(player.getUniqueId())) {
                ChatUtils.sendMessage(player, "party.not-the-leader");
                return;
            }

            List<Player> eligiblePlayers = eligiblePartyPlayers(player, pafHook.getPlayersOfParty(party));
            if (!canAcceptPlayers(eligiblePlayers.size())) {
                ChatUtils.sendMessage(player, "party.too-much-players");
                return;
            }
            if (!preparePartyPlayers(eligiblePlayers)) return;
            for (Player partyPlayer : eligiblePlayers) {
                internalAddPlayer(partyPlayer, minPlayers);
                if (!partyPlayer.getUniqueId().equals(player.getUniqueId())) {
                    ChatUtils.sendMessage(partyPlayer, "party.joined-game");
                }
            }
            return;
        }

        PartiesHook partiesHook = plugin.getPartiesHook();
        if (partiesHook != null) {
            Party party = partiesHook.getPlayerParty(player.getUniqueId());
            if (party != null) {
                if (!party.getLeader().equals(player.getUniqueId())) {
                    ChatUtils.sendMessage(player, "party.not-the-leader");
                    return;
                }

                List<Player> eligiblePlayers = eligiblePartyPlayers(player, partiesHook.getPlayersOfParty(party));
                if (!canAcceptPlayers(eligiblePlayers.size())) {
                    ChatUtils.sendMessage(player, "party.too-much-players");
                    return;
                }
                if (!preparePartyPlayers(eligiblePlayers)) return;
                for (Player partyPlayer : eligiblePlayers) {
                    internalAddPlayer(partyPlayer, minPlayers);
                    if (!partyPlayer.getUniqueId().equals(player.getUniqueId())) {
                        ChatUtils.sendMessage(partyPlayer, "party.joined-game");
                    }
                }
                return;
            }
        }

        internalAddPlayer(player, minPlayers);
    }

    /**
     * Adds exactly one player, bypassing party expansion. Intended for administrative force-join flows.
     */
    public synchronized boolean addPlayerDirect(Player player) {
        if (players.containsKey(player) || !canAcceptPlayers(1)) return false;
        if (!ensureLobbyState(player, true)) return false;

        internalAddPlayer(player, gameManager.arena.getMinPlayers());
        return players.containsKey(player);
    }

    private boolean preparePartyPlayers(Collection<Player> partyPlayers) {
        for (Player partyPlayer : partyPlayers) {
            if (!ensureLobbyState(partyPlayer, false)) return false;
        }
        return true;
    }

    private boolean ensureLobbyState(Player player, boolean teleport) {
        return lobbyManager.playerIsInLobby(player) || lobbyManager.enterLobby(player, teleport);
    }

    private List<Player> eligiblePartyPlayers(Player leader, Collection<Player> partyPlayers) {
        LinkedHashSet<Player> candidates = new LinkedHashSet<>();
        candidates.add(leader);
        if (partyPlayers != null) candidates.addAll(partyPlayers);

        return candidates.stream()
                .filter(Objects::nonNull)
                .filter(Player::isOnline)
                .filter(partyPlayer -> !players.containsKey(partyPlayer))
                .filter(partyPlayer -> !plugin.getArenaManager().playerIsInArena(partyPlayer))
                .toList();
    }

    private void internalAddPlayer(Player player, int minPlayers) {
        // A final centralized guard prevents command, party, or same-tick joins from exceeding capacity.
        if (players.containsKey(player) || !canAcceptPlayers(1)) return;

        PlayerJoinArenaEvent event = new PlayerJoinArenaEvent(player, gameManager.arena.getName());
        Bukkit.getPluginManager().callEvent(event);
        players.put(player, PlayerType.WAITING);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!players.containsKey(player)) return;
            gameManager.itemManager.giveWaitingItems(player);
            teleportToLobby(player);
            ChatUtils.sendMessage(player, "player.joined-arena");
            broadcast(ChatUtils.getRaw("arena.player-joined").replace("{player}", player.getName()));
        });

        if (gameManager.state != GameState.STARTING && getPlayerCount() >= minPlayers) {
            gameManager.start();
        }
    }

    public boolean canAcceptPlayers(int additionalPlayers) {
        if (additionalPlayers < 0) return false;
        if (gameManager.state == GameState.INGAME || gameManager.state == GameState.ENDING) return false;
        return getPlayerCount() + additionalPlayers <= gameManager.arena.getMaxPlayers();
    }

    public int getAvailableSlots() {
        return Math.max(0, gameManager.arena.getMaxPlayers() - getPlayerCount());
    }

    public synchronized void removePlayer(Player player, boolean message) {
        if (!players.containsKey(player)) return;
        PlayerLeaveArenaEvent event = new PlayerLeaveArenaEvent(player, gameManager.arena.getName());
        Bukkit.getPluginManager().callEvent(event);

        PlayerInformation playerInformation = plugin.getLobbyManager().getPlayerInformationMap().get(player);

        if (players.get(player) != PlayerType.SURVIVOR) {
            // The player lost his winstreak, we process that here to ensure that they can't bypass it by leaving while the game still lasts.
            // Incrementing the winstreak is done in the GameManager.
            PlayerData playerData = new PlayerData(player.getUniqueId());
            playerData.setWinstreak(0);
        }

        if (plugin.getTabHook() != null && playerInformation != null) {
            plugin.getTabHook().setPlayerPrefix(player.getUniqueId(), playerInformation.getTabPrefix());
        }

        setPlayerType(player, PlayerType.WAITING);

        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
        players.remove(player);
        if (message) {
            ChatUtils.sendMessage(player, "player.leaved-arena");
            broadcast(ChatUtils.getRaw("arena.player-leaved").replace("{player}", player.getName()));
        }

        if (Tnttag.configfile.getBoolean("global-lobby")) {
            gameManager.itemManager.giveGlobalLobbyItems(player);
            plugin.getLobbyManager().teleportToLobby(player);
            player.setTotalExperience(0);
            player.setExp(0);
        } else {
            plugin.getLobbyManager().leaveLobby(player);
        }

        if (gameManager.startRunnable != null && !gameManager.startRunnable.isCancelled() && getPlayerCount() < gameManager.arena.getMinPlayers()) {
            gameManager.startRunnable.cancel();
            broadcast(ChatUtils.getRaw("arena.countdown-stopped").replace("{player}", player.getName()));
            gameManager.setGameState(GameState.IDLE, false);
            return;
        }

        if (getPlayerCount() == 0) gameManager.stop();
        if (getPlayerCount() == 1 && gameManager.state == GameState.INGAME) {
            if (message) {
                broadcast(ChatUtils.getRaw("arena.last-player-leaved").replace("{player}", player.getName()));
            }
            gameManager.setGameState(GameState.ENDING, true);
        }

        if (players.entrySet().stream().noneMatch(p -> p.getValue() == PlayerType.TAGGER) && gameManager.state == GameState.INGAME) {
            if (message) {
                broadcast(ChatUtils.getRaw("arena.last-player-leaved").replace("{player}", player.getName()));
            }
            gameManager.stop();
        }
    }

    public boolean isIn(Player player) {
        for (Player p : players.keySet()) {
            if (p.getName().equals(player.getName())) return true;
        }
        return false;
    }

    public void setPlayerType(Player player, PlayerType type) {
        if (!players.containsKey(player)) return; //Safety check.
        if (players.get(player) == type) return; //Safety check.
        PlayerInformation playerInformation = plugin.getLobbyManager().getPlayerInformationMap().get(player);

        //If the player was a spectator before.
        if (players.get(player) == PlayerType.SPECTATOR) {
            // Make the player visible again
            player.setInvisible(false);
            player.setGameMode(GameMode.SURVIVAL);
            player.setFlying(false);
            player.setAllowFlight(false);
            if (plugin.getTabHook() != null) {
                plugin.getTabHook().showPlayerName(player.getUniqueId());
            } else {
                player.setCustomNameVisible(true);
            }
        }

        //If the player was a tagger before.
        if (players.get(player).equals(PlayerType.TAGGER)) {
            givePotionEffects(player);
            gameManager.itemManager.giveGameItems(player);
            ChatUtils.sendMessage(player, "player.tagger-removed");
            ChatUtils.sendTitle(player, "titles.untagged", 20L, 20L, 20L);

            PlayerData playerData = new PlayerData(player.getUniqueId());
            playerData.setTags(playerData.getTags() + 1);
        }

        switch (type) {
            case WAITING:
                setType(player, PlayerType.WAITING);
                player.teleport(gameManager.arena.getLobbyLocation());

                setPlayerName(player, PlayerType.WAITING);
                player.setCollidable(true);
                break;
            case SURVIVOR:
                if (players.get(player) != PlayerType.TAGGER) {
                    player.teleport(gameManager.arena.getStartLocation());
                }

                setType(player, PlayerType.SURVIVOR);
                givePotionEffects(player);
                setPlayerName(player, PlayerType.SURVIVOR);
                break;
            case TAGGER:
                if (players.get(player) != PlayerType.SURVIVOR) return; //Safety check.
                setType(player, PlayerType.TAGGER);

                givePotionEffects(player);
                gameManager.itemManager.giveTaggerItems(player);
                ChatUtils.sendTitle(player, "titles.tagged", 20L, 20L, 20L);

                PlayerData playerData = new PlayerData(player.getUniqueId());
                playerData.setTimesTagged(playerData.getTimesTagged() + 1);
                setPlayerName(player, PlayerType.TAGGER);
                break;
            case SPECTATOR:
                // The player should be invisible.
                player.setInvisible(true);
                player.setGameMode(GameMode.ADVENTURE);
                player.setAllowFlight(true);
                player.setFlying(true);
                player.setCollidable(false);

                setType(player, PlayerType.SPECTATOR);
                setPlayerName(player, PlayerType.SPECTATOR);
                break;
        }
    }

    private void setPlayerName(Player player, PlayerType playerType) {
        PlayerInformation playerInformation = plugin.getLobbyManager().getPlayerInformationMap().get(player);
        if (playerInformation == null) return;

        if (playerType == PlayerType.WAITING) {
            player.displayName(playerInformation.getDisplayName());
            player.playerListName(playerInformation.getPlayerListName());
            if (plugin.getTabHook() != null) {
                plugin.getTabHook().setPlayerPrefix(player.getUniqueId(), playerInformation.getTabPrefix());
            }
            return;
        }

        String configuredPrefix = switch (playerType) {
            case SURVIVOR -> Tnttag.customizationfile.getString("name-prefixes.survivor");
            case TAGGER -> Tnttag.customizationfile.getString("name-prefixes.tagger");
            case SPECTATOR -> Tnttag.customizationfile.getString("name-prefixes.spectator");
            default -> "";
        };
        Component prefix = ChatUtils.component(configuredPrefix).append(Component.space());
        player.displayName(prefix.append(playerInformation.getDisplayName()));
        player.playerListName(prefix.append(playerInformation.getPlayerListName()));

        if (plugin.getTabHook() != null) {
            plugin.getTabHook().setPlayerPrefix(player.getUniqueId(), ChatUtils.colorize(configuredPrefix) + " ");
            if (playerType == PlayerType.SPECTATOR) {
                plugin.getTabHook().hidePlayerName(player.getUniqueId());
            }
        }
    }

    public void broadcast(String message) {
        message = message.replace("{currentPlayers}", String.valueOf(getPlayerCount()));
        message = message.replace("{minPlayers}", String.valueOf(gameManager.arena.getMinPlayers()));
        message = message.replace("{maxPlayers}", String.valueOf(gameManager.arena.getMaxPlayers()));

        if (message.isEmpty()) return;

        for (Player player : players.keySet()) {
            player.sendMessage(ChatUtils.component(message));
        }
    }

    public void sendStartMessage() {
        for (Player player : players.keySet()) {
            for (String line : Tnttag.configfile.getStringList("startMessage")) {
                player.sendMessage(ChatUtils.component(line));
            }
        }
    }
    public void teleportToLobby(Player player) {
        Location lobbyLocation = gameManager.arena.getLobbyLocation();
        player.teleport(lobbyLocation);
    }

    public void teleportToStart() {
        Location startLocation = gameManager.arena.getStartLocation();
        for (Player player : players.keySet()) {
            player.teleport(startLocation);
        }
    }

    private void setType(Player player, PlayerType type) {
        for (Map.Entry<Player, PlayerType> entry : players.entrySet()) {
            if (entry.getKey().getName().equals(player.getName())) {
                entry.setValue(type);
            }
        }
    }

    public void givePotionEffects(Player player) {
        List<PotionEffect> activeEffects = new ArrayList<>(player.getActivePotionEffects());
        activeEffects.forEach(activePotionEffect -> player.removePotionEffect(activePotionEffect.getType()));


        for (String configuredEffect : gameManager.arena.getPotionEffects()) {
            String[] parts = configuredEffect.split(":", 3);
            if (parts.length != 3) {
                plugin.getLogger().severe("Arena '" + gameManager.arena.getName()
                        + "' has an invalid potion effect entry: " + configuredEffect);
                continue;
            }

            try {
                PotionEffect effect = new PotionEffect(
                        RegistryUtils.potionEffect(parts[0]),
                        Integer.MAX_VALUE,
                        Integer.parseInt(parts[1])
                );
                if (parts[2].equalsIgnoreCase("SURVIVORS") && players.get(player) == PlayerType.SURVIVOR) {
                    player.addPotionEffect(effect);
                } else if (parts[2].equalsIgnoreCase("TAGGERS") && players.get(player) == PlayerType.TAGGER) {
                    player.addPotionEffect(effect);
                }
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().severe("Arena '" + gameManager.arena.getName()
                        + "' has an invalid potion effect entry '" + configuredEffect + "': " + exception.getMessage());
            }
        }
    }

    public void pickPlayers(boolean pickPercentage) {
        List<Player> playersList = new ArrayList<>();
        for (Map.Entry<Player, PlayerType> entry : players.entrySet()) {
            if (entry.getValue() == PlayerType.SPECTATOR) continue;
            playersList.add(entry.getKey());
        }

        List<Player> taggers = new ArrayList<>();

        // Remove the percentage sign and convert to a double
        double taggerPercentage = Double.parseDouble(Tnttag.configfile.getString("taggers-percentage").replace("%", "")) / 100.0;
        int numTaggers = (int) Math.round(playersList.size() * taggerPercentage);

        // Shuffle the players list to introduce randomness
        Collections.shuffle(playersList);

        if (!pickPercentage || numTaggers < 1) {
            if (playersList.isEmpty()) {
                plugin.getLogger().warning("PlayerManager.pickPlayers: Tried to pick a random player, but playersList is empty!");
                return;
            }

            // If pickPercentage is true or there are no taggers based on the percentage, force assign one tagger
            Player randomTagger = playersList.get(new Random().nextInt(playersList.size()));
            taggers.add(randomTagger);
            setType(randomTagger, PlayerType.TAGGER);
            setPlayerName(randomTagger, PlayerType.TAGGER);
            ChatUtils.sendMessage(randomTagger, "player.is-tagger");
            gameManager.itemManager.giveTaggerItems(randomTagger);
        } else {
            // Assign taggers based on the percentage
            for (int i = 0; i < numTaggers; i++) {
                Player player = playersList.get(i);
                taggers.add(player);
                setType(player, PlayerType.TAGGER);
                setPlayerName(player, PlayerType.TAGGER);
                ChatUtils.sendMessage(player, "player.is-tagger");
                gameManager.itemManager.giveTaggerItems(player);
            }
        }

        for (Player p : players.keySet()) {
            // Only process players that are not spectators
            if (players.get(p) != PlayerType.SPECTATOR) {
                // Set the player's type to PlayerType.SURVIVOR if they are not a tagger
                if (!taggers.contains(p)) {
                    setType(p, PlayerType.SURVIVOR);
                }
                givePotionEffects(p);
            }
        }
    }

    public int getPlayerCount() {
        //Do NOT count spectators!!
        int count = 0;
        for (Map.Entry<Player, PlayerType> entry : players.entrySet()) {
            if (entry.getValue() == PlayerType.SPECTATOR) continue;
            count++;
        }
        return count;
    }

    public HashMap<Player, PlayerType> getPlayers() {
        return players;
    }

    public PlayerType getPlayerType(Player player) {
        return players.get(player);
    }
}
