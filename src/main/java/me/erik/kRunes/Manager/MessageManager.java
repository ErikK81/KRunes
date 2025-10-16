package me.erik.kRunes.Manager;

import me.erik.kRunes.KRunes;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MessageManager {

    private final KRunes plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public MessageManager(KRunes plugin) {
        this.plugin = plugin;
        createMessagesFile();
    }

    private void createMessagesFile() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String get(String category, String key) {
        String path = category + "." + key;
        String message = messagesConfig.getString(path, "&cMensagem não encontrada: " + path);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void reload() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void save() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
