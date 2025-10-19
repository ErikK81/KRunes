package me.erik.kRunes;

import me.erik.kRunes.Manager.DataManager;
import me.erik.kRunes.Manager.MessageManager;
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
    private final MessageManager messages;
    private final DataManager dataManager;

    public Commands(KRunes plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.messages = plugin.getMessageManager();
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(color(messages.get("general", "prefix") + messages.get("general", "player_only")));
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
            case "delete" -> handleDeleteCommand(player, args);
            case "edit" -> handleEditCommand(player, args);
            default -> sendUsage(player);
        }

        return true;
    }

    /* ----------------------------
     * Command: /krune give <type>
     * ---------------------------- */
    private void handleGiveCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color(messages.get("general", "prefix") +
                    "&cCorrect usage: /krune give <chalk|activator>"));
            return;
        }

        String itemType = args[1].toLowerCase();
        switch (itemType) {
            case "chalk" -> giveItem(player, "itens.chalk", "kRunes_chalk");
            case "activator" -> giveItem(player, "itens.activator", "kRunes_activator");
            default -> player.sendMessage(color(messages.get("general", "prefix") +
                    messages.get("command", "give_fail")));
        }
    }

    /* -----------------------------------------
     * Command: /krune create <name> <blocks> [cmd]
     * ----------------------------------------- */
    private void handleCreateCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(color("&cCorrect usage: /krune create <rune_name> <block_amount> [command]"));
            return;
        }

        String runeName = args[1];
        int runeBlocks;

        try {
            runeBlocks = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(color("&cThe block amount must be an integer."));
            return;
        }

        String command = args.length > 3
                ? String.join(" ", List.of(args).subList(3, args.length))
                : "";

        plugin.getRuneManager().startRuneCreation(player, runeName, runeBlocks, command);
        plugin.getRuneManager().setRuneCommand(runeName, command);
        giveCreationStick(player, runeName, command);
    }

    /* -----------------------------------------
     * Command: /krune delete <name>
     * ----------------------------------------- */
    private void handleDeleteCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(color("&cCorrect usage: /krune delete <rune_name>"));
            return;
        }

        String runeName = args[1];
        boolean success = dataManager.deleteRune(runeName);

        if (success) {
            player.sendMessage(color(ChatColor.RED + "Rune '" + runeName + "' deleted successfully!"));
        } else {
            player.sendMessage(color(ChatColor.GRAY + "Rune '" + runeName + "' not found or failed to delete."));
        }
    }

    /* -----------------------------------------
     * Command: /krune edit <name> <new_command>
     * ----------------------------------------- */
    private void handleEditCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(color("&cCorrect usage: /krune edit <rune_name> <new_command>"));
            return;
        }

        String runeName = args[1];
        String newCommand = String.join(" ", List.of(args).subList(2, args.length));

        if (!dataManager.runeExists(runeName)) {
            player.sendMessage(color("&cRune '" + runeName + "' not found."));
            return;
        }

        dataManager.setRuneCommand(runeName, newCommand);
        player.sendMessage(color("&aCommand for rune '" + runeName + "' updated to: &f" + newCommand));
    }

    private void giveCreationStick(Player player, String runeName, String command) {
        giveItem(player, "itens.creation", "kRunes_creation_stick", runeName, command);
    }

    /* ----------------------------
     * Item utility methods
     * ---------------------------- */
    private void giveItem(Player player, String configPath, String keyName) {
        giveItem(player, configPath, keyName, null, null);
    }

    private void giveItem(Player player, String configPath, String keyName, String runeName, String command) {
        String materialName = plugin.getConfig().getString(configPath + ".material");
        if (materialName == null) {
            player.sendMessage(color("&cError: material not found at " + configPath));
            return;
        }

        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            player.sendMessage(color("&cInvalid material: " + materialName));
            return;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String name = plugin.getConfig().getString(configPath + ".name");
        if (name != null) {
            name = replacePlaceholders(name, runeName, command);
            meta.setDisplayName(color(name));
        }

        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList(configPath + ".lore")) {
            lore.add(color(replacePlaceholders(line, runeName, command)));
        }
        meta.setLore(lore);

        if (plugin.getConfig().contains(configPath + ".custommodeldata")) {
            meta.setCustomModelData(plugin.getConfig().getInt(configPath + ".custommodeldata"));
        }

        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, keyName), PersistentDataType.INTEGER, 1);
        item.setItemMeta(meta);
        player.getInventory().addItem(item);
    }

    /* ----------------------------
     * General utilities
     * ---------------------------- */
    private void sendUsage(Player player) {
        player.sendMessage(color("&eAvailable commands:"));
        player.sendMessage(color("&7/krune give <chalk|activator>"));
        player.sendMessage(color("&7/krune create <rune_name> <block_amount> [commands]"));
        player.sendMessage(color("&7/krune delete <rune_name>"));
        player.sendMessage(color("&7/krune edit <rune_name> <new_command>"));
    }

    private String replacePlaceholders(String text, String runeName, String command) {
        if (text == null) return "";
        text = text.replace("%rune%", runeName != null ? runeName : "");
        text = text.replace("%command%", (command != null && !command.isEmpty()) ? command : "None");
        return text;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
