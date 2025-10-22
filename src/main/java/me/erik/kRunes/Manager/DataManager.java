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
        yaml.set("commands", data.commands);

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
    public void setRuneCommands(String runeName, List<String> newCommands) {
        RuneData data = savedRunes.get(runeName);
        if (data == null) return;

        data.commands = newCommands;
        saveRune(runeName, data);
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

        RuneData runeData = new RuneData(relativePositions, creation.commands);
        saveRune(creation.name, runeData);

        player.sendMessage(ChatColor.GREEN + "Rune '" + creation.name + "' created!");
        player.sendMessage(ChatColor.GRAY + "Command: " + ChatColor.WHITE + creation.commands);
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

            List<String> commandList = yaml.getStringList("commands");

            // compatibilidade com versões antigas (que tinham apenas "command")
            if (commandList.isEmpty() && yaml.contains("command")) {
                commandList = Collections.singletonList(yaml.getString("command", ""));
            }

            String runeName = file.getName().replace(".yml", "");
            savedRunes.put(runeName, new RuneData(positions, commandList));;
        }
    }

    // --- Listar runas ---
    public void listRunes(Player player) {
        if (savedRunes.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Não há runas salvas no momento.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "===== Runas Salvas =====");

        for (Map.Entry<String, RuneData> entry : savedRunes.entrySet()) {
            RuneData data = entry.getValue();

            player.sendMessage(ChatColor.GRAY + " | Comandos: " + ChatColor.GREEN +
                    (String.join(", ", data.commands.isEmpty() ? Collections.singletonList("Nenhum") : data.commands)));
            }

        player.sendMessage(ChatColor.GOLD + "======================");
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
        public List<String> commands;

        public RuneData(List<int[]> positions, List<String> commands) {
            this.positions = positions;
            this.commands = commands;
        }
    }

    public static class PlayerCreationData {
        public final String name;
        public final int requiredBlocks;
        public final List<Block> blocks = new ArrayList<>();
        public final List<String> commands;

        public PlayerCreationData(String name, int requiredBlocks, List<String> commands) {
            this.name = name;
            this.requiredBlocks = requiredBlocks;
            this.commands = commands != null ? commands : new ArrayList<>();
        }
    }
}
