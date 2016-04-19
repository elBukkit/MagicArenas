package com.elmakers.mine.bukkit.arenas.dueling;

import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.entity.EntityData;
import com.elmakers.mine.bukkit.utility.ConfigurationUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArenaStage {
    private List<ArenaMobSpawner> mobs = new ArrayList<ArenaMobSpawner>();
    
    public ArenaStage() {
        
    }
    
    public ArenaStage(MageController controller, ConfigurationSection configuration) {
        if (configuration.contains("mobs")) {
            Collection<ConfigurationSection> mobConfigurations = ConfigurationUtils.getNodeList(configuration, "mobs");
            for (ConfigurationSection mobConfiguration : mobConfigurations) {
                mobs.add(new ArenaMobSpawner(controller, mobConfiguration));
            }
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
    }
    
    public void addMob(EntityData entityType, int count) {
        mobs.add(new ArenaMobSpawner(entityType, count));
    }
    
    public void describe(CommandSender sender) {
        int numMobs = mobs.size();
        if (numMobs == 0) {
            sender.sendMessage(ChatColor.GRAY + " (No Mobs)");
        } else if (numMobs == 1) {
            sender.sendMessage(describeMob(mobs.get(0)));
        } else {
            sender.sendMessage(ChatColor.DARK_BLUE + "Mobs: " + ChatColor.BLUE + numMobs);
            for (ArenaMobSpawner mob : mobs) {
                sender.sendMessage(describeMob(mob));
            }
        }
    }
    
    protected String describeMob(ArenaMobSpawner mob) {
        return ChatColor.DARK_GREEN + " " + mob.getEntity().describe() + ChatColor.YELLOW + " x" + mobs.get(0).getCount();
    }
}
