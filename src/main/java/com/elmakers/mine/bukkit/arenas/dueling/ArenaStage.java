package com.elmakers.mine.bukkit.arenas.dueling;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.entity.EntityData;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.utility.ConfigurationUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Entity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArenaStage {
    private final Arena arena;
    private List<ArenaMobSpawner> mobs = new ArrayList<ArenaMobSpawner>();
    private List<Location> mobSpawns = new ArrayList<Location>();
    private List<WeakReference<Entity>> spawned = new ArrayList<WeakReference<Entity>>();
    private String startSpell;
    private String endSpell;
    
    public ArenaStage(Arena arena) {
        this.arena = arena;
    }
    
    public ArenaStage(Arena arena, MageController controller, ConfigurationSection configuration) {
        this.arena = arena;
        if (configuration.contains("mobs")) {
            Collection<ConfigurationSection> mobConfigurations = ConfigurationUtils.getNodeList(configuration, "mobs");
            for (ConfigurationSection mobConfiguration : mobConfigurations) {
                mobs.add(new ArenaMobSpawner(controller, mobConfiguration));
            }
        }
        startSpell = configuration.getString("spell_start");
        endSpell = configuration.getString("spell_end");
        
        for (String s : configuration.getStringList("mob_spawns")){
            mobSpawns.add(ConfigurationUtils.toLocation(s));
        }
    }
    
    public void save(ConfigurationSection configuration) {
        List<ConfigurationSection> mobsConfigurations = new ArrayList<ConfigurationSection>();
        for (ArenaMobSpawner mob : mobs) {
            ConfigurationSection section = new MemoryConfiguration();
            mob.save(section);
            mobsConfigurations.add(section);
        }
        configuration.set("mobs", mobsConfigurations);
        configuration.set("spell_start", startSpell);
        configuration.set("spell_end", endSpell);
        
        List<String> mobSpawnList = new ArrayList<String>();
        for (Location spawn : mobSpawns) {
            mobSpawnList.add(ConfigurationUtils.fromLocation(spawn));
        }
        configuration.set("mob_spawns", mobSpawnList);
    }
    
    public void addMob(EntityData entityType, int count) {
        mobs.add(new ArenaMobSpawner(entityType, count));
    }
    
    public void describe(CommandSender sender, String prefix) {
        int mobSpawnSize = mobSpawns.size();
        if (mobSpawnSize == 1) {
            sender.sendMessage(prefix + ChatColor.BLUE + "Spawn Mobs: " + arena.printLocation(mobSpawns.get(0)));
        } else if (mobSpawnSize > 1) {
            sender.sendMessage(prefix + ChatColor.BLUE + "Spawns Mobs: " + ChatColor.GRAY + mobSpawnSize);
            for (Location spawn : mobSpawns) {
                sender.sendMessage(prefix + arena.printLocation(spawn));
            }
        }
        
        int numMobs = mobs.size();
        if (numMobs == 0) {
            sender.sendMessage(prefix + ChatColor.GRAY + "(No Mobs)");
        } else if (numMobs == 1) {
            sender.sendMessage(prefix + describeMob(mobs.get(0)));
        } else {
            sender.sendMessage(prefix + ChatColor.DARK_BLUE + "Mobs: " + ChatColor.BLUE + numMobs);
            for (ArenaMobSpawner mob : mobs) {
                sender.sendMessage(prefix + " " + describeMob(mob));
            }
        }
        
        if (startSpell != null) {
            sender.sendMessage(prefix + ChatColor.DARK_AQUA + "Cast at Start: " + ChatColor.AQUA + startSpell);
        }

        if (endSpell != null) {
            sender.sendMessage(prefix + ChatColor.DARK_AQUA + "Cast at End: " + ChatColor.AQUA + endSpell);
        }
    }
    
    protected String describeMob(ArenaMobSpawner mob) {
        return ChatColor.DARK_GREEN + " " + mob.getEntity().describe() + ChatColor.YELLOW + " x" + mobs.get(0).getCount();
    }

    public String getStartSpell() {
        return startSpell;
    }

    public void setStartSpell(String startSpell) {
        this.startSpell = startSpell;
    }

    public String getEndSpell() {
        return endSpell;
    }

    public void setEndSpell(String endSpell) {
        this.endSpell = endSpell;
    }
    
    public void addMobSpawn(Location location) {
        mobSpawns.add(location.clone());
    }

    public Location removeMobSpawn(Location location) {
        int rangeSquared = 3 * 3;
        for (Location spawn : mobSpawns) {
            if (spawn.distanceSquared(location) < rangeSquared) {
                mobSpawns.remove(spawn);
                return spawn;
            }
        }

        return null;
    }
    
    public List<Location> getMobSpawns() {
        if (mobSpawns.size() == 0) {
            List<Location> centerList = new ArrayList<Location>();
            centerList.add(arena.getCenter());
            return centerList;
        }

        return mobSpawns;
    }
    
    public void start() {
        if (!mobs.isEmpty()) {
            MageController magic = arena.getController().getMagic();
            List<Location> spawns = getMobSpawns();
            int num = 0;
            for (ArenaMobSpawner mobSpawner : mobs) {
                EntityData mobType = mobSpawner.getEntity();
                if (mobType == null) continue;
                for (int i = 0; i < mobSpawner.getCount(); i++) {
                    Location spawn = spawns.get(num);
                    num = (num + 1) % spawns.size();
                    Entity spawnedEntity = mobType.spawn(magic, spawn);
                    if (spawnedEntity != null) {
                        spawned.add(new WeakReference<Entity>(spawnedEntity));
                    }
                }
            }
        }
        
        if (startSpell != null && !startSpell.isEmpty()) {
            Mage arenaMage = arena.getMage();
            arenaMage.setLocation(arena.getCenter());
            Spell spell = arenaMage.getSpell(startSpell);
            if (spell != null) {
               spell.cast(); 
            }
        }
    }
    
    public void finish() {
        if (endSpell != null && !endSpell.isEmpty()) {
            Mage arenaMage = arena.getMage();
            arenaMage.setLocation(arena.getCenter());
            Spell spell = arenaMage.getSpell(endSpell);
            if (spell != null) {
                spell.cast();
            }
        }
        
        for (WeakReference<Entity> spawnedEntity : spawned) {
            Entity entity = spawnedEntity.get();
            if (entity != null) {
                entity.remove();
            }
        }
        spawned.clear();
    }
}
