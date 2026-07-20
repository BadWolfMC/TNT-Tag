package nl.juriantech.tnttag.listeners;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.enums.SignType;
import nl.juriantech.tnttag.enums.StatType;
import nl.juriantech.tnttag.managers.SignManager;
import nl.juriantech.tnttag.signs.JoinSign;
import nl.juriantech.tnttag.signs.LeaveSign;
import nl.juriantech.tnttag.signs.SignInterface;
import nl.juriantech.tnttag.signs.TopSign;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public class SignListener implements Listener {

    private static final String HEADER = "[TT]";
    private final Tnttag plugin;
    private final SignManager signManager;

    public SignListener(Tnttag plugin) {
        this.plugin = plugin;
        this.signManager = plugin.getSignManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || !(event.getClickedBlock().getState() instanceof Sign sign)) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK && signManager.isPluginSign(sign)) {
            if (!player.hasPermission("tnttag.breaksigns")) {
                ChatUtils.sendMessage(player, "general.no-permission");
                event.setCancelled(true);
                return;
            }

            String arenaName = ChatUtils.plain(sign.getSide(Side.FRONT).line(1));
            if (plugin.getArenaManager().getArena(arenaName) != null) {
                signManager.removeSign(sign);
            }

            Material signMaterial = block.getType();
            block.setType(Material.AIR);
            if (player.getGameMode() == GameMode.SURVIVAL) {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(signMaterial));
            }
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && signManager.isPluginSign(sign)) {
            SignInterface pluginSign = signManager.getPluginSign(sign);
            if (pluginSign != null) pluginSign.onClick(player);
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        String header = plainLine(event, 0);
        String typeText = plainLine(event, 1).toUpperCase(Locale.ROOT);
        if (!HEADER.equals(header)) return;

        SignType signType;
        try {
            signType = SignType.valueOf(typeText);
        } catch (IllegalArgumentException exception) {
            return;
        }

        if (!player.hasPermission("tnttag.createsigns")) {
            ChatUtils.sendMessage(player, "general.no-permission");
            event.setCancelled(true);
            return;
        }

        switch (signType) {
            case JOIN -> {
                String arenaName = plainLine(event, 2);
                if (plugin.getArenaManager().getArena(arenaName) == null) return;
                signManager.addJoinSign(new JoinSign(plugin, arenaName, event.getBlock().getLocation()));
            }
            case LEAVE -> signManager.addLeaveSign(new LeaveSign(event.getBlock().getLocation()));
            case TOP -> {
                String statText = plainLine(event, 2).toUpperCase(Locale.ROOT);
                StatType statType;
                int position;
                try {
                    statType = StatType.valueOf(statText);
                    position = Integer.parseInt(plainLine(event, 3));
                } catch (IllegalArgumentException exception) {
                    return;
                }
                if (position < 1 || position > 10) return;
                signManager.addTopSign(new TopSign(event.getBlock().getLocation(), position, statType));
            }
        }
    }

    private String plainLine(SignChangeEvent event, int index) {
        return ChatUtils.plain(event.line(index));
    }
}
