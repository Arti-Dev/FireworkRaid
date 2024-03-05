package com.articreep.fireworkraid;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;

public class Utils {

    public static boolean isFirework(Entity entity) {
        return entity instanceof Firework;
    }

    public static boolean isArrow(Entity entity) {
        return entity instanceof Arrow;
    }
}
