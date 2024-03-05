package com.articreep.fireworkraid;

import com.articreep.fireworkraid.combo.Combo;
import com.articreep.fireworkraid.raid.RaidScoreboard;
import com.articreep.fireworkraid.raid.Raids;
import com.articreep.fireworkraid.queue.FireworkQueue;
import org.bukkit.plugin.java.JavaPlugin;

public final class FireworkRaid extends JavaPlugin {
    private static FireworkRaid instance;

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(new Combo(), this);
        FireworkQueue queue = new FireworkQueue();
        getServer().getPluginManager().registerEvents(queue, this);
        getCommand("fireworkraid").setExecutor(queue);
        getServer().getPluginManager().registerEvents(new Raids(), this);
        RaidScoreboard.initScoreboard();

    }

    @Override
    public void onDisable() {
        RaidScoreboard.resetScoreboard();
    }

    public static FireworkRaid getInstance() {
        return instance;
    }
}
