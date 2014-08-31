package com.elmakers.mine.bukkit.arenas.dueling;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ArenasPlugin extends JavaPlugin {
    private ArenaController controller;

    @Override
    public void onEnable() {
        controller = new ArenaController(this);
        ArenaCommandExecutor arenaCommands = new ArenaCommandExecutor(controller);
        getCommand("arena").setExecutor(arenaCommands);
        getCommand("arena").setTabCompleter(arenaCommands);

        ArenaListener listener = new ArenaListener(controller);
        getServer().getPluginManager().registerEvents(listener, this);

        // Make sure not to load configs until all worlds are loaded
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                load();
            }
        }, 2);
    }

    public void save() {
        controller.save();
    }

    public void load() {
        controller.load();
    }

    @Override
    public void onDisable() {
        save();
    }
}