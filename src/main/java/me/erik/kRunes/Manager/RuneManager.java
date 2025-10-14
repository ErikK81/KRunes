package me.erik.kRunes.Manager;

import me.erik.kRunes.KRunes;
import me.erik.kRunes.PlaceHolders;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

public class RuneManager {

    private final DataManager dataManager;
    private final EffectsManager effectsManager;

    private final Map<UUID, PlayerDrawingData> playerDrawings = new HashMap<>();

    public RuneManager(KRunes plugin, DataManager dataManager, EffectsManager effectsManager) {
        this.dataManager = dataManager;
        this.effectsManager = effectsManager;
    }

    // --- Criação de runas ---
    public void startRuneCreation(Player player, String runeName, int blockCount, String command) {
        DataManager.PlayerCreationData creation = new DataManager.PlayerCreationData(runeName, blockCount, command);
        dataManager.getPlayerCreations().computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(creation);

        player.sendMessage(PlaceHolders.replace("&aModo criação iniciado para a runa: &e" + runeName, player, null, null));
        player.sendMessage(PlaceHolders.replace("&bBlocos necessários: &e" + blockCount, player, null, null));
        player.sendMessage(PlaceHolders.replace("&7Comando: &f" + command, player, null, null));
    }

    public boolean addCreationBlock(Player player, Block block) {
        DataManager.PlayerCreationData current = getCurrentCreation(player);
        if (current == null) return false;

        if (current.blocks.contains(block)) {
            player.sendMessage(ChatColor.GRAY + "Este bloco já foi marcado.");
            return false;
        }

        // Adiciona o bloco à runa
        current.blocks.add(block);
        player.sendMessage(PlaceHolders.replace("&eBloco adicionado (" + current.blocks.size() + "/" + current.requiredBlocks + ").", player, null, null));
        effectsManager.playSound(player, "draw", block.getLocation());

        // Se houver bloco anterior, adiciona linha persistente
        if (current.blocks.size() > 1) {
            Block lastBlock = current.blocks.get(current.blocks.size() - 2);
            effectsManager.addLine(player, lastBlock.getLocation(), block.getLocation());
        }

        // Finaliza criação
        if (current.blocks.size() >= current.requiredBlocks) {
            dataManager.saveRuneFromCreation(player, current);
            dataManager.removeCurrentCreation(player, current);
            return true;
        }
        return false;
    }

    private DataManager.PlayerCreationData getCurrentCreation(Player player) {
        List<DataManager.PlayerCreationData> creations = dataManager.getPlayerCreations().get(player.getUniqueId());
        if (creations == null || creations.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Você não está criando nenhuma runa.");
            return null;
        }
        return creations.getLast();
    }

    // --- Giz rúnico ---
    public void addChalkBlock(Player player, Block block) {
        PlayerDrawingData drawing = playerDrawings.computeIfAbsent(player.getUniqueId(), k -> new PlayerDrawingData());
        drawing.blocks.add(block);
        player.sendMessage(PlaceHolders.replace("&ePonto adicionado (" + drawing.blocks.size() + ").", player, null, null));
        effectsManager.playSound(player, "draw", block.getLocation());

        // Desenha linha persistente
        if (drawing.blocks.size() > 1) {
            Block lastBlock = drawing.blocks.get(drawing.blocks.size() - 2);
            effectsManager.addLine(player, lastBlock.getLocation(), block.getLocation());
        }
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
        effectsManager.clearLines(player); // remove linhas persistentes após ativação
    }

    private void checkRuneActivation(Player player, List<Block> drawnBlocks) {
        List<String> matchedRunes = new ArrayList<>();

        for (Map.Entry<String, DataManager.RuneData> entry : dataManager.getSavedRunes().entrySet()) {
            String runeName = entry.getKey();
            DataManager.RuneData data = entry.getValue();

            if (drawnBlocks.size() != data.positions.size()) continue;
            if (matchesRune(drawnBlocks, data.positions)) matchedRunes.add(runeName);
        }

        if (matchedRunes.isEmpty()) {
            effectsManager.playSound(player, "fail", drawnBlocks.getFirst().getLocation());
            player.sendMessage(PlaceHolders.replace("&cRuna incorreta! Pontos resetados.", player, drawnBlocks.getFirst().getLocation(), drawnBlocks.getLast().getLocation()));
        } else {
            for (String runeName : matchedRunes) {
                DataManager.RuneData data = dataManager.getSavedRunes().get(runeName);
                effectsManager.playSound(player, "activate", drawnBlocks.getFirst().getLocation());

                Location start = drawnBlocks.getFirst().getLocation();
                Location end = drawnBlocks.getLast().getLocation();

                player.sendMessage(PlaceHolders.replace("&6Runa '" + runeName + "' ativada!", player, start, end));

                if (data.command != null && !data.command.isEmpty()) {
                    String finalCommand = PlaceHolders.replace(data.command, player, start, end);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                }
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

    // --- Comando associado ---
    public void setRuneCommand(String runeName, String command) {
        DataManager.RuneData data = dataManager.getSavedRunes().get(runeName);
        if (data != null) {
            data.command = command;
            dataManager.saveRune(runeName, data);
        }
    }

    // --- Classes internas ---
    public static class PlayerDrawingData {
        List<Block> blocks = new ArrayList<>();
    }
}
