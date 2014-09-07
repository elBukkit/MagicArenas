package com.elmakers.mine.bukkit.arenas.dueling;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.Collection;

public class ArenaListener implements Listener {
    private final ArenaController controller;
    private final static String SIGN_KEY = ChatColor.GOLD + "[" + ChatColor.BLUE + "Arena" + ChatColor.GOLD + "]";

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
            arena.increment(player, "lost");
            arena.remove(player);
            Location specroom = arena.getLoseLocation();
            player.setMetadata("respawnLocation", new FixedMetadataValue(controller.getPlugin(), specroom));
            player.sendMessage(ChatColor.AQUA + "You have lost - Better luck next time!");
            arena.check();
        }
        if (player.hasMetadata("death_message")) {
            Collection<MetadataValue> metadata = player.getMetadata("death_message");
            for (MetadataValue value : metadata) {
                e.setDeathMessage(ChatColor.translateAlternateColorCodes('&', value.asString()).replace("@p", player.getDisplayName()));
            }
            player.removeMetadata("death_message", controller.getPlugin());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        Arena leftArena = controller.leave(player);
        if (leftArena != null) {
            leftArena.increment(player, "quit");
            Bukkit.broadcastMessage(ChatColor.RED + player.getDisplayName() + ChatColor.DARK_AQUA + " has left " + ChatColor.AQUA + leftArena.getName());
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        Player player = e.getPlayer();
        if (!player.hasPermission("MagicArenas.signs.create")) {
            return;
        }
        String firstLine = e.getLine(0);
        if (firstLine.equalsIgnoreCase("[Arena]")) {
            String secondLine = e.getLine(1);
            if (secondLine.equalsIgnoreCase("Join")) {
                String arenaName = e.getLine(2);
                if (!arenaName.isEmpty()) {
                    Arena arena = controller.getArena(arenaName);
                    if (arena != null) {
                        e.setLine(0, SIGN_KEY);
                        e.setLine(1, ChatColor.DARK_AQUA + "Join");
                    } else {
                        e.getBlock().breakNaturally();
                        e.getPlayer().sendMessage(ChatColor.RED + "Unknown arena: " + arenaName);
                    }
                } else{
                    e.getBlock().breakNaturally();
                    e.getPlayer().sendMessage(ChatColor.RED + "You must specify an arena!");
                }
            } else if (secondLine.equalsIgnoreCase("Leave")) {
                e.setLine(0, SIGN_KEY);
                e.setLine(1, ChatColor.AQUA + "Leave");
            } else {
                e.getBlock().breakNaturally();
                e.getPlayer().sendMessage(ChatColor.RED + "You must specify Join or Leave");
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!player.hasPermission("MagicArenas.signs.use")) {
            return;
        }

        Block clickedBlock = e.getClickedBlock();
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && (clickedBlock.getType() == Material.SIGN || clickedBlock.getType() == Material.SIGN_POST || clickedBlock.getType() == Material.WALL_SIGN)) {
            Sign sign = (Sign) e.getClickedBlock().getState();
            String firstLine = sign.getLine(0);
            if (firstLine.equals(SIGN_KEY)) {
                String secondLine = sign.getLine(1);
                if (secondLine.contains("Join")) {
                    String arenaName = sign.getLine(2);
                    Arena arena = controller.getArena(arenaName);
                    if (arena != null) {
                        arena.join(e.getPlayer());
                    } else {
                        player.sendMessage(ChatColor.RED + "Sorry, that arena isn't available.");
                    }
                } else if (secondLine.contains("Leave")) {
                    controller.leave(e.getPlayer());
                }
            }
        }
    }

    protected boolean onEnterPortal(Entity entity) {
        // Mob arenas eventually!
        if (!(entity instanceof Player)) {
            return false;
        }
        Player player = ((Player)entity).getPlayer();
        Arena arena = controller.getArena(player);
        if (arena != null && arena.getPortalEnterDamage() > 0) {
            String portalDeathMessage = arena.getPortalDeathMessage();
            if (portalDeathMessage != null && !portalDeathMessage.isEmpty()) {
                player.setMetadata("death_message", new FixedMetadataValue(controller.getPlugin(), portalDeathMessage));
            }
            player.damage(arena.getPortalEnterDamage());
            if (portalDeathMessage != null && !portalDeathMessage.isEmpty()) {
                player.removeMetadata("death_message", controller.getPlugin());
            }
            return true;
        }

        return false;
    }

    protected boolean onPortal(Entity entity) {
        // Mob arenas eventually!
        if (!(entity instanceof Player)) {
            return false;
        }
        Player player = ((Player)entity).getPlayer();
        Arena arena = controller.getArena(player);
        if (arena != null && arena.getPortalDamage() > 0) {
            String portalDeathMessage = arena.getPortalDeathMessage();
            if (portalDeathMessage != null && !portalDeathMessage.isEmpty()) {
                player.setMetadata("death_message", new FixedMetadataValue(controller.getPlugin(), portalDeathMessage));
            }
            player.damage(arena.getPortalDamage());
            if (portalDeathMessage != null && !portalDeathMessage.isEmpty()) {
                player.removeMetadata("death_message", controller.getPlugin());
            }

            return true;
        }

        return false;
    }

    @EventHandler
    public void onEntityPortal(EntityPortalEnterEvent event) {
        onEnterPortal(event.getEntity());
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (onPortal(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
