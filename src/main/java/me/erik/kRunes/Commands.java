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
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Uso correto: /KRune give chalk ou /KRune create <runa> [comando]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                if (args.length >= 2 && args[1].equalsIgnoreCase("chalk")) {
                    giveChalk(player);
                } else {
                    player.sendMessage(ChatColor.RED + "Uso correto: /KRune give chalk");
                }
                break;

            case "create":
                if (args.length >= 2) {
                    String runeName = args[1];
                    String command = "";

                    if (args.length > 2) {
                        // junta todos os argumentos depois do nome da runa em um comando
                        StringBuilder sb = new StringBuilder();
                        for (int i = 2; i < args.length; i++) {
                            sb.append(args[i]).append(" ");
                        }
                        command = sb.toString().trim();
                    }

                    // inicia criação da runa
                    plugin.getRuneManager().startRuneCreation(player, runeName);
                    plugin.getRuneManager().setRuneCommand(runeName, command);
                    giveCreationStick(player, runeName, command);

                } else {
                    player.sendMessage(ChatColor.RED + "Uso correto: /KRune create <nome_da_runa> [comando]");
                }
                break;

            default:
                player.sendMessage(ChatColor.RED + "Comando desconhecido. Use /KRune give chalk ou /KRune create <runa> [comando]");
                break;
        }

        return true;
    }

    private void giveCreationStick(Player player, String runeName, String command) {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        assert meta != null;

        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Cajado de Criação");
        meta.setLore(List.of(
                ChatColor.GRAY + "Use para desenhar a runa: " + runeName,
                ChatColor.GRAY + "Máximo de 9 blocos.",
                ChatColor.GRAY + (command.isEmpty() ? "Sem comando definido." : "Comando: " + command)
        ));

        NamespacedKey creationStickKey = new NamespacedKey(plugin, "kRunes_creation_stick");
        meta.getPersistentDataContainer().set(creationStickKey, PersistentDataType.INTEGER, 1);

        stick.setItemMeta(meta);

        player.getInventory().addItem(stick);
        player.sendMessage(ChatColor.GREEN + "Modo de criação iniciado para a runa: " + ChatColor.YELLOW + runeName);
    }

    private void giveChalk(Player player) {
        ItemStack giz = new ItemStack(Material.STICK);
        ItemMeta meta = giz.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.AQUA + "Giz Rúnico");
        meta.setLore(List.of(ChatColor.GRAY + "Use para desenhar runas no chão."));

        NamespacedKey key = new NamespacedKey(plugin, "kRunes_chalk");
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);

        giz.setItemMeta(meta);
        player.getInventory().addItem(giz);
        player.sendMessage(ChatColor.GREEN + "Você recebeu o Giz Rúnico!");
    }


}
