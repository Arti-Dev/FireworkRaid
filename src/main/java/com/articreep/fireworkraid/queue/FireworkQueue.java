package com.articreep.fireworkraid.queue;

import com.articreep.fireworkraid.FireworkRaid;
import com.articreep.fireworkraid.Utils;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class FireworkQueue implements Listener, CommandExecutor {
    // todo clarify when to use ItemQueue.pop() and not removeFromQueue
    // todo this code is too long smh
    protected static HashMap<UUID, ItemQueue> enabledPlayers = new HashMap<>();
    static HashMap<UUID, BukkitTask> activeIndicators = new HashMap<>();

    @EventHandler
    public void onFireworkExplode(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile proj)) return;
        if (!Utils.isFirework(proj)) return;
        if (!(proj.getShooter() instanceof Player player)) return;

        // disable players being able to hit themselves with their own fireworks
        if (event.getEntity().equals(player)) event.setCancelled(true);

        if (proj.getPersistentDataContainer().has(CustomQueueItems.damageKey)) {
            if (Utils.isFirework(proj)) {
                PersistentDataContainer container = proj.getPersistentDataContainer();
                double damage = container.get(CustomQueueItems.damageKey, PersistentDataType.DOUBLE);
                event.setDamage(damage);

            }
        }
    }

    HashSet<Player> recentlyFired = new HashSet<>();

    @EventHandler
    public void onFireworkFire(ProjectileLaunchEvent event) {
        Projectile proj = event.getEntity();
        if (!Utils.isArrow(proj) && !Utils.isFirework(proj)) return;
        if (!(proj.getShooter() instanceof Player player)) return;
        if (enabledPlayers.containsKey(player.getUniqueId())) {
            // Apply properties of the firework fired
            ItemQueue queue = enabledPlayers.get(player.getUniqueId());
            ItemStack item = queue.getActiveItem();
            if (!item.hasItemMeta()) return;
            PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();

            int range = container.getOrDefault(CustomQueueItems.rangeKey, PersistentDataType.INTEGER, 20);
            double damage = container.getOrDefault(CustomQueueItems.damageKey, PersistentDataType.DOUBLE, 7d);

            if (Utils.isFirework(proj)) {
                if (recentlyFired.contains(player)) return;
                Firework firework = (Firework) proj;
                firework.setMaxLife(range);
                firework.getPersistentDataContainer().set(CustomQueueItems.damageKey, PersistentDataType.DOUBLE, damage);
            } else if (Utils.isArrow(proj)) {
                Arrow arrow = (Arrow) proj;
                arrow.getPersistentDataContainer().set(CustomQueueItems.damageKey, PersistentDataType.DOUBLE, damage);
            }

            consumeItem(player);

            // Prevent instances where multishot would consume three fireworks instead of one
            recentlyFired.add(player);
            Bukkit.getScheduler().runTask(FireworkRaid.getInstance(), () -> recentlyFired.remove(player));
        }
    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        if (!enabledPlayers.containsKey(event.getPlayer().getUniqueId())) return;
        if (event.getNewSlot() != 0) {
            event.setCancelled(true);
            event.getPlayer().getInventory().setHeldItemSlot(0);
        }
    }

    @EventHandler
    public void onDC(PlayerQuitEvent event) {
        enabledPlayers.remove(event.getPlayer().getUniqueId());
        activeIndicators.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        consumeItem(event.getPlayer());
    }

    @EventHandler
    public void onPlayerPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            UUID uuid = player.getUniqueId();
            if (enabledPlayers.containsKey(uuid)) {
                event.setCancelled(true);
                event.getItem().remove();
//                addToQueue(player, event.getItem().getItemStack());
                player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);
            }
        }
        // todo extend this to arrow entity pickups since they clutter the inventory
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        // if the player's inventory isn't empty and they're not enabled
        if (!player.getInventory().isEmpty() && !enabledPlayers.containsKey(uuid)) {
            player.sendMessage(ChatColor.RED + "Your inventory needs to be empty in order to start a raid!");
        } else if (!enabledPlayers.containsKey(uuid)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BAD_OMEN, Integer.MAX_VALUE, 3));
            player.sendMessage(ChatColor.GREEN + "Gave you Bad Omen III!");
        } else {
            player.sendMessage("Queue disabled");
            enabledPlayers.remove(uuid);
            player.getInventory().clear();
        }
        return true;
    }

    /** Updates the player's inventory to match the queue state **/
    private static void updateInventory(Player player) {
        UUID uuid = player.getUniqueId();
        if (!enabledPlayers.containsKey(uuid)) return;

        PlayerInventory inventory = player.getInventory();
        ItemQueue queue = enabledPlayers.get(uuid);
        ItemStack activeItem = queue.getActiveItem();
        ItemStack holdItem = queue.getHoldItem();
        ArrayList<ItemStack> visibleQueue = queue.getVisibleQueue();

        // Process current item and necessary weapons
        if (!activeItem.hasItemMeta()) inventory.setItem(0, activeItem);
        else {
            if (activeItem.getType() == Material.FIREWORK_ROCKET) {
                ItemStack crossbow = new ItemStack(Material.CROSSBOW);
                PersistentDataContainer container = activeItem.getItemMeta().getPersistentDataContainer();
                if (container.has(CustomQueueItems.quickChargeKey)) {
                    crossbow.addUnsafeEnchantment(Enchantment.QUICK_CHARGE,
                            container.get(CustomQueueItems.quickChargeKey, PersistentDataType.INTEGER));
                }
                if (container.has(CustomQueueItems.multiShotKey)) {
                    crossbow.addUnsafeEnchantment(Enchantment.MULTISHOT, 1);
                }
                inventory.setItem(0, crossbow);
                inventory.setItemInOffHand(activeItem);
            } else if (activeItem.getType() == Material.ARROW) {
                ItemStack bow = new ItemStack(Material.BOW);
                bow.addEnchantment(Enchantment.ARROW_FIRE, 1);
                inventory.setItem(0, bow);
                inventory.setItemInOffHand(activeItem);
            } else {
                inventory.setItem(0, activeItem);
                inventory.setItemInOffHand(null);
            }
        }

        for (int i = 1; i < 8; i++) {
            if (i <= visibleQueue.size()) {
                inventory.setItem(i, visibleQueue.get(i-1));
            } else {
                inventory.setItem(i, null);
            }
        }
        inventory.setItem(8, holdItem);

        onItemChange(player, activeItem);
    }

    public static void onItemChange(Player player, ItemStack activeItem) {
        if (activeItem.getType() == Material.FIREWORK_ROCKET) {
            if (!activeIndicators.containsKey(player.getUniqueId())) {
                activeIndicators.put(player.getUniqueId(), drawIndicator(player));
            }
        } else if (activeIndicators.containsKey(player.getUniqueId())) {
            activeIndicators.get(player.getUniqueId()).cancel();
            activeIndicators.remove(player.getUniqueId());
        }
    }

    private static BukkitTask drawIndicator(Player player) {
        return new BukkitRunnable() {
            BlockDisplay display;
            @Override
            public void run() {
                ItemQueue queue = enabledPlayers.get(player.getUniqueId());
                if (queue == null) return;
                ItemStack item = queue.getActiveItem();
                PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
                int range = container.getOrDefault(CustomQueueItems.rangeKey, PersistentDataType.INTEGER, 20);
                double actualRange = 1.58 * range + 1.9;
                Vector v = player.getLocation().getDirection().multiply(actualRange);
                Location indicatorLoc = player.getLocation().add(v).add(0, 1.8, 0);
                player.getWorld().spawnParticle(Particle.HEART, indicatorLoc, 1);
            }
        }.runTaskTimer(FireworkRaid.getInstance(), 0, 1);
    }

    private static void addToQueue(Player player, ItemStack item) {
        UUID uuid = player.getUniqueId();
        if (enabledPlayers.containsKey(uuid)) {
            ItemQueue queue = enabledPlayers.get(uuid);

            // save last resort durability
            if (queue.isLastResortActive()) {
                ItemStack lastResort = player.getInventory().getItem(0);
                queue.setLastResortItem(lastResort);
            }

            enabledPlayers.get(player.getUniqueId()).add(item);
            updateInventory(player);
        }
    }

    private void consumeItem(Player player) {
        UUID uuid = player.getUniqueId();
        if (enabledPlayers.containsKey(uuid)) {
            ItemQueue queue = enabledPlayers.get(uuid);
            if (queue.consumeActiveItem()) {
                String name = queue.getActiveItem().getItemMeta().getDisplayName();
                player.sendTitle(name, "", 0, 1, 19);
                updateInventory(player);
            } else {
                if (queue.getActiveItem().getAmount() < 3) {
                    player.sendTitle("",
                            ChatColor.GRAY + "" + queue.getActiveItem().getAmount() + " left!",
                            0, 1, 19);
                }
            }
        }
    }

    private static void starterItems(Player player) {
        ItemQueue queue = enabledPlayers.get(player.getUniqueId());
        queue.add(CustomQueueItems.longRangeFirework(2));
        queue.add(CustomQueueItems.explosiveArrow(2));
        queue.add(CustomQueueItems.shortRangeFirework(6));
        queue.add(CustomQueueItems.mediumRangeMultiShot(4));
        PlayerInventory inventory = player.getInventory();
        inventory.setHelmet(new ItemStack(Material.TURTLE_HELMET));
        inventory.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        inventory.setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
        inventory.setBoots(new ItemStack(Material.IRON_BOOTS));
        updateInventory(player);
    }

    public static void addRandomItems(Player player, int amount) {
        Random random = new Random();
        for (int i = 0; i < amount; i++) {
            ItemStack item;
            item = switch (random.nextInt(4)) {
                case 0 -> CustomQueueItems.longRangeFirework(2);
                case 1 -> CustomQueueItems.explosiveArrow(1);
                case 2 -> CustomQueueItems.shortRangeFirework(6);
                case 3 -> CustomQueueItems.mediumRangeMultiShot(4);
                default -> CustomQueueItems.shortRangeFirework(1);
            };
            addToQueue(player, item);
            player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1, 1);
        }
        player.sendMessage(ChatColor.DARK_GRAY + "+" + amount + " items!");
    }

    public static boolean isFireworking(Player player) {
        // awesome name
        return enabledPlayers.containsKey(player.getUniqueId());
    }

    public static void toggleQueue(Player player) {
        UUID uuid = player.getUniqueId();
        if (enabledPlayers.containsKey(uuid)) {
            player.sendMessage("Queue disabled");
            enabledPlayers.remove(uuid);
            player.getInventory().clear();
        } else {
            player.sendMessage("Queue enabled");
            ItemQueue queue = new ItemQueue();
            enabledPlayers.put(uuid, queue);

            starterItems(player);
            updateInventory(player);
        }
    }
}
