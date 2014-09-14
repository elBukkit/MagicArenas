package com.elmakers.mine.bukkit.arenas.dueling;

import com.elmakers.mine.bukkit.utility.ConfigurationUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ArenaCommandExecutor implements TabExecutor {
    private final static String[] SUB_COMMANDS = {
        "start", "stop", "add", "remove", "configure", "describe", "join", "leave", "load",
        "save", "stats", "reset"
    };

    private final static String[] ARENA_PROPERTIES = {
            "max", "min", "win", "lose", "lobby", "spawn", "exit", "center",
            "add", "remove", "randomize", "name", "description", "portal_damage",
            "portal_enter_damage", "portal_death_message", "leaderboard_games_required",
            "leaderboard_size", "leaderboard_record_size",
            "xp_win", "xp_lose", "xp_draw"
    };

    private final static String[] ARENA_LISTS = {
            "spawn"
    };

    private final static String[] ARENA_RANDOMIZE = {
            "spawn"
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
        } else if (args.length == 2) {
            Collection<Arena> arenas = controller.getArenas();
            for (Arena arena : arenas) {
                allOptions.add(arena.getKey());
            }
            if (args[0].equalsIgnoreCase("reset")) {
                allOptions.add("ALL");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("configure")) {
            allOptions.addAll(Arrays.asList(ARENA_PROPERTIES));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("configure") && (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove"))) {
            allOptions.addAll(Arrays.asList(ARENA_LISTS));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("configure") && args[2].equalsIgnoreCase("randomize")) {
            allOptions.addAll(Arrays.asList(ARENA_RANDOMIZE));
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("leave") || args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("reset"))) {
            allOptions.addAll(controller.getMagic().getPlayerNames());
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
            sender.sendMessage("Configuration and data reloaded");
            return true;
        }

        if (subCommand.equalsIgnoreCase("save")) {
            controller.save();
            sender.sendMessage("Data saved");
            return true;
        }

        if (subCommand.equalsIgnoreCase("describe") && args.length < 2) {
            Collection<Arena> arenas = controller.getArenas();
            sender.sendMessage(ChatColor.BLUE + "Arenas: " + ChatColor.DARK_AQUA + arenas.size());
            for (Arena arena : arenas) {
                String arenaMessage = ChatColor.AQUA + arena.getName() + ChatColor.GRAY + " (" + arena.getKey() + ")";
                if (arena.isStarted()) {
                    arenaMessage = arenaMessage + ChatColor.GREEN + "ACTIVE";
                }
                int minPlayers = arena.getMinPlayers();
                int maxPlayers = arena.getMaxPlayers();
                int queuedPlayers = arena.getQueuedPlayers();
                int inGamePlayers = arena.getInGamePlayers();
                arenaMessage = arenaMessage + ChatColor.WHITE + " (" + ChatColor.GREEN + inGamePlayers + ChatColor.WHITE
                        + ", " + ChatColor.YELLOW + queuedPlayers + ChatColor.WHITE + " / "
                        + ChatColor.GRAY + minPlayers + "-" + maxPlayers + ChatColor.WHITE + ")";
                sender.sendMessage(arenaMessage);
            }
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
            ArenaPlayer leftPlayer = controller.leave(player);
            if (leftPlayer != null) {
                sender.sendMessage(ChatColor.AQUA + playerName + " has left " + leftPlayer.getArena().getName());
            } else {
                sender.sendMessage(ChatColor.AQUA + playerName + ChatColor.RED + " is not in an arena");
            }

            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "You must provide an arena name");
            return true;
        }

        String arenaName = args[1];
        boolean isAllArenas = arenaName.equalsIgnoreCase("ALL");
        Arena arena = isAllArenas ? null : controller.getArena(arenaName);
        if (subCommand.equalsIgnoreCase("add")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Must be used in-game");
                return true;
            }
            Player player = (Player) sender;
            Location location = player.getLocation();
            ArenaType arenaType = ArenaType.ONEVONE;
            if (args.length > 2) {
                String arenaTypeName = args[2];
                arenaType = ArenaType.parse(arenaTypeName);
                if (arenaType == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown arena type: " + arenaTypeName);
                    return true;
                }
            }
            if (arena == null) {
                arena = controller.addArena(arenaName, location, 2, 2, arenaType);
                controller.save();
                player.sendMessage(ChatColor.AQUA + "Arena Created: " + arena.getName());
            } else {
                sender.sendMessage(ChatColor.AQUA + "Arena already exists!");
            }
            return true;
        }

        if (arena == null && !isAllArenas) {
            sender.sendMessage(ChatColor.RED + "Unknown arena: " + arenaName);
            return true;
        }

        if (subCommand.equalsIgnoreCase("reset")) {
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
            if (isAllArenas) {
                controller.reset(player);
                sender.sendMessage(ChatColor.AQUA + playerName + ChatColor.GRAY + " has been " + ChatColor.RED + " reset from ALL arenas");
            } else {
                arena.reset(player);
                sender.sendMessage(ChatColor.AQUA + playerName + ChatColor.GRAY + " has been " + ChatColor.RED + " reset from " + ChatColor.GOLD + arena.getName());
            }

            return true;
        }

        if (isAllArenas) {
            sender.sendMessage(ChatColor.RED + "ALL not applicable here.");
            return true;
        }

        if (subCommand.equalsIgnoreCase("remove")) {
            controller.remove(arenaName);
            controller.save();
            sender.sendMessage(ChatColor.RED + "Arena Removed: " + ChatColor.DARK_RED + arena.getName());
            return true;
        }

        if (subCommand.equalsIgnoreCase("start")) {
            arena.startCountdown(10);
            return true;
        }

        if (subCommand.equalsIgnoreCase("describe")) {
            arena.describe(sender);
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

        if (subCommand.equalsIgnoreCase("stats")) {
            if (args.length > 2) {
                String playerName = args[2];
                Player player = Bukkit.getPlayer(playerName);
                if (player == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown player: " + playerName);
                    return true;
                }
                arena.describeStats(sender, player);
            } else {
                arena.describeLeaderboard(sender);
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

            arena.join(player);
            return true;
        }

        if (subCommand.equalsIgnoreCase("configure")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Must specify a property name: ");
                sender.sendMessage(ChatColor.YELLOW + StringUtils.join(ARENA_PROPERTIES, ','));
                return true;
            }

            String propertyName = args[2];
            String[] configureArgs;

            if (args.length > 3) {
                configureArgs = new String[args.length - 3];
                System.arraycopy(args, 3, configureArgs, 0, args.length - 3);
            } else {
                configureArgs = new String[0];
            }
            onConfigureArena(sender, arena, propertyName, configureArgs);

            return true;
        }

        sender.sendMessage(ChatColor.RED + "Not a valid option: " + subCommand);
        sender.sendMessage(ChatColor.AQUA + "Options: " + StringUtils.join(SUB_COMMANDS, ", "));
        return true;
    }

    protected void onConfigureArena(CommandSender sender, Arena arena, String propertyName, String[] args)
    {
        if (propertyName.equalsIgnoreCase("randomize"))
        {
            String randomizeType = "spawn";
            if (args.length > 0) {
                randomizeType = args[0];
            }
            String vectorParameter = null;
            if (args.length > 1) {
                vectorParameter = args[1];
            }

            if (randomizeType.equalsIgnoreCase("spawn")) {
                if (vectorParameter == null || vectorParameter.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "Cleared randomized spawn of " + arena.getName());
                    arena.setRandomizeSpawn(null);
                } else {
                    Vector vector = ConfigurationUtils.toVector(vectorParameter);
                    sender.sendMessage(ChatColor.AQUA + "Set randomized spawn of " + arena.getName() + " to " + vector);
                    arena.setRandomizeSpawn(vector);
                }
                return;
            }

            sender.sendMessage(ChatColor.RED + "Not a valid randomization option: " + randomizeType);
            sender.sendMessage(ChatColor.AQUA + "Options: " + StringUtils.join(ARENA_RANDOMIZE, ", "));
            return;
        }

        if
        (
            propertyName.equalsIgnoreCase("lobby") || propertyName.equalsIgnoreCase("spawn")
        ||  propertyName.equalsIgnoreCase("win") || propertyName.equalsIgnoreCase("lose")
        ||  propertyName.equalsIgnoreCase("center") || propertyName.equalsIgnoreCase("exit")
        ||  propertyName.equalsIgnoreCase("add") || propertyName.equalsIgnoreCase("remove")
        ) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Must be used in-game");
                return;
            }
            Player player = (Player) sender;
            Location location = player.getLocation();

            boolean addLocation = propertyName.equalsIgnoreCase("add");
            boolean removeLocation = propertyName.equalsIgnoreCase("remove");
            if (addLocation || removeLocation)
            {
                String locList = "spawn";
                if (args.length > 0) {
                    locList = args[0];
                }

                if (locList.equalsIgnoreCase("spawn")) {
                    if (addLocation) {
                        arena.addSpawn(location);
                        controller.save();
                        sender.sendMessage(ChatColor.AQUA + "You have added a spawn location!");
                    } else {
                        Location removed = arena.removeSpawn(location);
                        if (removed != null) {
                            controller.save();
                            sender.sendMessage(ChatColor.AQUA + "You have removed a spawn location at: " + removed.toVector());
                        } else {
                            sender.sendMessage(ChatColor.RED + "No nearby spawn locations");
                        }
                    }

                    return;
                }

                sender.sendMessage(ChatColor.RED + "Not a valid location list: " + locList);
                sender.sendMessage(ChatColor.AQUA + "Options: " + StringUtils.join(ARENA_LISTS, ", "));
                return;
            }

            if (propertyName.equalsIgnoreCase("lobby")) {
                arena.setLobby(location);
                controller.save();
                sender.sendMessage(ChatColor.AQUA + "You have set the lobby!");
            } else if (propertyName.equalsIgnoreCase("spawn")) {
                arena.setSpawn(location);
                controller.save();
                sender.sendMessage(ChatColor.AQUA + "You have set the spawn location!");
            } else if (propertyName.equalsIgnoreCase("exit")) {
                arena.setExit(location);
                controller.save();
                sender.sendMessage(ChatColor.AQUA + "You have set the exit location!");
            } else if (propertyName.equalsIgnoreCase("center")) {
                arena.setCenter(location);
                controller.save();
                sender.sendMessage(ChatColor.AQUA + "You have set the center location!");
            } else if (propertyName.equalsIgnoreCase("lose")) {
                arena.setLoseLocation(location);
                controller.save();
                sender.sendMessage(ChatColor.AQUA + "You have set the spectating room!");
            } else if (propertyName.equalsIgnoreCase("win")) {
                arena.setWinLocation(location);
                controller.save();
                sender.sendMessage(ChatColor.AQUA + "You have set the treasure room!");
            }

            return;
        }

        String propertyValue = null;
        if (args.length > 0) {
            propertyValue = StringUtils.join(args, " ");
        }
        if (propertyName.equalsIgnoreCase("name")) {
            if (propertyValue == null || propertyValue.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Cleared name of " + arena.getName());
            } else {
                sender.sendMessage(ChatColor.AQUA + "Change name of " + arena.getName() + " to " + propertyValue);
            }
            arena.setName(propertyValue);
            return;
        }
        if (propertyName.equalsIgnoreCase("description")) {
            if (propertyValue == null || propertyValue.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Cleared description of " + arena.getName());
            } else {
                sender.sendMessage(ChatColor.AQUA + "Change description of " + arena.getName() + " to " + propertyValue);
            }
            arena.setDescription(propertyValue);
            return;
        }

        if (propertyName.equalsIgnoreCase("portal_death_message"))
        {
            if (propertyValue == null || propertyValue.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Cleared portal death message of " + arena.getName());
            } else {
                sender.sendMessage(ChatColor.AQUA + "Change portal death message of " + arena.getName() + " to " + propertyValue);
            }
            arena.setPortalDeathMessage(propertyValue);
            return;
        }

        if (propertyValue == null) {
            sender.sendMessage(ChatColor.RED + "Must specify a property value");
            return;
        }

        if (propertyName.equalsIgnoreCase("min") || propertyName.equalsIgnoreCase("max") ||
            propertyName.equalsIgnoreCase("portal_damage") || propertyName.equalsIgnoreCase("portal_enter_damage") ||
            propertyName.equalsIgnoreCase("leaderboard_games_required") || propertyName.equalsIgnoreCase("leaderboard_size") ||
            propertyName.equalsIgnoreCase("leaderboard_record_size") ||
            propertyName.equalsIgnoreCase("xp_win") || propertyName.equalsIgnoreCase("xp_lose") || propertyName.equalsIgnoreCase("xp_draw")
        ) {
            Integer intValue;
            try {
                intValue = Integer.parseInt(propertyValue);
            } catch (Exception ex) {
                intValue = null;
            }

            if (intValue == null) {
                sender.sendMessage(ChatColor.RED + "Not a valid integer: " + propertyValue);
                return;
            }

            if (propertyName.equalsIgnoreCase("leaderboard_games_required")) {
                arena.setLeaderboardGamesRequired(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set # games required for leaderboard on " + arena.getName() + " to " + intValue);
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("leaderboard_size")) {
                arena.setLeaderboardSize(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set leaderboard size of " + arena.getName() + " to " + intValue);
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("leaderboard_record_size")) {
                arena.setLeaderboardRecordSize(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set leaderboard record size of " + arena.getName() + " to " + intValue);
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("min")) {
                arena.setMinPlayers(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set min players of " + arena.getName() + " to " + intValue);
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("max")) {
                arena.setMaxPlayers(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set max players of " + arena.getName() + " to " + intValue);
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("xp_win")) {
                arena.setWinXP(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set winning XP of " + arena.getName() + " to " + intValue);
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("xp_lose")) {
                arena.setLoseXP(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set losing XP of " + arena.getName() + " to " + intValue);
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("xp_draw")) {
                arena.setDrawXP(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set draw XP of " + arena.getName() + " to " + intValue);
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("portal_damage")) {
                arena.setPortalDamage(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set portal damage of " + arena.getName() + " to " + intValue);
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("portal_enter_damage")) {
                arena.setPortalEnterDamage(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set portal entry damage of " + arena.getName() + " to " + intValue);
                controller.save();
                return;
            }
        }

        sender.sendMessage(ChatColor.RED + "Not a valid property: " + propertyName);
        sender.sendMessage(ChatColor.AQUA + "Options: " + StringUtils.join(ARENA_PROPERTIES, ", "));
    }
}