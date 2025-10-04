package me.erik.kRunes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;

public class RuneManager {

    private final File runesFolder;
    private final Gson gson = new Gson();

    // Cada jogador pode ter várias runas em criação
    private final Map<UUID, List<PlayerCreationData>> playerCreations = new HashMap<>();
    // Runa salva no JSON
    private final Map<String, RuneData> savedRunes = new HashMap<>();

    public RuneManager(KRunes plugin) {
        this.runesFolder = new File(plugin.getDataFolder(), "Runes");
        if (!runesFolder.exists()) runesFolder.mkdirs();
        loadRunes();
    }

    /** Inicia criação de uma nova runa */
    public void startRuneCreation(Player player, String runeName) {
        playerCreations.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())
                .add(new PlayerCreationData(runeName));
        player.sendMessage(ChatColor.GREEN + "Modo criação iniciado para a runa: " + ChatColor.YELLOW + runeName);
    }

    /** Adiciona bloco durante criação de runa */
    public boolean addCreationBlock(Player player, Block block) {
        UUID uuid = player.getUniqueId();
        List<PlayerCreationData> creations = playerCreations.get(uuid);
        if (creations == null || creations.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Você não está criando nenhuma runa.");
            return false;
        }

        PlayerCreationData current = creations.getLast();
        if (current.blocks.contains(block)) {
            player.sendMessage(ChatColor.GRAY + "Este bloco já foi marcado.");
            return false;
        }

        current.blocks.add(block);
        player.sendMessage(ChatColor.YELLOW + "Bloco adicionado (" + current.blocks.size() + "/9).");

        // Só finaliza a runa ao atingir 9 blocos
        if (current.blocks.size() >= 9) {
            saveRuneFromCreation(player, current);
            creations.remove(current); // remove da criação ativa
            return true;
        }

        return false;
    }

    /** Salva runa finalizada */
    private void saveRuneFromCreation(Player player, PlayerCreationData creation) {
        Block origin = creation.blocks.getFirst();
        List<int[]> relativePositions = new ArrayList<>();
        for (Block b : creation.blocks) {
            relativePositions.add(new int[]{
                    b.getX() - origin.getX(),
                    b.getY() - origin.getY(),
                    b.getZ() - origin.getZ()
            });
        }

        RuneData runeData = new RuneData(relativePositions, ""); // comando vazio
        savedRunes.put(creation.runeName, runeData);
        saveRune(creation.runeName, runeData);

        player.sendMessage(ChatColor.GREEN + "Runa '" + creation.runeName + "' criada e salva!");
    }

    /** Adiciona bloco de giz e verifica se alguma runa foi completada */
    public void addChalkBlock(Player player, Block block) {
        UUID uuid = player.getUniqueId();
        playerCreations.computeIfAbsent(uuid, k -> new ArrayList<>());
        PlayerDrawingData drawing = playerDrawings.computeIfAbsent(uuid, k -> new PlayerDrawingData());
        drawing.blocks.add(block);

        // só checa quando atingir 9 blocos
        if (drawing.blocks.size() >= 9) {
            checkRuneActivation(player, drawing.blocks);
            drawing.blocks.clear();
        }
    }

    /** Verifica se alguma runa foi completada */
    private void checkRuneActivation(Player player, List<Block> drawnBlocks) {
        List<String> matchedRunes = new ArrayList<>();

        for (Map.Entry<String, RuneData> entry : savedRunes.entrySet()) {
            String runeName = entry.getKey();
            RuneData data = entry.getValue();

            if (drawnBlocks.size() != data.positions.size()) continue;

            Block origin = drawnBlocks.getFirst();
            boolean match = true;

            for (int i = 0; i < drawnBlocks.size(); i++) {
                int dx = drawnBlocks.get(i).getX() - origin.getX();
                int dy = drawnBlocks.get(i).getY() - origin.getY();
                int dz = drawnBlocks.get(i).getZ() - origin.getZ();
                int[] expected = data.positions.get(i);

                if (dx != expected[0] || dy != expected[1] || dz != expected[2]) {
                    match = false;
                    break;
                }
            }

            if (match) matchedRunes.add(runeName);
        }

        if (matchedRunes.isEmpty()) {
            Location loc = drawnBlocks.getLast().getLocation().add(0.5, 1, 0.5);
            player.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, loc, 20, 0.3, 0.3, 0.3, 0.02);
            player.sendMessage(ChatColor.RED + "Runa incorreta! Pontos resetados.");
        } else {
            for (String runeName : matchedRunes) {
                RuneData data = savedRunes.get(runeName);
                activateRune(player, drawnBlocks.getFirst().getLocation(), runeName, data.command);
            }
        }
    }

    /** Executa comando e partículas da runa */
    private void activateRune(Player player, Location origin, String runeName, String command) {
        player.getWorld().spawnParticle(Particle.FLAME, origin.clone().add(0.5, 1, 0.5), 50, 0.5, 0.5, 0.5, 0.05);
        player.sendMessage(ChatColor.GOLD + "Runa '" + runeName + "' ativada!");

        if (command != null && !command.isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
    }

    /** Define comando a ser executado ao ativar runa */
    public void setRuneCommand(String runeName, String command) {
        RuneData data = savedRunes.get(runeName);
        if (data != null) {
            data.command = command;
            saveRune(runeName, data);
        }
    }

    // --- JSON Save/Load ---
    private void saveRune(String name, RuneData data) {
        File file = new File(runesFolder, name + ".json");
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadRunes() {
        if (!runesFolder.exists()) return;
        for (File file : Objects.requireNonNull(runesFolder.listFiles((dir, name) -> name.endsWith(".json")))) {
            try (Reader reader = Files.newBufferedReader(file.toPath())) {
                Type type = new TypeToken<RuneData>() {}.getType();
                RuneData data = gson.fromJson(reader, type);
                String runeName = file.getName().replace(".json", "");
                savedRunes.put(runeName, data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // --- Classes internas ---
    private static class RuneData {
        List<int[]> positions;
        String command;

        RuneData(List<int[]> positions, String command) {
            this.positions = positions;
            this.command = command;
        }
    }

    private static class PlayerCreationData {
        String runeName;
        List<Block> blocks = new ArrayList<>();

        PlayerCreationData(String runeName) {
            this.runeName = runeName;
        }
    }

    private static class PlayerDrawingData {
        List<Block> blocks = new ArrayList<>();
    }

    // Guardar desenhos temporários do jogador
    private final Map<UUID, PlayerDrawingData> playerDrawings = new HashMap<>();
}
