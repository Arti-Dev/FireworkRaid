package com.articreep.fireworkraid.enemies;

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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Raids implements Listener {
    @EventHandler
    public void onRaidStart(RaidTriggerEvent event) {
        // todo make this work for multiple people
        Player player = event.getPlayer();
        FireworkQueue.toggleQueue(player);
        player.sendMessage("Good luck!");
    }

    @EventHandler
    public void onNewWave(RaidSpawnWaveEvent event) {
        // make all raiders glow
        List<Raider> raiders = event.getRaiders();
        for (Raider raider : raiders) {
            raider.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1));
        }

        Bukkit.broadcastMessage("Heroes: " + event.getRaid().getHeroes());
        // todo temporary
        for (UUID uuid : event.getRaid().getHeroes()) {
            FireworkQueue.addRandomItems(Bukkit.getPlayer(uuid), 4);
        }
    }

    @EventHandler
    public void onRaidEnd(RaidStopEvent event) {
        // todo temporary
        for (UUID uuid : event.getRaid().getHeroes()) {
            Player player = Bukkit.getPlayer(uuid);
            FireworkQueue.toggleQueue(player);
            player.sendMessage("Raid complete");
        }
    }


}
