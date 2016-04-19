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
    private String startSpell;
    private String endSpell;
    
    public ArenaStage() {
        
    }
    
    public ArenaStage(MageController controller, ConfigurationSection configuration) {
        if (configuration.contains("mobs")) {
            Collection<ConfigurationSection> mobConfigurations = ConfigurationUtils.getNodeList(configuration, "mobs");
            for (ConfigurationSection mobConfiguration : mobConfigurations) {
                mobs.add(new ArenaMobSpawner(controller, mobConfiguration));
            }
        }
        startSpell = configuration.getString("spell_start");
        endSpell = configuration.getString("spell_end");
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
            sender.sendMessage(ChatColor.DARK_BLUE + " Mobs: " + ChatColor.BLUE + numMobs);
            for (ArenaMobSpawner mob : mobs) {
                sender.sendMessage("  " + describeMob(mob));
            }
        }
        
        if (startSpell != null) {
            sender.sendMessage(ChatColor.DARK_AQUA + " Cast at Start: " + ChatColor.AQUA + startSpell);
        }

        if (endSpell != null) {
            sender.sendMessage(ChatColor.DARK_AQUA + " Cast at End: " + ChatColor.AQUA + endSpell);
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
}
