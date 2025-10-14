package me.erik.kRunes;

import me.erik.kRunes.Manager.RuneManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class Commands implements CommandExecutor {

    private final KRunes plugin;

    public Commands(KRunes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give" -> handleGiveCommand(player, args);
            case "create" -> handleCreateCommand(player, args);
            default -> sendUsage(player);
        }

        return true;
    }

    private void handleGiveCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso correto: /KRune give <chalk|activator>");
            return;
        }

        String itemType = args[1].toLowerCase();
        switch (itemType) {
            case "chalk" -> giveItem(player, "itens.chalk", "kRunes_chalk");
            case "activator" -> giveItem(player, "itens.activator", "kRunes_activator");
            default -> player.sendMessage(ChatColor.RED + "Item desconhecido. Use: chalk ou activator.");
        }
    }

    private void handleCreateCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Uso correto: /KRune create <nome_da_runa> <quantidade> [comando]");
            return;
        }

        String runeName = args[1];
        int runeBlocks;

        try {
            runeBlocks = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "A quantidade de blocos deve ser um número inteiro.");
            return;
        }

        String command = (args.length > 3)
                ? String.join(" ", List.of(args).subList(3, args.length))
                : "";

        plugin.getRuneManager().startRuneCreation(player, runeName, runeBlocks, command);
        plugin.getRuneManager().setRuneCommand(runeName, command);

        giveCreationStick(player, runeName, command);
    }

    private void giveCreationStick(Player player, String runeName, String command) {
        giveItem(player, "itens.creation", "kRunes_creation_stick", runeName, command);
    }

    private void giveItem(Player player, String configPath, String keyName) {
        giveItem(player, configPath, keyName, null, null);
    }

    private void giveItem(Player player, String configPath, String keyName, String runeName, String command) {
        Material material = Material.valueOf(plugin.getConfig().getString(configPath + ".material"));
        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Nome
        String name = plugin.getConfig().getString(configPath + ".name");
        if (name != null) {
            if (runeName != null) name = name.replace("%rune%", runeName);
            if (command != null) name = name.replace("%command%", command);
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        }

        // Lore
        List<String> lore = new ArrayList<>();
        List<String> configLore = plugin.getConfig().getStringList(configPath + ".lore");
        for (String line : configLore) {
            if (runeName != null) line = line.replace("%rune%", runeName);
            if (command != null) line = line.replace("%command%", command.isEmpty() ? "Nenhum" : command);
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(lore);

        // Custom model data
        if (plugin.getConfig().contains(configPath + ".custommodeldata")) {
            meta.setCustomModelData(plugin.getConfig().getInt(configPath + ".custommodeldata"));
        }

        // Persistent data (opcional)
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, keyName), PersistentDataType.INTEGER, 1);

        item.setItemMeta(meta);
        player.getInventory().addItem(item);
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Uso correto:");
        player.sendMessage(ChatColor.GRAY + "/KRune give <chalk|activator>");
        player.sendMessage(ChatColor.GRAY + "/KRune create <nome_da_runa> <quantidade> [comando]");
    }
}
