package nl.juriantech.tnttag.listeners;

import nl.juriantech.tnttag.Tnttag;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ItemListener implements Listener {

    private final Tnttag plugin;

    public ItemListener(Tnttag plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == null) return;

        Player player = event.getPlayer();
        if (!plugin.getLobbyManager().playerIsInLobby(player)) return;

        ItemStack itemInHand = player.getInventory().getItem(event.getHand());
        if (itemInHand == null || itemInHand.getType() == Material.AIR) return;

        String command = plugin.getItemManager().getCommand(itemInHand);
        if (command == null || command.isBlank() || command.equalsIgnoreCase("NONE")) return;

        event.setCancelled(true);
        player.performCommand(command);
    }
}
