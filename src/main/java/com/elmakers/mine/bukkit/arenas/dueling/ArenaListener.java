package com.elmakers.mine.bukkit.arenas.dueling;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.Collection;

public class ArenaListener implements Listener {
    private final ArenaController controller;

    public ArenaListener(ArenaController controller) {
        this.controller = controller;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        if (player.hasMetadata("respawnLocation")) {
            Collection<MetadataValue> metadata = player.getMetadata("respawnLocation");
            for (MetadataValue value : metadata) {
                e.setRespawnLocation((Location)value.value());
            }
            player.removeMetadata("respawnLocation", controller.getPlugin());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        Arena arena = controller.getArena(player);
        if (arena != null) {
            arena.remove(player);
            Location specroom = arena.getSpectatingRoom();
            player.setMetadata("respawnLocation", new FixedMetadataValue(controller.getPlugin(), specroom));
            player.sendMessage(ChatColor.AQUA + "You have lost :( Better luck next time!");
            arena.check();
        }
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent e) {
        Player player = e.getPlayer();
        if (controller.playerLeft(player)) {
            e.setLeaveMessage(ChatColor.AQUA + e.getPlayer().getName() + " was kicked out of the arena!");
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent e){
        if (e.getLine(0).contains("Duel")) {
            if (!e.getLine(1).isEmpty()) {
                String arenaName = e.getLine(1);
                Arena arena = controller.getArena(arenaName);
                if (arena != null) {
                    e.setLine(0,ChatColor.GOLD + "[" + ChatColor.BLUE + "Duel" + ChatColor.GOLD + "]");
                } else {
                    e.getBlock().breakNaturally();
                    e.getPlayer().sendMessage(ChatColor.RED + "Unknown arena!");
                }
            } else{
                e.getBlock().breakNaturally();
                e.getPlayer().sendMessage(ChatColor.RED + "You must specify an arena!");
            }
        }
    }

    @EventHandler
    public void onPlayerRightClickSign(PlayerInteractEvent e){
        Block clickedBlock = e.getClickedBlock();
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && (clickedBlock.getType() == Material.SIGN || clickedBlock.getType() == Material.SIGN_POST || clickedBlock.getType() == Material.WALL_SIGN)) {
            Sign sign = (Sign) e.getClickedBlock().getState();
            if (sign.getLine(0).contains("Duel")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dueling admin join " + sign.getLine(1) + " " + e.getPlayer().getName());
            }
        }
    }
    @EventHandler
    public void onSignChange2(SignChangeEvent e){
        if (e.getLine(0).contains("Leave")) {
            e.setLine(0,ChatColor.GOLD + "[" + ChatColor.BLUE + "Leave" + ChatColor.GOLD + "]");
        }
    }

    @EventHandler
    public void onPlayerRightClickSign2(PlayerInteractEvent e){
        Block clickedBlock = e.getClickedBlock();
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && (clickedBlock.getType() == Material.SIGN || clickedBlock.getType() == Material.SIGN_POST || clickedBlock.getType() == Material.WALL_SIGN)) {
            Sign sign = (Sign) e.getClickedBlock().getState();
            if (sign.getLine(0).contains("Leave")) {
                controller.leave(e.getPlayer());
            }
        }
    }
}
