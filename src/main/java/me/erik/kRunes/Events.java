package me.erik.kRunes;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class Events implements Listener {

    private final KRunes plugin;

    public Events(KRunes plugin) {
        this.plugin = plugin;
    }

    private NamespacedKey creationKey() {
        return new NamespacedKey(plugin, "kRunes_creating");
    }

    private NamespacedKey chalkKey() {
        return new NamespacedKey(plugin, "kRunes_chalk");
    }

    private NamespacedKey creationStickKey() {
        return new NamespacedKey(plugin, "kRunes_creation_stick");
    }

    @EventHandler
    public void onPlayerUseItem(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        Block block = event.getClickedBlock();
        Location loc = block.getLocation().add(0.5, 1, 0.5);

        // Giz Rúnico
        if (meta.getPersistentDataContainer().has(chalkKey(), PersistentDataType.INTEGER)) {
            event.setCancelled(true);
            plugin.getRuneManager().addChalkBlock(player, block);
            player.getWorld().spawnParticle(Particle.END_ROD, loc, 10, 0.2, 0.2, 0.2, 0.01);
            player.sendMessage("Bloco marcado com energia rúnica!");
            return;
        }

        // Cajado de Criação
        if (meta.getPersistentDataContainer().has(creationStickKey(), PersistentDataType.INTEGER)) {

            event.setCancelled(true);
            boolean finished = plugin.getRuneManager().addCreationBlock(player, block);
            player.getWorld().spawnParticle(Particle.OMINOUS_SPAWNING, loc, 15, 0.2, 0.2, 0.2, 0.01);

            if (finished) {
                player.sendMessage("Runa criada com sucesso!");
                player.getInventory().remove(item);
                player.getPersistentDataContainer().remove(creationKey());
            }
        }
    }
}
