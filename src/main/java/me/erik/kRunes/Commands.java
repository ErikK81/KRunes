package me.erik.kRunes;

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
            case "chalk" -> giveChalk(player);
            case "activator" -> giveActivator(player);
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
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        if (meta == null) return;

        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Cajado de Criação");
        meta.setLore(List.of(
                ChatColor.GRAY + "Use para desenhar a runa: " + runeName,
                ChatColor.GRAY + "Sem limite de blocos.",
                ChatColor.GRAY + (command.isEmpty() ? "Sem comando definido." : "Comando: " + command)
        ));

        NamespacedKey key = new NamespacedKey(plugin, "kRunes_creation_stick");
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);

        stick.setItemMeta(meta);
        player.getInventory().addItem(stick);
        player.sendMessage(ChatColor.GREEN + "Modo de criação iniciado para a runa: " + ChatColor.YELLOW + runeName);
    }

    private void giveChalk(Player player) {
        ItemStack chalk = new ItemStack(Material.STICK);
        ItemMeta meta = chalk.getItemMeta();
        if (meta == null) return;

        meta.setDisplayName(ChatColor.AQUA + "Giz Rúnico");
        meta.setLore(List.of(ChatColor.GRAY + "Use para desenhar runas no chão."));

        NamespacedKey key = new NamespacedKey(plugin, "kRunes_chalk");
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);

        chalk.setItemMeta(meta);
        player.getInventory().addItem(chalk);
        player.sendMessage(ChatColor.GREEN + "Você recebeu o Giz Rúnico!");
    }

    private void giveActivator(Player player) {
        ItemStack activator = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = activator.getItemMeta();
        if (meta == null) return;

        meta.setDisplayName(ChatColor.GOLD + "Ativador Rúnico");
        meta.setLore(List.of(ChatColor.GRAY + "Use para ativar runas no chão."));

        NamespacedKey key = new NamespacedKey(plugin, "kRunes_activator");
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);

        activator.setItemMeta(meta);
        player.getInventory().addItem(activator);
        player.sendMessage(ChatColor.GREEN + "Você recebeu o Ativador Rúnico!");
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Uso correto:");
        player.sendMessage(ChatColor.GRAY + "/KRune give <chalk|activator>");
        player.sendMessage(ChatColor.GRAY + "/KRune create <nome_da_runa> <quantidade> [comando]");
    }
}
