package me.erik.kRunes;

import me.erik.kRunes.Manager.DataManager;
import me.erik.kRunes.Manager.RuneManager;
import me.erik.kRunes.Manager.EffectsManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class KRunes extends JavaPlugin {

    private DataManager dataManager;
    private EffectsManager effectsManager;
    private RuneManager runeManager;

    @Override
    public void onEnable() {
        // Cria a pasta do plugin, caso não exista
        saveDefaultConfig();

        // Inicializa DataManager
        dataManager = new DataManager(getDataFolder());

        // Inicializa EffectsManager (passando o plugin e o config.yml)
        effectsManager = new EffectsManager(this);

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

    // --- Getters ---
    public DataManager getDataManager() {
        return dataManager;
    }

    public EffectsManager getEffectsManager() {
        return effectsManager;
    }

    public RuneManager getRuneManager() {
        return runeManager;
    }
}
