package me.erik.kRunes.Manager;

import me.erik.kRunes.KRunes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class EffectsManager {

    private final KRunes plugin;

    // Partículas e sons padrões
    private final Map<String, ParticleData> particleMap = new HashMap<>();
    private final Map<String, SoundData> soundMap = new HashMap<>();

    // Linhas persistentes por jogador (pares de blocos)
    private final Map<UUID, List<Location[]>> activeLines = new HashMap<>();

    public EffectsManager(KRunes plugin) {
        this.plugin = plugin;
        loadConfigs();
        startLineTicker();
    }

    // --- Carregar partículas e sons do config ---
    private void loadConfigs() {
        // Partículas
        particleMap.put("draw", new ParticleData(
                Particle.valueOf(plugin.getConfig().getString("default-particles.draw.type")),
                plugin.getConfig().getInt("default-particles.draw.amount"),
                (float) plugin.getConfig().getDouble("default-particles.draw.force")
        ));
        particleMap.put("activate", new ParticleData(
                Particle.valueOf(plugin.getConfig().getString("default-particles.activate.type")),
                plugin.getConfig().getInt("default-particles.activate.amount"),
                (float) plugin.getConfig().getDouble("default-particles.activate.force")
        ));
        particleMap.put("fail", new ParticleData(
                Particle.valueOf(plugin.getConfig().getString("default-particles.fail.type")),
                plugin.getConfig().getInt("default-particles.fail.amount"),
                (float) plugin.getConfig().getDouble("default-particles.fail.force")
        ));

        // Sons
        soundMap.put("draw", new SoundData(
                Sound.valueOf(Objects.requireNonNull(plugin.getConfig().getString("default-sounds.draw.sound"))),
                (float) plugin.getConfig().getDouble("default-sounds.draw.volume"),
                (float) plugin.getConfig().getDouble("default-sounds.draw.pitch")
        ));
        soundMap.put("activate", new SoundData(
                Sound.valueOf(Objects.requireNonNull(plugin.getConfig().getString("default-sounds.activate.sound"))),
                (float) plugin.getConfig().getDouble("default-sounds.activate.volume"),
                (float) plugin.getConfig().getDouble("default-sounds.activate.pitch")
        ));
        soundMap.put("fail", new SoundData(
                Sound.valueOf(Objects.requireNonNull(plugin.getConfig().getString("default-sounds.fail.sound"))),
                (float) plugin.getConfig().getDouble("default-sounds.fail.volume"),
                (float) plugin.getConfig().getDouble("default-sounds.fail.pitch")
        ));
    }

    // --- Spawn de partículas ---
    public void playParticle(Player player, String type, Location loc) {
        ParticleData data = particleMap.get(type);
        if (data == null) return;
        player.getWorld().spawnParticle(data.particle, loc, data.amount, data.force, data.force, data.force, data.force);
    }

    // --- Spawn de sons ---
    public void playSound(Player player, String type, Location loc) {
        SoundData data = soundMap.get(type);
        if (data == null) return;
        player.getWorld().playSound(loc, data.sound, data.volume, data.pitch);
    }

    // --- Adicionar linha persistente ---
    public void addLine(Player player, Location start, Location end) {
        activeLines.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())
                .add(new Location[]{start, end});
    }

    // --- Limpar todas as linhas de um jogador ---
    public void clearLines(Player player) {
        activeLines.remove(player.getUniqueId());
    }

    // --- Task para redesenhar linhas a cada tick ---
    private void startLineTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : activeLines.keySet()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;

                    List<Location[]> lines = activeLines.get(uuid);
                    for (Location[] pair : lines) {
                        drawLine(player, pair[0], pair[1]);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // a cada 2 ticks (~0.1s)
    }

    // --- Desenhar linha de partículas entre duas posições ---
    private void drawLine(Player player, Location start, Location end) {
        ParticleData data = particleMap.get("draw");
        if (data == null) return;

        double distance = start.distance(end);
        int points = (int) (distance * 5); // quantidade de partículas proporcional
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            double x = start.getX() + (end.getX() - start.getX()) * t + 0.5;
            double y = start.getY() + (end.getY() - start.getY()) * t + 0.5;
            double z = start.getZ() + (end.getZ() - start.getZ()) * t + 0.5;
            player.getWorld().spawnParticle(data.particle, x, y, z, data.amount, data.force, data.force, data.force, data.force);
        }
    }

    // --- Classes internas ---
    private static class ParticleData {
        Particle particle;
        int amount;
        float force;

        ParticleData(Particle particle, int amount, float force) {
            this.particle = particle;
            this.amount = amount;
            this.force = force;
        }
    }

    private static class SoundData {
        Sound sound;
        float volume;
        float pitch;

        SoundData(Sound sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }
    }
}
