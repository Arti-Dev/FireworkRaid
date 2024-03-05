package com.articreep.fireworkraid.raid;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class RaidScoreboard {
    private static Scoreboard board;
    private static Objective objective;
    public static void initScoreboard() {
        // todo could be null
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        board = manager.getNewScoreboard();
        objective = board.registerNewObjective("Score", Criteria.DUMMY, "Score");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public static void updateScore(Player player, int score) {
        objective.getScore(player.getName()).setScore(score);
    }

    public static void assignScoreboard(Player player) {
        player.setScoreboard(board);
    }

    public static void resetScoreboard() {
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }
    }
}
