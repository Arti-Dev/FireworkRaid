package com.articreep.fireworkraid.combo;

import com.articreep.fireworkraid.Utils;
import com.articreep.fireworkraid.queue.CustomQueueItems;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;

public class Combo implements Listener {
    private static final HashMap<Player, ComboCounter> comboMap = new HashMap<>();

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        getComboCounter(player).incrementCombo();
    }

    @EventHandler
    public void onDamageFirework(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile proj)) return;
        if (!Utils.isFirework(proj)) return;
        if (!(proj.getShooter() instanceof Player player)) return;
        getComboCounter(player).incrementCombo();
    }

    @EventHandler
    public void onArrowCollide(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        if (!Utils.isArrow(proj)) return;
        if (!(proj.getShooter() instanceof Player player)) return;
        if (proj.getPersistentDataContainer().has(CustomQueueItems.damageKey)) {
            PersistentDataContainer container = proj.getPersistentDataContainer();
            double damage = container.getOrDefault(CustomQueueItems.damageKey, PersistentDataType.DOUBLE, 7d);
            // create radius explosion
            int radius = 5;
            List<Entity> entities = proj.getNearbyEntities(radius, radius, radius);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity && !entity.equals(player)) ((LivingEntity) entity).damage(damage);
                getComboCounter(player).incrementCombo();
            }
            proj.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, proj.getLocation(), 1);
        }

    }


    @EventHandler
    public void onDC(PlayerQuitEvent event) {
        comboMap.remove(event.getPlayer());
    }

    public static void registerCombo(Player player) {
        if (hasRegisteredCombo(player)) return;
        comboMap.put(player, new ComboCounter(player, 60));
    }

    public static boolean hasRegisteredCombo(Player player) {
        return comboMap.containsKey(player);
    }

    public static ComboCounter getComboCounter(Player player) {
        if (!hasRegisteredCombo(player)) registerCombo(player);
        return comboMap.get(player);
    }


}
