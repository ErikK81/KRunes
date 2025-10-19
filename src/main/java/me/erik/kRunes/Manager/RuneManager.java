package me.erik.kRunes.Manager;

import me.erik.kRunes.KRunes;
import me.erik.kRunes.PlaceHolders;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

public class RuneManager {

    private final DataManager dataManager;
    private final EffectsManager effects;
    private final MessageManager messages;
    private final Map<UUID, PlayerDrawingData> drawings = new HashMap<>();

    public RuneManager(KRunes plugin, DataManager dataManager, EffectsManager effects) {
        this.dataManager = dataManager;
        this.effects = effects;
        this.messages = plugin.getMessageManager();
    }

    /* ==========================================================
     *                      RUNE CREATION
     * ========================================================== */
    public void startRuneCreation(Player player, String runeName, int blockCount, String command) {
        var creation = new DataManager.PlayerCreationData(runeName, blockCount, command);
        dataManager.getPlayerCreations()
                .computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())
                .add(creation);

        player.sendMessage(messages.get("general", "prefix") + messages.get("rune", "start"));
        player.sendMessage(PlaceHolders.replace("&bBlocks required: &e" + blockCount, player, null, null));
        player.sendMessage(PlaceHolders.replace("&7Command: &f" + (command.isEmpty() ? "None" : command), player, null, null));
    }

    public boolean addCreationBlock(Player player, Block block) {
        var creation = getCurrentCreation(player);
        if (creation == null) return false;

        if (creation.blocks.contains(block)) return false;

        creation.blocks.add(block);
        effects.playSound(player, "draw", block.getLocation());

        // Draw connecting line if there's a previous block
        if (creation.blocks.size() > 1) {
            Block previous = creation.blocks.get(creation.blocks.size() - 2);
            effects.addLine(player, previous.getLocation(), block.getLocation());
        }

        // If rune complete
        if (creation.blocks.size() >= creation.requiredBlocks) {
            dataManager.saveRuneFromCreation(player, creation);
            dataManager.removeCurrentCreation(player, creation);
            effects.clearLines(player); 
            return true;
        }

        return false;
    }

    private DataManager.PlayerCreationData getCurrentCreation(Player player) {
        var creations = dataManager.getPlayerCreations().get(player.getUniqueId());
        if (creations == null || creations.isEmpty()) {
            player.sendMessage(messages.get("general", "prefix") + messages.get("errors", "invalid_rune"));
            return null;
        }
        return creations.getLast();
    }

    /* ==========================================================
     *                      CHALK DRAWING
     * ========================================================== */
    public void addChalkBlock(Player player, Block block) {
        var drawing = drawings.computeIfAbsent(player.getUniqueId(), k -> new PlayerDrawingData());
        drawing.blocks.add(block);

        effects.playSound(player, "draw", block.getLocation());
        effects.spawnParticle("draw", block.getLocation());

        if (drawing.blocks.size() > 1) {
            Block previous = drawing.blocks.get(drawing.blocks.size() - 2);
            effects.addLine(player, previous.getLocation(), block.getLocation());
        }
    }

    /* ==========================================================
     *                      RUNE ACTIVATION
     * ========================================================== */
    public void tryActivateRune(Player player) {
        var drawing = drawings.get(player.getUniqueId());

        if (drawing == null || drawing.blocks.isEmpty()) {
            player.sendMessage(messages.get("general", "prefix") + messages.get("errors", "not_active"));
            return;
        }

        evaluateActivation(player, drawing.blocks);

        drawing.blocks.clear();
        effects.clearLines(player);
    }

    private void evaluateActivation(Player player, List<Block> drawnBlocks) {
        List<String> matchedRunes = findMatchingRunes(drawnBlocks);

        if (matchedRunes.isEmpty()) {
            Location loc = drawnBlocks.getFirst().getLocation();
            effects.playSound(player, "fail", loc);
            effects.spawnParticle("fail", loc);
            player.sendMessage(messages.get("general", "prefix") + messages.get("errors", "not_active"));
            return;
        }

        // Run all matching runes
        for (String runeName : matchedRunes) {
            DataManager.RuneData data = dataManager.getSavedRunes().get(runeName);
            Location start = drawnBlocks.getFirst().getLocation();
            Location end = drawnBlocks.getLast().getLocation();

            player.sendMessage(messages.get("general", "prefix") + messages.get("rune", "success"));

            if (data.command != null && !data.command.isEmpty()) {
                effects.spawnParticle("activate", end);
                effects.playSound(player, "activate", end);

                String parsedCommand = PlaceHolders.replace(data.command, player, start, end);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
            }
        }
    }

    private List<String> findMatchingRunes(List<Block> drawnBlocks) {
        List<String> matches = new ArrayList<>();

        for (var entry : dataManager.getSavedRunes().entrySet()) {
            String name = entry.getKey();
            DataManager.RuneData rune = entry.getValue();

            if (drawnBlocks.size() == rune.positions.size() && matchesRune(drawnBlocks, rune.positions)) {
                matches.add(name);
            }
        }
        return matches;
    }

    private boolean matchesRune(List<Block> drawn, List<int[]> positions) {
        Block origin = drawn.getFirst();

        for (int i = 0; i < drawn.size(); i++) {
            int dx = drawn.get(i).getX() - origin.getX();
            int dy = drawn.get(i).getY() - origin.getY();
            int dz = drawn.get(i).getZ() - origin.getZ();

            int[] expected = positions.get(i);
            if (dx != expected[0] || dy != expected[1] || dz != expected[2])
                return false;
        }
        return true;
    }

    /* ==========================================================
     *                      RUNE COMMANDS
     * ========================================================== */
    public void setRuneCommand(String runeName, String command) {
        var rune = dataManager.getSavedRunes().get(runeName);
        if (rune == null) return;

        rune.command = command;
        dataManager.saveRune(runeName, rune);
    }

    /* ==========================================================
     *                      INTERNAL CLASS
     * ========================================================== */
    public static class PlayerDrawingData {
        public final List<Block> blocks = new ArrayList<>();
    }
}
