package com.elmakers.mine.bukkit.arenas.dueling;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ArenaCommandExecutor implements TabExecutor {
    private final static String[] SUB_COMMANDS = {
        "start", "stop", "add", "remove", "configure", "describe", "join", "leave", "load"
    };

    private final static String[] ARENA_PROPERTIES = {
            "max", "min", "win", "lose", "lobby", "spawn", "addspawn", "removespawn"
    };

    private final ArenaController controller;

    public ArenaCommandExecutor(ArenaController controller) {
        this.controller = controller;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String completeCommand = args.length > 0 ? args[args.length - 1] : "";

        List<String> allOptions = new ArrayList<String>();
        if (args.length < 2) {
            allOptions.addAll(Arrays.asList(SUB_COMMANDS));
        } else if (args.length == 3) {
            Collection<Arena> arenas = controller.getArenas();
            for (Arena arena : arenas) {
                allOptions.add(arena.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("configure")) {
            allOptions.addAll(Arrays.asList(SUB_COMMANDS));
        }

        completeCommand = completeCommand.toLowerCase();
        List<String> options = new ArrayList<String>();
        for (String option : allOptions) {
            String lowercase = option.toLowerCase();
            if (lowercase.startsWith(completeCommand)) {
                options.add(option);
            }
        }

        return options;
    }

    protected void sendNoPermission(CommandSender sender)
    {
        if (sender != null) sender.sendMessage(ChatColor.RED + "You are not allowed to use that command.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("arena")) {
            return false;
        }

        if (!sender.hasPermission("MagicArenas.commands.arena")) {
            sendNoPermission(sender);
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "---------" +  ChatColor.WHITE  + "Help: MagicArenas " + ChatColor.YELLOW +   "--------");
            sender.sendMessage("/arena add [name] <type> : Add a new arena");
            sender.sendMessage("/arena remove [name] : Remove an existing arena");
            sender.sendMessage("/arena start [name] : Manually start an arena");
            sender.sendMessage("/arena stop [name] : Manually stop an arena");
            sender.sendMessage("/arena describe [name] : List properties of an arena");
            sender.sendMessage("/arena join [name] <player> : Force a player to join an arena");
            sender.sendMessage("/arena leave [name] <player> : Force a player to leave an arena");
            sender.sendMessage("/arena configure [name] [property] <value> : Reconfigure an arena");
            sender.sendMessage(ChatColor.YELLOW + "-----------------------------------------------");
            return true;
        }

        String subCommand = args[0];
        if (!sender.hasPermission("MagicArenas.commands.arena." + subCommand)) {
            sendNoPermission(sender);
            return true;
        }

        if (subCommand.equalsIgnoreCase("load")) {
            controller.load();
            sender.sendMessage("Configuration reloaded");
            return true;
        }

        if (subCommand.equalsIgnoreCase("leave")) {
            Player player = null;
            String playerName = null;
            if (args.length > 1) {
                playerName = args[1];
                player = Bukkit.getPlayer(playerName);
            } else if (sender instanceof Player) {
                player = (Player) sender;
            }

            if (player == null) {
                if (playerName != null) {
                    sender.sendMessage(ChatColor.RED + "Unknown player: " + playerName);
                } else {
                    sender.sendMessage(ChatColor.RED + "You must specify a player name");
                }
                return true;
            }
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "You must provide an arena name");
            return true;
        }

        String arenaName = args[1];
        Arena arena = controller.getArena(arenaName);
        if (subCommand.equalsIgnoreCase("add")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Must be used in-game");
                return true;
            }
            Player player = (Player) sender;
            Location location = player.getLocation();
            ArenaType arenaType = ArenaType.FFA;
            if (args.length > 2) {
                String arenaTypeName = args[2];
                arenaType = ArenaType.parse(arenaTypeName);
                if (arenaType == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown arena type: " + arenaTypeName);
                    return true;
                }
            }
            if (arena == null) {
                arena = controller.addArena(arenaName, location, 2, 20, arenaType);
                controller.save();
                player.sendMessage(ChatColor.AQUA + "Arena Created: " + arenaName);
            } else {
                sender.sendMessage(ChatColor.AQUA + "Arena already exists!");
            }
            return true;
        }

        if (arena == null) {
            sender.sendMessage(ChatColor.RED + "Unknown arena: " + arenaName);
            return true;
        }

        if (subCommand.equalsIgnoreCase("remove")) {
            controller.remove(arenaName);
            controller.save();
            return true;
        }

        if (subCommand.equalsIgnoreCase("start")) {
            arena.startCountdown(10);
            return true;
        }

        if (subCommand.equalsIgnoreCase("stop")) {
            if (arena.stop()) {
                sender.sendMessage(ChatColor.AQUA + "Match stopped!");
            } else {
                sender.sendMessage(ChatColor.AQUA + "Arena not active");
            }
            return true;
        }

        if (subCommand.equalsIgnoreCase("join")) {
            Player player = null;
            String playerName = null;
            if (args.length > 2) {
                playerName = args[2];
                player = Bukkit.getPlayer(playerName);
            } else if (sender instanceof Player) {
                player = (Player) sender;
            }

            if (player == null) {
                if (playerName != null) {
                    sender.sendMessage(ChatColor.RED + "Unknown player: " + playerName);
                } else {
                    sender.sendMessage(ChatColor.RED + "You must specify a player name");
                }
                return true;
            }

            if (!arena.has(player)) {
                if (!arena.isStarted()) {
                    if (!arena.isFull()) {
                        arena.add(player);
                        Bukkit.broadcastMessage(ChatColor.AQUA + player.getDisplayName() + " has joined the queue for " + arenaName);
                        player.sendMessage(ChatColor.AQUA + "You have joined the game!");
                        player.setHealth(20.0);
                        player.setFoodLevel(20);
                        player.setFireTicks(0);
                        for (PotionEffect pt : player.getActivePotionEffects()) {
                            player.removePotionEffect(pt.getType());
                        }
                        if (arena.isReady()) {
                            arena.startCountdown(10);
                        } else {
                            arena.lobbyMessage();
                        }
                    } else {
                        Bukkit.getPlayer(args[3]).sendMessage(ChatColor.RED + "There are too many players! Wait until next round!");
                    }
                } else {
                    Bukkit.getPlayer(args[3]).sendMessage(ChatColor.RED + "There are too many players! Wait until next round!");
                }
            } else {
                Bukkit.getPlayer(args[3]).sendMessage(ChatColor.RED + "Already in game!");
            }
            return true;
        }

        if (subCommand.equalsIgnoreCase("configure")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Must specify a property name: ");
                sender.sendMessage(ChatColor.YELLOW + StringUtils.join(ARENA_PROPERTIES, ','));
                return true;
            }

            String propertyName = args[2];

            if
            (
               propertyName.equalsIgnoreCase("lobby") || propertyName.equalsIgnoreCase("spawn")
            || propertyName.equalsIgnoreCase("win") || propertyName.equalsIgnoreCase("lose")
            || propertyName.equalsIgnoreCase("addspawn") || propertyName.equalsIgnoreCase("removespawn")
            ) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Must be used in-game");
                    return true;
                }
                Player player = (Player) sender;
                Location location = player.getLocation();

                if (propertyName.equalsIgnoreCase("lobby")) {
                    arena.setLobby(location);
                    controller.save();
                    sender.sendMessage(ChatColor.AQUA + "You have set the lobby!");
                } else if (propertyName.equalsIgnoreCase("addspawn")) {
                    arena.addSpawn(location);
                    controller.save();
                    sender.sendMessage(ChatColor.AQUA + "You have added a spawn location!");
                } else if (propertyName.equalsIgnoreCase("spawn")) {
                    arena.setSpawn(location);
                    controller.save();
                    sender.sendMessage(ChatColor.AQUA + "You have set the spawn location!");
                } else if (propertyName.equalsIgnoreCase("removespawn")) {
                    Location removed = arena.removeSpawn(location);
                    if (removed != null) {
                        controller.save();
                        sender.sendMessage(ChatColor.AQUA + "You have removed a spawn location at: " + removed.toVector());
                    } else {
                        sender.sendMessage(ChatColor.RED + "No nearby spawn locations");
                    }
                } else if (propertyName.equalsIgnoreCase("lose")) {
                    arena.setSpectatingRoom(location);
                    controller.save();
                    sender.sendMessage(ChatColor.AQUA + "You have set the spectating room!");
                } else if (propertyName.equalsIgnoreCase("win")) {
                    arena.setTreasureRoom(location);
                    controller.save();
                    sender.sendMessage(ChatColor.AQUA + "You have set the treasure room!");
                }

                return true;
            }

            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Must specify a property value");
                return true;
            }

            String propertyValue = args[3];
            if (propertyName.equalsIgnoreCase("min") || propertyName.equalsIgnoreCase("max")) {
                Integer intValue = null;
                try {
                    intValue = Integer.parseInt(propertyValue);
                } catch (Exception ex) {
                    intValue = null;
                }

                if (intValue == null) {
                    sender.sendMessage(ChatColor.RED + "Not a valid integer: " + propertyValue);
                    return true;
                }
                if (propertyName.equalsIgnoreCase("min")) {
                    arena.setMinPlayers(intValue);
                    sender.sendMessage(ChatColor.AQUA + "Set min players");
                    controller.save();
                    return true;
                }

                if (propertyName.equalsIgnoreCase("max")) {
                    arena.setMaxPlayers(intValue);
                    sender.sendMessage(ChatColor.AQUA + "Set max players");
                    controller.save();
                    return true;
                }
            }

            sender.sendMessage(ChatColor.RED + "Not a valid property: " + propertyName);
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Not a valid option: " + subCommand);
        return true;
    }
}