package me.erik.kRunes;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;

public class KRunes extends JavaPlugin {

    private RuneManager runeManager;

    @Override
    public void onEnable() {
        this.runeManager = new RuneManager(this);

        getServer().getPluginManager().registerEvents(new Events(this), this);
        Objects.requireNonNull(getCommand("KRune")).setExecutor(new Commands(this));

        getLogger().info("[KRunes] Plugin Loaded!");
    }

    public RuneManager getRuneManager() {
        return runeManager;
    }
}
