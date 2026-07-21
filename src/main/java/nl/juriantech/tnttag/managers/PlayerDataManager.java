package nl.juriantech.tnttag.managers;

import nl.juriantech.tnttag.Tnttag;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Coalesces frequent statistic mutations into periodic main-thread YAML saves.
 */
public final class PlayerDataManager {

    private final Tnttag plugin;
    private final BukkitTask flushTask;
    private boolean dirty;

    public PlayerDataManager(Tnttag plugin) {
        this.plugin = plugin;
        this.flushTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::flush, 100L, 100L);
    }

    public void markDirty() {
        dirty = true;
    }

    public void flush() {
        if (!dirty || Tnttag.playerdatafile == null) return;
        try {
            Tnttag.playerdatafile.save();
            dirty = false;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save TNT-Tag player data.", exception);
        }
    }

    public void close() {
        flushTask.cancel();
        flush();
    }
}
