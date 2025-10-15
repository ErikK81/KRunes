package me.erik.kRunes;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PlaceHolders {

    public static String replace(String text, Player player, Location start, Location end) {
        if (player != null) {
            text = text.replace("%player%", player.getName());
            text = text.replace("%world%", player.getWorld().getName());
        }
        if (start != null) {
            text = text.replace("%RuneStartX%", String.valueOf(start.getBlockX()));
            text = text.replace("%RuneStartY%", String.valueOf(start.getBlockY()));
            text = text.replace("%RuneStartZ%", String.valueOf(start.getBlockZ()));
        }
        if (end != null) {
            text = text.replace("%RuneEndX%", String.valueOf(end.getBlockX()));
            text = text.replace("%RuneEndY%", String.valueOf(end.getBlockY()));
            text = text.replace("%RuneEndZ%", String.valueOf(end.getBlockZ()));
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
