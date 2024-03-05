package com.articreep.fireworkraid;

import com.articreep.fireworkraid.combo.Combo;
import com.articreep.fireworkraid.enemies.Raids;
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

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static FireworkRaid getInstance() {
        return instance;
    }
}
