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
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ArenaStage {
    private static Random random = new Random();
    private final Arena arena;
    private final int index;
    private List<ArenaMobSpawner> mobs = new ArrayList<ArenaMobSpawner>();
    private List<Location> mobSpawns = new ArrayList<Location>();
    private Set<Entity> spawned = new HashSet<Entity>();
    private String startSpell;
    private String endSpell;
    private String name;

    private Vector randomizeMobSpawn;

    private int winXP = 0;
    private int winSP = 0;
    private int winMoney = 0;

    private boolean defaultDrops = false;

    public ArenaStage(Arena arena, int index) {
        this.arena = arena;
        this.index = index;
    }

    public ArenaStage(Arena arena, int index, MageController controller, ConfigurationSection configuration) {
        this.arena = arena;
        this.index = index;
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
        name = configuration.getString("name");
        winXP = configuration.getInt("win_xp");
        winSP = configuration.getInt("win_sp");
        winMoney = configuration.getInt("win_money");
        defaultDrops = configuration.getBoolean("drops");

        if (configuration.contains("randomize_mob_spawn")) {
            randomizeMobSpawn = ConfigurationUtils.toVector(configuration.getString("randomize_mob_spawn"));
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
        configuration.set("name", name);
        configuration.set("win_xp", winXP);
        configuration.set("win_sp", winSP);
        configuration.set("win_money", winMoney);
        configuration.set("drops", defaultDrops);

        if (randomizeMobSpawn != null) {
            configuration.set("randomize_mob_spawn", ConfigurationUtils.fromVector(randomizeMobSpawn));
        }
    }

    public void addMob(EntityData entityType, int count) {
        mobs.add(new ArenaMobSpawner(entityType, count));
    }

    public void describe(CommandSender sender, String prefix) {
        sender.sendMessage(prefix + ChatColor.AQUA + getName());
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
        if (randomizeMobSpawn != null) {
            sender.sendMessage(ChatColor.DARK_BLUE + " Randomize Spawning: " + ChatColor.BLUE + randomizeMobSpawn);
        }

        if (startSpell != null) {
            sender.sendMessage(prefix + ChatColor.DARK_AQUA + "Cast at Start: " + ChatColor.AQUA + startSpell);
        }

        if (endSpell != null) {
            sender.sendMessage(prefix + ChatColor.DARK_AQUA + "Cast at End: " + ChatColor.AQUA + endSpell);
        }

        if (winXP > 0) {
            sender.sendMessage(ChatColor.AQUA + "Winning Reward: " + ChatColor.LIGHT_PURPLE + winXP + ChatColor.AQUA + " xp");
        }
        if (winSP > 0) {
            sender.sendMessage(ChatColor.AQUA + "Winning Reward: " + ChatColor.LIGHT_PURPLE + winSP + ChatColor.AQUA + " sp");
        }
        if (winMoney > 0) {
            sender.sendMessage(ChatColor.AQUA + "Winning Reward: $" + ChatColor.LIGHT_PURPLE + winMoney);
        }
    }

    protected String describeMob(ArenaMobSpawner mob) {
        if (mob == null) {
            return ChatColor.RED + "(Invalid Mob)";
        }
        if (mob.getEntity() == null) {
            return ChatColor.RED + "(Invalid Mob)" + ChatColor.YELLOW + " x" + mob.getCount();
        }
        return ChatColor.DARK_GREEN + " " + mob.getEntity().describe() + ChatColor.YELLOW + " x" + mob.getCount();
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
        arena.messageInGamePlayers(getName());
        if (!mobs.isEmpty()) {
            MageController magic = arena.getController().getMagic();
            magic.setForceSpawn(true);
            try {
                List<Location> spawns = getMobSpawns();
                int num = 0;
                for (ArenaMobSpawner mobSpawner : mobs) {
                    EntityData mobType = mobSpawner.getEntity();
                    if (mobType == null) continue;
                    for (int i = 0; i < mobSpawner.getCount(); i++) {
                        Location spawn = spawns.get(num);
                        if (randomizeMobSpawn != null) {
                            spawn = spawn.clone();
                            spawn.add
                            (
                                (2 * random.nextDouble() - 1) * randomizeMobSpawn.getX(),
                                (2 * random.nextDouble() - 1) * randomizeMobSpawn.getY(),
                                (2 * random.nextDouble() - 1) * randomizeMobSpawn.getZ()
                            );
                        }
                        num = (num + 1) % spawns.size();
                        Entity spawnedEntity = mobType.spawn(spawn);
                        if (spawnedEntity != null) {
                            arena.getController().register(spawnedEntity, arena);
                            spawned.add(spawnedEntity);
                            if (!defaultDrops) {
                                magic.disableDrops(spawnedEntity);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();;
            }
            magic.setForceSpawn(false);
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

    public void mobDied(LivingEntity entity) {
        arena.getController().unregister(entity);
        spawned.remove(entity);
    }

    public void completed() {
        arena.messageInGamePlayers(ChatColor.GREEN + "Congratulations!" + ChatColor.AQUA + "  You have passed " + ChatColor.DARK_AQUA + getName());

        Set<ArenaPlayer> players = arena.getAllInGamePlayers();
        for (ArenaPlayer player : players) {
            Mage mage = player.getMage();
            if (winXP > 0) {
                mage.sendMessage(ChatColor.AQUA + "You have been awarded " + ChatColor.DARK_AQUA + Integer.toString(winXP) + ChatColor.AQUA + " experience!");
                mage.giveExperience(winXP);
            }
            if (winSP > 0) {
                mage.sendMessage(ChatColor.AQUA + "You have been awarded " + ChatColor.DARK_AQUA + Integer.toString(winSP) + ChatColor.AQUA + " spell points!");
                mage.addSkillPoints(winSP);
            }
            if (winMoney > 0) {
                mage.sendMessage(ChatColor.AQUA + "You have been awarded $" + ChatColor.DARK_AQUA + Integer.toString(winMoney) + ChatColor.AQUA + "!");
                mage.addVaultCurrency(winMoney);
            }
        }

        finish();
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

        for (Entity entity : spawned) {
            if (entity.isValid()) {
                arena.getController().unregister(entity);;
                entity.remove();
            }
        }
        spawned.clear();
    }

    public boolean hasMobs() {
        return !mobs.isEmpty();
    }

    public boolean isFinished() {
        for (Entity entity : spawned) {
            if (entity.isValid()) {
                return false;
            }
        }

        return true;
    }

    public String getName() {
        if (name != null) {
            return ChatColor.translateAlternateColorCodes('&', name);
        }
        return "Stage " + getNumber();
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumber() {
        return index + 1;
    }

    public void reset() {
        for (Entity entity : spawned) {
            entity.remove();
        }
        spawned.clear();;
    }

    public void setRandomizeMobSpawn(Vector vector) {
        randomizeMobSpawn = vector;
    }

    public void setWinXP(int xp) {
        winXP = Math.max(xp, 0);
    }

    public void setWinSP(int sp) {
        winSP = Math.max(sp, 0);
    }

    public void setWinMoney(int money) {
        winMoney = Math.max(money, 0);
    }
}
