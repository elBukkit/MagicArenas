package com.elmakers.mine.bukkit.arenas.dueling;

import com.elmakers.mine.bukkit.api.magic.MageController;
import org.bukkit.Bukkit;
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
    private final MageController magic;
    private final Object saveLock = new Object();

    public ArenaController(Plugin plugin, MageController magic) {
        this.magic = magic;
        this.plugin = plugin;
    }

    public Arena addArena(String arenaName, Location location, int min, int max, ArenaType type) {
        Arena arena = new Arena(arenaName, this, location, min, max, type);
        arenas.put(arenaName, arena);
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
                            plugin.getLogger().info("Magic arena data saved");
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
                plugin.getLogger().info("Magic arena data saved synchronously");
            } catch (Exception ex) {
                plugin.getLogger().warning("Error saving arena data to " + arenaSaveFile.getName() + " synchronously");
                ex.printStackTrace();
            }
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

        for (Arena arena : arenas.values()) {
            arena.remove();
        }
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

    public Arena getArena(String arenaName) {
        return arenas.get(arenaName);
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
        Arena arena = arenas.get(arenaName);
        if (arena != null) {
            arena.remove();
            arenas.remove(arenaName);
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
            player.teleport(arena.getExit());
            arena.check();
        }

        return arenaPlayer;
    }

    public void reset(Player player) {
        for (Arena arena : arenas.values()) {
            arena.reset(player);
        }
    }

    public boolean isInArena(Player player) {
        return player.hasMetadata("arena");
    }

    public MageController getMagic() {
        return magic;
    }
}
