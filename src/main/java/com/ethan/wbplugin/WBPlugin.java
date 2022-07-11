package com.ethan.wbplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class WBPlugin extends JavaPlugin implements Listener {

    // boolean checks if players can welcome someone
    // int is the number of players eligible to be welcomed
    // random is used for loot tables
    boolean welcoming;
    int welcome = 0;
    Random random = new Random();
    List<UUID> recently_joined = new ArrayList<>();
    HashMap<UUID, Integer> welcome_counter = new HashMap<>();

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        if (!getConfig().getBoolean("enable-plugin")){
            System.out.println("Plugin not enabled in config. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
        }
        // Plugin startup logic

    }

    // Activates welcome wagon when player joins and notifies players.
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        // If the same player leaves and rejoins the server before the welcome cool down expires, it will not
        // start  a new welcoming window.
        UUID player = e.getPlayer().getUniqueId();
        if (!recently_joined.contains(player)) {
            recently_joined.add(player);
            welcome_counter.put(player, 1);
            Bukkit.getScheduler().runTaskLater(this, () -> recently_joined.remove(player),
                    20 * getConfig().getLong("player-cool-down"));
            welcome++;
            welcoming = true;
            // This task deactivates the welcome wagon after 10 seconds
            BukkitTask end_welcome = Bukkit.getScheduler().runTaskLater(this, () -> {
                welcoming = false;
                welcome = 0;
                welcome_counter.clear();
                for (Player p : Bukkit.getOnlinePlayers()){
                    p.sendMessage("Welcome period has ended");
                }


            }, 20 * getConfig().getLong("welcome-period"));
            // If another player joins during the timer, the previous end_welcome is canceled
            // Because a new end_welcome every time a player joins the server, the welcoming window
            // will be closed 10 seconds after the latest player joins the server
            if (welcome > 1){
                Bukkit.getScheduler().cancelTask(end_welcome.getTaskId() - 2);
            }

        } else{
            e.getPlayer().sendMessage("Not eligible for welcome wagon.");
        }
    }


    // Monitors chat for players welcoming the joiner
    // Runs loot table for kind players
    @EventHandler
    public void onPlayerMessage(AsyncPlayerChatEvent e) {
        String message = e.getMessage().toLowerCase();
        if ((message.contains("wb") || message.contains("welcome")) && welcoming) {
            UUID player = e.getPlayer().getUniqueId();
            Player name = e.getPlayer();
            if (!welcome_counter.containsKey(player)){
                welcome_counter.put(player, 1);
                give_loot(name);
            } else if (welcome_counter.get(player) < welcome){
                welcome_counter.replace(player, welcome_counter.get(player) + 1);
                give_loot(name);
            } else {
                e.getPlayer().sendMessage("You have already welcomed everyone!");
            }
        }
        }

        void give_loot(Player name){
            name.sendMessage("You just sent a kind message. Here is a reward!");
            Set<String> keys = getConfig().getConfigurationSection("loot").getKeys(false);
            HashMap<String, Long> loot = new HashMap<String, Long>();
            HashMap<String, Float> chances = new HashMap<String, Float>();
            Long total_chance = 0L;
            Float counter = 0F;

            for (String key : keys){
                loot.put(key, getConfig().getLong("loot." + key));
                total_chance += getConfig().getLong("loot." + key);
            }

            for (Map.Entry<String, Long> entry : loot.entrySet()){
                String key = entry.getKey();
                Long value = entry.getValue();
                Float chance_of_item = Float.valueOf(value) / Float.valueOf(total_chance);
                chances.put(key, chance_of_item);
            }

            Float rand = random.nextFloat();
            for (Map.Entry<String, Float> entry : chances.entrySet()){
                String key = entry.getKey();
                Float value = entry.getValue();
                if (1 - value - counter <= rand){
                    ItemStack item = new ItemStack(Material.getMaterial(key));
                    name.getInventory().addItem(item);
                    break;
                } else{
                    counter += value;
                }
            }
        }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
