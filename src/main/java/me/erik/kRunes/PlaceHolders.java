package me.erik.kRunes;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PlaceHolders {

    /**
     * Substitui placeholders em um texto.
     * Suporta:
     * - %player% => nome do jogador
     * - %world% => nome do mundo do jogador
     * - %RuneStart% => coordenadas X Y Z do início da runa
     * - %RuneEnd% => coordenadas X Y Z do fim da runa
     *
     * @param text  texto com placeholders
     * @param player jogador (pode ser null)
     * @param start posição inicial da runa (pode ser null)
     * @param end   posição final da runa (pode ser null)
     * @return texto com placeholders substituídos
     */
    public static String replace(String text, Player player, Location start, Location end) {
        if (player != null) {
            text = text.replace("%player%", player.getName());
            if (player.getWorld() != null)
                text = text.replace("%world%", player.getWorld().getName());
        }

        if (start != null)
            text = text.replace("%RuneStart%", formatLocation(start));

        if (end != null)
            text = text.replace("%RuneEnd%", formatLocation(end));

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Formata uma Location para "X Y Z" (sem vírgulas), ideal para comandos do Minecraft.
     *
     * @param loc Location
     * @return string formatada
     */
    private static String formatLocation(Location loc) {
        return loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
    }
}
