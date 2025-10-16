package me.erik.kRunes;

import me.erik.kRunes.Manager.MessageManager;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Events implements Listener {

    private final KRunes plugin;
    private final MessageManager messages;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private static final long COOLDOWN_MS = 250; // 0.5s

    public Events(KRunes plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessageManager();
    }

    private NamespacedKey creationKey() {
        return new NamespacedKey(plugin, "kRunes_creating");
    }

    private NamespacedKey chalkKey() {
        return new NamespacedKey(plugin, "kRunes_chalk");
    }

    private NamespacedKey activatorKey() {
        return new NamespacedKey(plugin, "activator");
    }

    private NamespacedKey creationStickKey() {
        return new NamespacedKey(plugin, "kRunes_creation_stick");
    }

    @EventHandler
    public void onPlayerUseItem(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) return;

        Player player = event.getPlayer();

        // Cooldown de 0.5s
        if (isOnCooldown(player)) {
            event.setCancelled(true);
            return;
        }
        updateCooldown(player);

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        Block block = event.getClickedBlock();
        Location loc = block.getLocation().add(0.5, 1, 0.5);

        if (isChalk(meta)) {
            handleChalk(player, block, event);
        } else if (isActivator(meta)) {
            handleActivator(player, loc, event);
        } else if (isCreationStick(meta)) {
            handleCreationStick(player, item, block, loc, event);
        }
    }

    private boolean isOnCooldown(Player player) {
        Long last = cooldowns.get(player.getUniqueId());
        return last != null && (System.currentTimeMillis() - last) < COOLDOWN_MS;
    }

    private void updateCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private boolean isChalk(ItemMeta meta) {
        return meta.getPersistentDataContainer().has(chalkKey(), PersistentDataType.INTEGER);
    }

    private boolean isActivator(ItemMeta meta) {
        return meta.getPersistentDataContainer().has(activatorKey(), PersistentDataType.INTEGER);
    }

    private boolean isCreationStick(ItemMeta meta) {
        return meta.getPersistentDataContainer().has(creationStickKey(), PersistentDataType.INTEGER);
    }

    private void handleChalk(Player player, Block block, PlayerInteractEvent event) {
        event.setCancelled(true);
        plugin.getRuneManager().addChalkBlock(player, block);
    }

    private void handleActivator(Player player, Location loc, PlayerInteractEvent event) {
        event.setCancelled(true);
        plugin.getRuneManager().tryActivateRune(player);
        spawnParticle(player, loc, Particle.SMOKE, 10);
        player.sendMessage(messages.get("general", "prefix") + messages.get("rune", "success"));
    }

    private void handleCreationStick(Player player, ItemStack item, Block block, Location loc, PlayerInteractEvent event) {
        event.setCancelled(true);
        boolean finished = plugin.getRuneManager().addCreationBlock(player, block);
        spawnParticle(player, loc, Particle.OMINOUS_SPAWNING, 5);

        if (finished) {
            player.sendMessage(messages.get("general", "prefix") + messages.get("rune", "complete"));
            player.getInventory().remove(item);
            player.getPersistentDataContainer().remove(creationKey());
        }
    }

    private void spawnParticle(Player player, Location loc, Particle particle, int count) {
        player.getWorld().spawnParticle(particle, loc, count, 0.2, 0.2, 0.2, 0.01);
    }
}
