package com.articreep.fireworkraid.raid;

import com.articreep.fireworkraid.combo.Combo;
import com.articreep.fireworkraid.queue.FireworkQueue;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Raider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.raid.RaidSpawnWaveEvent;
import org.bukkit.event.raid.RaidStopEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.UUID;

public class Raids implements Listener {
    @EventHandler
    public void onRaidStart(RaidTriggerEvent event) {
        // todo make this work for multiple people
        Player player = event.getPlayer();
        FireworkQueue.toggleQueue(player);
        player.sendMessage("Good luck!");
        RaidScoreboard.assignScoreboard(player);
        Combo.getComboCounter(player).resetScore();
        RaidScoreboard.updateScore(player, 0);
    }

    @EventHandler
    public void onNewWave(RaidSpawnWaveEvent event) {
        // make all raiders glow
        List<Raider> raiders = event.getRaiders();
        for (Raider raider : raiders) {
            raider.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
        }

        for (UUID uuid : event.getRaid().getHeroes()) {
            FireworkQueue.addRandomItems(Bukkit.getPlayer(uuid), 4);
        }
    }

    @EventHandler
    public void onRaidEnd(RaidStopEvent event) {
        // todo for some reason this doesn't work when the raid ends in vanilla defeat
        for (UUID uuid : event.getRaid().getHeroes()) {
            Player player = Bukkit.getPlayer(uuid);
            player.sendMessage("Raid complete");
            player.sendMessage("Your final score was " + Combo.getComboCounter(player).getScore());
            FireworkQueue.toggleQueue(player);
        }
    }


}
