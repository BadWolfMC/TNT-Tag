package nl.juriantech.tnttag.subcommands;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DumpSubCommand {

    private static final long CONFIRMATION_WINDOW_MILLIS = 10_000L;

    private final Tnttag plugin;
    private final Map<UUID, Long> firstExecutionTimes = new HashMap<>();

    public DumpSubCommand(Tnttag plugin) {
        this.plugin = plugin;
    }

    public void onDumpAll(Player player) {
        if (confirmed(player)) {
            plugin.getDumpManager().dumpAll(player);
        } else {
            requestConfirmation(player);
        }
    }

    public void onDumpLog(Player player) {
        if (confirmed(player)) {
            plugin.getDumpManager().dumpLog(player);
        } else {
            requestConfirmation(player);
        }
    }

    private boolean confirmed(Player player) {
        UUID playerId = player.getUniqueId();
        Long firstExecution = firstExecutionTimes.remove(playerId);
        return firstExecution != null
                && System.currentTimeMillis() - firstExecution <= CONFIRMATION_WINDOW_MILLIS;
    }

    private void requestConfirmation(Player player) {
        firstExecutionTimes.put(player.getUniqueId(), System.currentTimeMillis());
        ChatUtils.sendMessage(player, "commands.dump-warning");
        ChatUtils.sendMessage(player, "commands.dump-confirmation");
    }
}
