package com.elmakers.mine.bukkit.arenas.dueling;

import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ArenasPlugin extends JavaPlugin {
    private ArenaController controller;
    private MagicAPI magicAPI = null;

    @Override
    public void onEnable() {
        Plugin magicPlugin = Bukkit.getPluginManager().getPlugin("Magic");
        if (magicPlugin == null || !(magicPlugin instanceof MagicAPI)) {
            getLogger().warning("Magic plugin not found, MagicArenas will not function.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        } else {
            getLogger().info("Integrating with Magic");
            this.magicAPI = (MagicAPI)magicPlugin;
        }

        controller = new ArenaController(this, magicAPI.getController());
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
        controller.save(false);
    }

    public void load() {
        controller.load();
    }

    @Override
    public void onDisable() {
        save();
    }
}