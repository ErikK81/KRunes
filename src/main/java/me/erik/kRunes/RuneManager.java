package me.erik.kRunes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;

public class RuneManager {

    private final File runesFolder;
    private final Gson gson = new Gson();

    private final Map<UUID, List<PlayerCreationData>> playerCreations = new HashMap<>();
    private final Map<String, RuneData> savedRunes = new HashMap<>();
    private final Map<UUID, PlayerDrawingData> playerDrawings = new HashMap<>();

    public RuneManager(KRunes plugin) {
        this.runesFolder = new File(plugin.getDataFolder(), "Runes");
        if (!runesFolder.exists()) runesFolder.mkdirs();
        loadRunes();
    }

    // --- Criação de runas ---
    public void startRuneCreation(Player player, String runeName, int blockCount, String command) {
        playerCreations.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())
                .add(new PlayerCreationData(runeName, blockCount, command));

        player.sendMessage(ChatColor.GREEN + "Modo criação iniciado para a runa: " + ChatColor.YELLOW + runeName);
        player.sendMessage(ChatColor.AQUA + "Blocos necessários: " + ChatColor.YELLOW + blockCount);
        player.sendMessage(ChatColor.GRAY + "Comando: " + ChatColor.WHITE + command);
    }

    public boolean addCreationBlock(Player player, Block block) {
        PlayerCreationData current = getCurrentCreation(player);
        if (current == null) return false;

        if (current.blocks.contains(block)) {
            player.sendMessage(ChatColor.GRAY + "Este bloco já foi marcado.");
            return false;
        }

        current.blocks.add(block);
        player.sendMessage(ChatColor.YELLOW + "Bloco adicionado (" + current.blocks.size() + "/" + current.requiredBlocks + ").");

        if (current.blocks.size() >= current.requiredBlocks) {
            saveRuneFromCreation(player, current);
            removeCurrentCreation(player, current);
            return true;
        }
        return false;
    }

    private PlayerCreationData getCurrentCreation(Player player) {
        List<PlayerCreationData> creations = playerCreations.get(player.getUniqueId());
        if (creations == null || creations.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Você não está criando nenhuma runa.");
            return null;
        }
        return creations.get(creations.size() - 1);
    }

    private void removeCurrentCreation(Player player, PlayerCreationData creation) {
        List<PlayerCreationData> creations = playerCreations.get(player.getUniqueId());
        if (creations != null) creations.remove(creation);
    }

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

        RuneData runeData = new RuneData(relativePositions, creation.command);
        savedRunes.put(creation.runeName, runeData);
        saveRune(creation.runeName, runeData);

        player.sendMessage(ChatColor.GREEN + "Runa '" + creation.runeName + "' criada e salva com sucesso!");
        player.sendMessage(ChatColor.GRAY + "Comando: " + ChatColor.WHITE + creation.command);
    }

    // --- Giz rúnico ---
    public void addChalkBlock(Player player, Block block) {
        PlayerDrawingData drawing = playerDrawings.computeIfAbsent(player.getUniqueId(), k -> new PlayerDrawingData());
        drawing.blocks.add(block);
        player.sendMessage(ChatColor.YELLOW + "Ponto adicionado (" + drawing.blocks.size() + ").");
    }

    // --- Ativação de runa ---
    public void tryActivateRune(Player player) {
        PlayerDrawingData drawing = playerDrawings.get(player.getUniqueId());
        if (drawing == null || drawing.blocks.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Você ainda não desenhou uma runa.");
            return;
        }

        checkRuneActivation(player, drawing.blocks);
        drawing.blocks.clear();
    }

    private void checkRuneActivation(Player player, List<Block> drawnBlocks) {
        List<String> matchedRunes = new ArrayList<>();

        for (Map.Entry<String, RuneData> entry : savedRunes.entrySet()) {
            String runeName = entry.getKey();
            RuneData data = entry.getValue();

            if (drawnBlocks.size() != data.positions.size()) continue;

            if (matchesRune(drawnBlocks, data.positions)) {
                matchedRunes.add(runeName);
            }
        }

        if (matchedRunes.isEmpty()) {
            spawnFailureParticles(player, drawnBlocks.getLast().getLocation());
            player.sendMessage(ChatColor.RED + "Runa incorreta! Pontos resetados.");
        } else {
            for (String runeName : matchedRunes) {
                RuneData data = savedRunes.get(runeName);
                activateRune(player, drawnBlocks.getFirst().getLocation(), runeName, data.command);
            }
        }
    }

    private boolean matchesRune(List<Block> drawn, List<int[]> positions) {
        Block origin = drawn.getFirst();
        for (int i = 0; i < drawn.size(); i++) {
            int dx = drawn.get(i).getX() - origin.getX();
            int dy = drawn.get(i).getY() - origin.getY();
            int dz = drawn.get(i).getZ() - origin.getZ();
            int[] expected = positions.get(i);
            if (dx != expected[0] || dy != expected[1] || dz != expected[2]) return false;
        }
        return true;
    }

    private void spawnFailureParticles(Player player, Location loc) {
        player.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION,
                loc.clone().add(0.5, 1, 0.5), 20, 0.3, 0.3, 0.3, 0.02);
    }

    private void activateRune(Player player, Location origin, String runeName, String command) {
        player.getWorld().spawnParticle(Particle.FLAME, origin.clone().add(0.5, 1, 0.5),
                50, 0.5, 0.5, 0.5, 0.05);
        player.sendMessage(ChatColor.GOLD + "Runa '" + runeName + "' ativada!");
        if (command != null && !command.isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
    }

    // --- Comando associado ---
    public void setRuneCommand(String runeName, String command) {
        RuneData data = savedRunes.get(runeName);
        if (data != null) {
            data.command = command;
            saveRune(runeName, data);
        }
    }

    // --- JSON ---
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
        int requiredBlocks;
        String command;
        List<Block> blocks = new ArrayList<>();

        PlayerCreationData(String runeName, int requiredBlocks, String command) {
            this.runeName = runeName;
            this.requiredBlocks = requiredBlocks;
            this.command = command;
        }
    }

    private static class PlayerDrawingData {
        List<Block> blocks = new ArrayList<>();
    }
}
