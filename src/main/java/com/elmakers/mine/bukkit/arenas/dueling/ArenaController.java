package com.elmakers.mine.bukkit.arenas.dueling;

import com.elmakers.mine.bukkit.api.magic.MageController;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ArenaController implements Runnable {
    private final Map<String, Arena> arenas = new HashMap<String, Arena>();
    private final Plugin plugin;
    private final MageController magic;
    private final Object saveLock = new Object();

    private BukkitTask task = null;
    private int tickInterval = 20;
    private String pathTemplate = "beginner";

    public ArenaController(Plugin plugin, MageController magic) {
        this.magic = magic;
        this.plugin = plugin;
    }

    public Arena addArena(String arenaName, Location location, int min, int max, ArenaType type) {
        Arena arena = new Arena(arenaName, this, location, min, max, type);
        arenas.put(arenaName.toLowerCase(), arena);
        return arena;
    }

    public void save() {
        save(true);
    }

    public void save(boolean asynchronous) {
        final File arenaSaveFile = getDataFile("arenas");
        final YamlConfiguration arenaSaves = new YamlConfiguration();
        save(arenaSaves);

        try {
            arenaSaves.save(arenaSaveFile);
        } catch (Exception ex) {
            plugin.getLogger().warning("Error saving arena configuration to " + arenaSaveFile.getName());
            ex.printStackTrace();
        }

        if (asynchronous) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    synchronized (saveLock) {
                        try {
                            arenaSaves.save(arenaSaveFile);
                        } catch (Exception ex) {
                            plugin.getLogger().warning("Error saving arena configuration to " + arenaSaveFile.getName());
                            ex.printStackTrace();
                        }
                    }
                }
            });
        } else {
            try {
                arenaSaves.save(arenaSaveFile);
            } catch (Exception ex) {
                plugin.getLogger().warning("Error saving arena configuration to " + arenaSaveFile.getName() + " synchronously");
                ex.printStackTrace();
            }
        }
    }

    public void saveData() {
        saveData(true);
    }

    public void saveData(boolean asynchronous) {
        final File arenaSaveFile = getDataFile("data");
        final YamlConfiguration arenaSaves = new YamlConfiguration();
        saveData(arenaSaves);

        try {
            arenaSaves.save(arenaSaveFile);
        } catch (Exception ex) {
            plugin.getLogger().warning("Error saving arena data to " + arenaSaveFile.getName());
            ex.printStackTrace();
        }

        if (asynchronous) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    synchronized (saveLock) {
                        try {
                            arenaSaves.save(arenaSaveFile);
                        } catch (Exception ex) {
                            plugin.getLogger().warning("Error saving arena data to " + arenaSaveFile.getName());
                            ex.printStackTrace();
                        }
                    }
                }
            });
        } else {
            try {
                arenaSaves.save(arenaSaveFile);
            } catch (Exception ex) {
                plugin.getLogger().warning("Error saving arena data to " + arenaSaveFile.getName() + " synchronously");
                ex.printStackTrace();
            }
        }
    }

    public void load() {
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        if (task != null) {
            task.cancel();
            task = null;
        }
        ConfigurationSection arenaConfiguration = loadDataFile("arenas");
        load(arenaConfiguration);

        ConfigurationSection arenaData = loadDataFile("data");
        loadData(arenaData);

        plugin.reloadConfig();
        Configuration config = plugin.getConfig();
        pathTemplate = config.getString("path_template", pathTemplate);
        tickInterval = config.getInt("tick_interval", 40);

        task = scheduler.runTaskTimer(plugin, this, 1, tickInterval);
    }

    private void save(ConfigurationSection configuration) {
        Collection<String> oldKeys = configuration.getKeys(false);
        for (String oldKey : oldKeys) {
            configuration.set(oldKey, null);
        }
        for (Arena arena : arenas.values()) {
            ConfigurationSection arenaConfig = configuration.createSection(arena.getKey());
            arena.save(arenaConfig);
        }
    }

    private void saveData(ConfigurationSection configuration) {
        Collection<String> oldKeys = configuration.getKeys(false);
        for (String oldKey : oldKeys) {
            configuration.set(oldKey, null);
        }
        for (Arena arena : arenas.values()) {
            ConfigurationSection arenaConfig = configuration.createSection(arena.getKey());
            arena.saveData(arenaConfig);
        }
    }

    private void load(ConfigurationSection configuration) {
        if (configuration == null) return;

        Collection<String> arenaKeys = configuration.getKeys(false);

        for (Arena arena : arenas.values()) {
            arena.remove();
        }
        arenas.clear();
        for (String arenaKey : arenaKeys) {
            Arena arena = new Arena(arenaKey, this);
            arena.load(configuration.getConfigurationSection(arenaKey));
            arenas.put(arenaKey.toLowerCase(), arena);
        }

        plugin.getLogger().info("Loaded " + arenas.size() + " arenas");
    }

    private void loadData(ConfigurationSection configuration) {
        if (configuration == null) return;

        Collection<String> arenaKeys = configuration.getKeys(false);

        for (String arenaKey : arenaKeys) {
            Arena arena = arenas.get(arenaKey);
            if (arena != null) {
                arena.loadData(configuration.getConfigurationSection(arenaKey));
            }
        }
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public Arena getArena(String arenaName) {
        return arenas.get(arenaName.toLowerCase());
    }

    public Arena getMobArena(LivingEntity entity) {
        if (entity.hasMetadata("arena")) {
            for (MetadataValue value : entity.getMetadata("arena")) {
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
    
    public Arena getArena(Player player) {
        ArenaPlayer arenaPlayer = getArenaPlayer(player);
        return arenaPlayer == null ? null : arenaPlayer.getArena();
    }

    public ArenaPlayer getArenaPlayer(Player player) {
        if (player.hasMetadata("arena")) {
            for (MetadataValue value : player.getMetadata("arena")) {
                if (value.getOwningPlugin().equals(getPlugin())) {
                    Object arena = value.value();
                    if (arena instanceof ArenaPlayer) {
                        return (ArenaPlayer)value.value();
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
        Arena arena = arenas.get(arenaName.toLowerCase());
        if (arena != null) {
            arena.remove();
            arenas.remove(arenaName.toLowerCase());
        }
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public ArenaPlayer leave(Player player) {
        ArenaPlayer arenaPlayer = getArenaPlayer(player);
        if (arenaPlayer != null) {
            Arena arena = arenaPlayer.getArena();
            arena.remove(player);
            player.sendMessage("You have left " + arena.getName());
            arenaPlayer.teleport(arena.getExit());
            arena.check();
        }

        return arenaPlayer;
    }

    public void reset(Player player) {
        for (Arena arena : arenas.values()) {
            arena.reset(player);
        }
    }

    public void reset() {
        for (Arena arena : arenas.values()) {
            arena.reset();
        }
    }

    public boolean isInArena(Player player) {
        return player.hasMetadata("arena");
    }

    public MageController getMagic() {
        return magic;
    }

    public String getPathTemplate() {
        return pathTemplate;
    }

    @Override
    public void run() {
        for (Arena arena : arenas.values()) {
            if (arena.isStarted()) {
                arena.tick();
            }
        }
    }
}
