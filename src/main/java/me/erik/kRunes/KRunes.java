package me.erik.kRunes;

import me.erik.kRunes.Manager.DataManager;
import me.erik.kRunes.Manager.MessageManager;
import me.erik.kRunes.Manager.RuneManager;
import me.erik.kRunes.Manager.EffectsManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class KRunes extends JavaPlugin {

    private RuneManager runeManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        // Cria a pasta do plugin, caso não exista
        saveDefaultConfig();
        messageManager = new MessageManager(this);
        // Inicializa DataManager
        DataManager dataManager = new DataManager(getDataFolder());

        // Inicializa EffectsManager (passando o plugin e o config.yml)
        EffectsManager effectsManager = new EffectsManager(this);

        // Inicializa RuneManager (passando DataManager e EffectsManager)
        runeManager = new RuneManager(this, dataManager, effectsManager);

        // Registra eventos
        Bukkit.getPluginManager().registerEvents(new Events(this), this);
        Objects.requireNonNull(this.getCommand("KRune")).setExecutor(new Commands(this));
        getLogger().info("KRunes ativado com sucesso!");
    }

    @Override
    public void onDisable() {
        getLogger().info("KRunes desativado.");
    }
    public MessageManager getMessageManager() {
        return messageManager;
    }
    public RuneManager getRuneManager() {
        return runeManager;
    }
}
