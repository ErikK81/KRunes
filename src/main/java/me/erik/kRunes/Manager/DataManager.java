package me.erik.kRunes.Manager;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {

    private final File runesFolder;
    private final Map<String, RuneData> savedRunes = new HashMap<>();
    private final Map<UUID, List<PlayerCreationData>> playerCreations = new HashMap<>();

    public DataManager(File pluginFolder) {
        this.runesFolder = new File(pluginFolder, "Runes");
        if (!runesFolder.exists()) runesFolder.mkdirs();
        loadRunes();
    }

    // --- Salvar runa ---
    public void saveRune(String name, RuneData data) {
        savedRunes.put(name, data);
        File file = new File(runesFolder, name + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();

        List<String> posStrings = new ArrayList<>();
        for (int[] pos : data.positions) {
            posStrings.add(pos[0] + "," + pos[1] + "," + pos[2]);
        }

        yaml.set("positions", posStrings);
        yaml.set("command", data.command);

        try {
            yaml.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Deletar runa ---
    public boolean deleteRune(String name) {
        RuneData removed = savedRunes.remove(name);

        File file = new File(runesFolder, name + ".yml");
        boolean deleted = false;
        if (file.exists()) deleted = file.delete();

        return removed != null && deleted;
    }

    // --- Editar comando de uma runa ---
    public void setRuneCommand(String runeName, String newCommand) {
        RuneData data = savedRunes.get(runeName);
        if (data == null) return;

        data.command = newCommand;
        saveRune(runeName, data); // Salva alteração no arquivo
    }

    // --- Verifica existência de runa ---
    public boolean runeExists(String runeName) {
        return savedRunes.containsKey(runeName);
    }

    // --- Remover criação atual ---
    public void removeCurrentCreation(Player player, PlayerCreationData creation) {
        List<PlayerCreationData> creations = playerCreations.get(player.getUniqueId());
        if (creations != null) creations.remove(creation);
    }

    // --- Salvar runa a partir de criação ---
    public void saveRuneFromCreation(Player player, PlayerCreationData creation) {
        if (creation.blocks.isEmpty()) return;

        Block origin = creation.blocks.get(0);
        List<int[]> relativePositions = new ArrayList<>();
        for (Block b : creation.blocks) {
            relativePositions.add(new int[]{
                    b.getX() - origin.getX(),
                    b.getY() - origin.getY(),
                    b.getZ() - origin.getZ()
            });
        }

        RuneData runeData = new RuneData(relativePositions, creation.command);
        saveRune(creation.runeName, runeData);

        player.sendMessage(ChatColor.GREEN + "Rune '" + creation.runeName + "' created!");
        player.sendMessage(ChatColor.GRAY + "Command: " + ChatColor.WHITE + creation.command);
    }

    // --- Carregar runas do disco ---
    private void loadRunes() {
        if (!runesFolder.exists()) return;

        File[] files = runesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            List<String> posStrings = yaml.getStringList("positions");
            List<int[]> positions = new ArrayList<>();

            for (String s : posStrings) {
                String[] split = s.split(",");
                if (split.length != 3) continue;
                positions.add(new int[]{
                        Integer.parseInt(split[0]),
                        Integer.parseInt(split[1]),
                        Integer.parseInt(split[2])
                });
            }

            String command = yaml.getString("command", "");
            String runeName = file.getName().replace(".yml", "");
            savedRunes.put(runeName, new RuneData(positions, command));
        }
    }

    // --- Getters ---
    public Map<String, RuneData> getSavedRunes() {
        return savedRunes;
    }

    public Map<UUID, List<PlayerCreationData>> getPlayerCreations() {
        return playerCreations;
    }

    // --- Classes internas ---
    public static class RuneData {
        public List<int[]> positions;
        public String command;

        public RuneData(List<int[]> positions, String command) {
            this.positions = positions;
            this.command = command;
        }
    }

    public static class PlayerCreationData {
        public String runeName;
        public int requiredBlocks;
        public String command;
        public List<Block> blocks = new ArrayList<>();

        public PlayerCreationData(String runeName, int requiredBlocks, String command) {
            this.runeName = runeName;
            this.requiredBlocks = requiredBlocks;
            this.command = command;
        }
    }
}
