package com.elmakers.mine.bukkit.arenas.dueling;

import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ArenaController {
    private final Map<String, Arena> arenas = new HashMap<String, Arena>();
    private final Plugin plugin;

    public ArenaController(Plugin plugin) {
        this.plugin = plugin;
    }

    public Arena addArena(String arenaName, Location location, int min, int max, ArenaType type) {
        Arena arena = new Arena(arenaName, this, location, min, max, type);
        arenas.put(arenaName, arena);
        return arena;
    }

    public void save() {
        File arenaSaveFile = getDataFile("arenas");
        YamlConfiguration arenaSaves = new YamlConfiguration();
        save(arenaSaves);
        try {
            arenaSaves.save(arenaSaveFile);
        } catch (Exception ex) {
            plugin.getLogger().warning("Error saving arena data to " + arenaSaveFile.getName());
            ex.printStackTrace();
        }
    }

    public void load() {
        ConfigurationSection arenaSaves = loadDataFile("arenas");
        load(arenaSaves);
    }

    private void save(ConfigurationSection configuration) {
        Collection<String> oldKeys = configuration.getKeys(false);
        for (String oldKey : oldKeys) {
            configuration.set(oldKey, null);
        }
        for (Map.Entry<String, Arena> entry : arenas.entrySet()) {
            ConfigurationSection arenaConfig = configuration.createSection(entry.getKey());
            entry.getValue().save(arenaConfig);
        }
    }

    private void load(ConfigurationSection configuration) {
        Collection<String> arenaKeys = configuration.getKeys(false);

        arenas.clear();
        for (String arenaKey : arenaKeys) {
            Arena arena = new Arena(arenaKey, this);
            arena.load(configuration.getConfigurationSection(arenaKey));
            arenas.put(arenaKey, arena);
        }

        plugin.getLogger().info("Loaded " + arenas.size() + " arenas");
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public boolean playerLeft(Player player) {
        for (Arena arena : arenas.values()) {
            if (arena.has(player)) {
                player.teleport(arena.getLoseLocation());
                arena.remove(player);
                arena.check();
                return true;
            }
        }

        return false;
    }

    public Arena getArena(String arenaName) {
        return arenas.get(arenaName);
    }

    public Arena getArena(Player player) {
        if (player.hasMetadata("arena")) {
            for (MetadataValue value : player.getMetadata("arena")) {
                if (value.getOwningPlugin().equals(getPlugin())) {
                    Object arena = value.value();
                    if (arena instanceof Arena) {
                        return (Arena)value.value();
                    }
                }
            }
        }

        return null;
    }

    protected ConfigurationSection loadDataFile(String fileName) {
        File dataFile = getDataFile(fileName);
        if (!dataFile.exists()) {
            return null;
        }
        Configuration configuration = YamlConfiguration.loadConfiguration(dataFile);
        return configuration;
    }

    protected File getDataFile(String fileName) {
        return new File(plugin.getDataFolder(), fileName + ".yml");
    }

    public void remove(String arenaName) {
        Arena arena = arenas.get(arenaName);
        if (arena != null) {
            arena.cancel();
            arenas.remove(arenaName);
        }
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public void leave(Player player) {
        Arena arena = getArena(player);
        if (arena != null) {
            arena.remove(player);
            player.sendMessage("You have left " + arena.getName());
            player.teleport(arena.getExit());
        }
    }
}
