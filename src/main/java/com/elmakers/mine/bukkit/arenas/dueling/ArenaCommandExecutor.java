package com.elmakers.mine.bukkit.arenas.dueling;

import com.elmakers.mine.bukkit.api.entity.EntityData;
import com.elmakers.mine.bukkit.api.spell.SpellTemplate;
import com.elmakers.mine.bukkit.utility.ConfigurationUtils;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("deprecation")
public class ArenaCommandExecutor implements TabExecutor {
    private final static String[] SUB_COMMANDS = {
        "start", "stop", "add", "remove", "configure", "describe", "join", "leave", "load",
        "save", "stats", "reset", "stage"
    };

    private final static String[] STAGE_PROPERTIES = {
        "sp_win", "xp_win", "money_win", "randomize", "spell_start", "spell_end", "add", "remove",
        "duration", "respawn_duration"
    };

    private final static String[] STAGE_COMMANDS = {
        "add", "remove", "name", "next", "previous", "go", "describe", "addbefore", "addafter", "move",
        "configure", "all"
    };

    private final static String[] STAGE_RANDOMIZE = {
        "mob_spawn"
    };

    private final static String[] STAGE_LISTS = {
        "mob_spawn", "mob"
    };

    private final static String[] ARENA_PROPERTIES = {
        "max", "min", "win", "lose", "lobby", "spawn", "exit", "center",
        "add", "remove", "randomize", "name", "description", "portal_damage",
        "portal_enter_damage", "portal_death_message", "leaderboard_games_required",
        "leaderboard_size", "leaderboard_record_size", "max_teleport_distance",
        "xp_win", "xp_lose", "xp_draw", "countdown", "countdown_max", "op_check", "allow_interrupt",
        "announcer_range", "sp_win", "sp_lose", "sp_draw", "duration", "sudden_death",
        "sudden_death_effect", "start_commands", "border", "keep_inventory", "keep_level",
        "money_win", "money_lose", "money_draw", "item_wear",
        "allow_consuming", "leaderboard_sign_type", "allow_melee", "allow_projectiles"
    };

    private final static String[] ARENA_LISTS = {
        "spawn"
    };

    private final static String[] ARENA_RANDOMIZE = {
        "spawn"
    };

    private final static String[] BOOLEAN_PROPERTIES = {
        "true", "false"
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
        } else if (args.length == 3 && args[0].equalsIgnoreCase("stage")) {
            allOptions.addAll(Arrays.asList(STAGE_COMMANDS));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("configure")) {
            allOptions.addAll(Arrays.asList(ARENA_PROPERTIES));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("configure") && (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove"))) {
            allOptions.addAll(Arrays.asList(ARENA_LISTS));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("configure") && args[2].equalsIgnoreCase("randomize")) {
            allOptions.addAll(Arrays.asList(ARENA_RANDOMIZE));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("stage") && args[2].equalsIgnoreCase("configure")) {
            allOptions.addAll(Arrays.asList(STAGE_PROPERTIES));
        } else if (args.length == 5 && args[0].equalsIgnoreCase("stage") && args[2].equalsIgnoreCase("configure") && (args[3].equalsIgnoreCase("add") || args[3].equalsIgnoreCase("remove"))) {
            allOptions.addAll(Arrays.asList(STAGE_LISTS));
        } else if (args.length == 5 && args[0].equalsIgnoreCase("stage") && args[2].equalsIgnoreCase("configure") && args[3].equalsIgnoreCase("randomize")) {
            allOptions.addAll(Arrays.asList(STAGE_RANDOMIZE));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("configure") && (
                args[2].equalsIgnoreCase("keep_inventory")
                || args[2].equalsIgnoreCase("keep_level")
                || args[2].equalsIgnoreCase("item_wear")
                || args[2].equalsIgnoreCase("allow_consuming")
                || args[2].equalsIgnoreCase("op_check")
                || args[2].equalsIgnoreCase("allow_interrupt")
                )) {
            allOptions.addAll(Arrays.asList(BOOLEAN_PROPERTIES));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("configure") && args[2].equalsIgnoreCase("sudden_death_effect")) {
            for (PotionEffectType pt : PotionEffectType.values()) {
                if (pt == null) continue;
                String name = pt.getName();
                if (name == null) continue;
                allOptions.add(name.toLowerCase());
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("configure") && (args[2].equalsIgnoreCase("spell_start") || args[2].equalsIgnoreCase("spell_start"))) {
            Collection<SpellTemplate> spells = controller.getMagic().getSpellTemplates();
            for (SpellTemplate spell : spells) {
                allOptions.add(spell.getKey());
            }
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("leave") || args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("reset"))) {
            allOptions.addAll(controller.getMagic().getPlayerNames());
        } else if (args.length == 6 && args[0].equalsIgnoreCase("stage") && args[2].equalsIgnoreCase("configure") && args[3].equalsIgnoreCase("add") && args[4].equalsIgnoreCase("mob")) {
            allOptions.addAll(controller.getMagic().getMobKeys());
            for (EntityType entityType : EntityType.values()) {
                if (entityType.isAlive() && entityType.isSpawnable()) {
                    allOptions.add(entityType.name().toLowerCase());
                }
            }
        } else if (args.length == 6 && args[0].equalsIgnoreCase("stage") && args[2].equalsIgnoreCase("configure") && args[3].equalsIgnoreCase("remove") && args[4].equalsIgnoreCase("mob")) {
            Arena arena = controller.getArena(args[1]);
            if (arena != null) {
                EditingStage stage = arena.getIfEditingStage();
                if (stage != null) {
                    for (EntityData mob : stage.getSpawns()) {
                        String key = mob.getKey();
                        if (key != null && !key.isEmpty()) {
                            allOptions.add(key);
                        }
                    }
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("configure") && args[2].equalsIgnoreCase("leaderboard_sign_type")) {
            for (Material sign : controller.getMagic().getMaterialSetManager().getMaterialSet("signs").getMaterials()) {
                allOptions.add(sign.name().toLowerCase());
            }
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
            sender.sendMessage("/arena stage [name] [command] <value> : Display or configure mob arena stages");
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
            controller.saveData();
            sender.sendMessage("Data saved");
            return true;
        }

        if (subCommand.equalsIgnoreCase("describe") && args.length < 2) {
            Collection<Arena> arenas = controller.getArenas();
            sender.sendMessage(ChatColor.BLUE + "Arenas: " + ChatColor.DARK_AQUA + arenas.size());
            for (Arena arena : arenas) {
                String arenaMessage = ChatColor.AQUA + arena.getName() + ChatColor.GRAY + " (" + arena.getKey() + ")";
                if (arena.isStarted()) {
                    arenaMessage = arenaMessage + ChatColor.GREEN + " ACTIVE";
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
                playerName = player.getName();
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
                sender.sendMessage(ChatColor.AQUA + leftPlayer.getDisplayName() + " has left " + leftPlayer.getArena().getName());
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
            } else {
                if (isAllArenas) {
                    controller.reset();
                    sender.sendMessage(ChatColor.AQUA + "All arenas" + ChatColor.GRAY + " have been " + ChatColor.RED + " reset");
                } else {
                    arena.reset();
                    sender.sendMessage(ChatColor.AQUA + arena.getName() + ChatColor.GRAY + " has been " + ChatColor.RED + " reset");
                }
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
            arena.startCountdown();
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

        if (subCommand.equalsIgnoreCase("stage")) {
            if (args.length < 3) {
                sender.sendMessage("TODO: Stage overview for arena");
                return true;
            }
            String stageCommand = args[2];
            args = Arrays.copyOfRange(args, 3, args.length);
            onArenaStage(sender, arena, stageCommand, args);
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Not a valid option: " + subCommand);
        sender.sendMessage(ChatColor.AQUA + "Options: " + StringUtils.join(SUB_COMMANDS, ", "));
        return true;
    }

    protected void onArenaStage(CommandSender sender, Arena arena, String stageCommand, String[] args) {
        switch (stageCommand) {
            case "name":
                onNameArenaStage(sender, arena, args);
                break;
            case "all":
                onAllArenaStage(sender, arena);
                break;
            case "go":
                onGoArenaStage(sender, arena, args);
                break;
            case "move":
                onMoveArenaStage(sender, arena, args);
                break;
            case "addafter":
                onAddAfterArenaStage(sender, arena);
                break;
            case "configure":
                onConfigureArenaStage(sender, arena.getEditingStage(), args);
                break;
            case "addbefore":
                onAddBeforeArenaStage(sender, arena);
                break;
            case "add":
                onAddArenaStage(sender, arena);
                break;
            case "remove":
                onRemoveArenaStage(sender, arena);
                break;
            case "describe":
                onDescribeArenaStage(sender, arena);
                break;
            case "next":
                onNextArenaStage(sender, arena);
                break;
            case "previous":
                onPreviousArenaStage(sender, arena);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Not a valid stage command: " + stageCommand);
                sender.sendMessage(ChatColor.AQUA + "Options: " + StringUtils.join(STAGE_COMMANDS, ", "));
        }
    }

    protected void onAddAfterArenaStage(CommandSender sender, Arena arena) {
        arena.addStageAfterCurrent();
        showCurrentStage(sender, arena);
        controller.save();
    }

    protected void onAddBeforeArenaStage(CommandSender sender, Arena arena) {
        arena.addStageBeforeCurrent();
        showCurrentStage(sender, arena);
        controller.save();
    }

    protected void onAddArenaStage(CommandSender sender, Arena arena) {
        arena.addStage();
        showCurrentStage(sender, arena);
        controller.save();
    }

    protected void onRemoveArenaStage(CommandSender sender, Arena arena) {
        int stageCount = arena.getStageCount();
        if (stageCount <= 1) {
            sender.sendMessage(ChatColor.RED + "Can't remove the last stage");
            return;
        }
        EditingStage stage = arena.getEditingStage();
        arena.removeStage();
        sender.sendMessage(ChatColor.AQUA + "Removed stage: " + ChatColor.DARK_AQUA + stage.getFullName());
        showCurrentStage(sender, arena);
        controller.save();
    }

    protected void showCurrentStage(CommandSender sender, Arena arena) {
        EditingStage stage = arena.getEditingStage();
        int stageNumber = arena.getEditingStageIndex() + 1;
        sender.sendMessage(ChatColor.AQUA + "Current stage is now " + ChatColor.GOLD +
                stage.getFullName() + ChatColor.GRAY + " (" + ChatColor.YELLOW + stageNumber + ChatColor.GRAY + ")");
    }

    protected void onNextArenaStage(CommandSender sender, Arena arena) {
        int currentStage = arena.getEditingStageIndex();
        int stageCount = arena.getStageCount();
        if (currentStage >= stageCount - 1) {
            sender.sendMessage(ChatColor.YELLOW + "At end of list, wrapped to beginning");
            currentStage = 0;
        } else {
            currentStage++;
        }
        arena.setEditingStage(currentStage);
        showCurrentStage(sender, arena);
    }

    protected void onPreviousArenaStage(CommandSender sender, Arena arena) {
        int currentStage = arena.getEditingStageIndex();
        int stageCount = arena.getStageCount();
        if (currentStage <= 0) {
            sender.sendMessage(ChatColor.YELLOW + "At beginning of list, wrapped to end");
            currentStage = stageCount - 1;
        } else {
            currentStage--;
        }
        arena.setEditingStage(currentStage);
        showCurrentStage(sender, arena);
    }

    protected void onMoveArenaStage(CommandSender sender, Arena arena, String[] args) {
        String validStages = "1";
        int stageCount = arena.getStageCount();
        if (stageCount > 1) {
            validStages += " - " + stageCount;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: " + ChatColor.YELLOW + "/arena stage move [" + validStages + "]");
            return;
        }

        int stageNumber = 0;
        if (args.length > 0) {
            try {
                stageNumber = Integer.parseInt(args[0]);
            } catch (Exception ex) {

            }
        }
        if (stageNumber < 1 || stageNumber > stageCount) {
            sender.sendMessage(ChatColor.RED + "Not a valid stage number: " + stageNumber + ", use: " + validStages);
            return;
        }
        arena.moveCurrentStage(stageNumber);
        showCurrentStage(sender, arena);
    }

    protected void onGoArenaStage(CommandSender sender, Arena arena, String[] args) {
        int stageCount = arena.getStageCount();
        int stageNumber = 0;
        if (args.length > 0) {
            try {
                stageNumber = Integer.parseInt(args[0]);
            } catch (Exception ex) {

            }
        }
        String validStages = "1";
        if (stageCount > 1) {
            validStages += " - " + stageCount;
        }
        if (stageNumber <= 0) {
            sender.sendMessage(ChatColor.RED + "Usage: " + ChatColor.YELLOW + "/arena stage go [" + validStages + "]");
            return;
        }
        int stageIndex = stageNumber - 1;
        if (stageIndex < 0 || stageIndex >= stageCount) {
            sender.sendMessage(ChatColor.RED + "Invalid stage number, expecting: " + ChatColor.YELLOW + validStages);
            return;
        }
        arena.setEditingStage(stageIndex);
        showCurrentStage(sender, arena);
    }

    protected void onDescribeArenaStage(CommandSender sender, Arena arena) {
        EditingStage stage = arena.getEditingStage();
        stage.describe(sender);
    }

    protected void onAllArenaStage(CommandSender sender, Arena arena) {
        arena.setEditAllStages(true);
        sender.sendMessage("Arena " + ChatColor.AQUA + arena.getName() + ChatColor.WHITE + " now editing all stages at once");
    }

    protected void onNameArenaStage(CommandSender sender, Arena arena, String[] args) {
        EditingStage stage = arena.getEditingStage();
        if (args.length == 0) {
            stage.setName(null);
            sender.sendMessage("Cleared name of " + ChatColor.YELLOW + stage.getName());
            return;
        }
        String name = StringUtils.join(args, " ");
        stage.setName(name);
        sender.sendMessage("Set name to " + ChatColor.YELLOW + stage.getName());
        controller.save();
    }

    protected void onConfigureArenaStage(CommandSender sender, EditingStage stage, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: " + ChatColor.WHITE + "/area stage <arena> configure <property> [value]");
            return;
        }

        String propertyName = args[0];
        args = Arrays.copyOfRange(args, 1, args.length);
        if (propertyName.equalsIgnoreCase("randomize")) {
            String randomizeType = "mob_spawn";
            if (args.length > 0) {
                randomizeType = args[0];
            }
            String vectorParameter = null;
            if (args.length > 1) {
                vectorParameter = args[1];
            }

            if (randomizeType.equalsIgnoreCase("mob_spawn")) {
                if (vectorParameter == null || vectorParameter.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "Cleared randomized mob_spawn of " + stage.getFullName());
                    stage.setRandomizeMobSpawn(null);
                } else {
                    Vector vector = ConfigurationUtils.toVector(vectorParameter);
                    sender.sendMessage(ChatColor.AQUA + "Set randomized mob_spawn of " + stage.getFullName() + " to " + vector);
                    stage.setRandomizeMobSpawn(vector);
                }
                controller.save();
                return;
            }

            sender.sendMessage(ChatColor.RED + "Not a valid randomization option: " + randomizeType);
            sender.sendMessage(ChatColor.AQUA + "Options: " + StringUtils.join(STAGE_RANDOMIZE, ", "));
            return;
        }

        if (propertyName.equalsIgnoreCase("add") || propertyName.equalsIgnoreCase("remove")) {
            boolean isAdd = propertyName.equalsIgnoreCase("add");
            boolean isRemove = propertyName.equalsIgnoreCase("remove");
            if (isAdd || isRemove)
            {
                String subItem = "mob_spawn";
                if (args.length > 0) {
                    subItem = args[0];
                }

                if (subItem.equalsIgnoreCase("mob_spawn")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "Must be used in-game");
                        return;
                    }
                    Player player = (Player) sender;
                    Location location = player.getLocation();

                    if (isAdd) {
                        stage.addMobSpawn(location);
                        controller.save();
                        sender.sendMessage(ChatColor.AQUA + "You have added a mob spawn location!");
                    } else {
                        Location removed = stage.removeMobSpawn(location);
                        if (removed != null) {
                            controller.save();
                            sender.sendMessage(ChatColor.AQUA + "You have removed a mob spawn location at: " + removed.toVector());
                        } else {
                            sender.sendMessage(ChatColor.RED + "No nearby mob spawn locations");
                        }
                    }

                    return;
                } else if (subItem.equalsIgnoreCase("mob") && isAdd) {
                    if (args.length <= 1) {
                        sender.sendMessage(ChatColor.RED + "Missing mob type specifier");
                        return;
                    }

                    String entityType = args[1];
                    int count = 1;
                    if (args.length > 2) {
                        try {
                            count = Integer.parseInt(args[2]);
                        } catch (Exception ex) {
                            sender.sendMessage(ChatColor.RED + "Not a valid count: " + args[2]);
                            return;
                        }
                    }
                    EntityData mobType = controller.getMagic().getMob(entityType);
                    if (mobType == null) {
                        sender.sendMessage(ChatColor.RED + "Not a valid mob type: " + entityType);
                        return;
                    }
                    stage.addMob(mobType, count);
                    controller.save();
                    sender.sendMessage(ChatColor.AQUA + "Added " + ChatColor.YELLOW + count + ChatColor.BLUE
                            + " " + mobType.describe() + ChatColor.AQUA + " to " + ChatColor.GOLD + stage.getFullName());
                    return;
                } else if (subItem.equalsIgnoreCase("mob") && !isAdd) {
                    if (args.length <= 1) {
                        sender.sendMessage(ChatColor.RED + "Missing mob type specifier");
                        return;
                    }

                    String entityType = args[1];
                    EntityData mobType = controller.getMagic().getMob(entityType);
                    if (mobType == null) {
                        sender.sendMessage(ChatColor.RED + "Not a valid mob type: " + entityType);
                        return;
                    }
                    stage.removeMob(mobType);
                    controller.save();
                    sender.sendMessage(ChatColor.AQUA + "Removed " + ChatColor.BLUE
                            + mobType.describe() + ChatColor.AQUA + " from " + ChatColor.GOLD + stage.getFullName());
                    return;
                }

                sender.sendMessage(ChatColor.RED + "Not a valid add/remove option: " + subItem);
                sender.sendMessage(ChatColor.AQUA + "Options: " + StringUtils.join(ARENA_LISTS, ", "));
                return;
            }

            return;
        }

        String propertyValue = null;
        if (args.length > 0) {
            propertyValue = StringUtils.join(args, " ");
        }
        if (propertyName.equalsIgnoreCase("name")) {
            if (propertyValue == null || propertyValue.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Cleared name of " + stage.getFullName());
            } else {
                sender.sendMessage(ChatColor.AQUA + "Changed name of " + stage.getFullName() + " to " + propertyValue);
            }
            stage.setName(propertyValue);
            controller.save();
            return;
        }

        if (propertyName.equalsIgnoreCase("spell_start")) {
            stage.setStartSpell(propertyValue);
            if (propertyValue == null || propertyValue.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Cleared start spell for " + stage.getFullName());
            } else {
                sender.sendMessage(ChatColor.AQUA + "Set start spell for " + stage.getFullName());
            }
            controller.save();
            return;
        }

        if (propertyName.equalsIgnoreCase("spell_end")) {
            stage.setEndSpell(propertyValue);
            if (propertyValue == null || propertyValue.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Cleared end spell for " + stage.getFullName());
            } else {
                sender.sendMessage(ChatColor.AQUA + "Set end spell for " + stage.getFullName());
            }
            controller.save();
            return;
        }

        // Integers after here
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

        if (propertyName.equalsIgnoreCase("duration")) {
            stage.setDuration(intValue * 1000);
            sender.sendMessage(ChatColor.AQUA + "Set duration of " + stage.getFullName() + " to " + intValue + " seconds");
            controller.save();
            return;
        }

        if (propertyName.equalsIgnoreCase("respawn_duration")) {
            stage.setRespawnDuration(intValue * 1000);
            sender.sendMessage(ChatColor.AQUA + "Set respawn duration of " + stage.getFullName() + " to " + intValue + " seconds");
            controller.save();
            return;
        }

        if (propertyName.equalsIgnoreCase("sp_win")) {
            stage.setWinSP(intValue);
            sender.sendMessage(ChatColor.AQUA + "Set winning SP of " + stage.getFullName() + " to " + intValue);
            controller.save();
            return;
        }

        if (propertyName.equalsIgnoreCase("xp_win")) {
            stage.setWinXP(intValue);
            sender.sendMessage(ChatColor.AQUA + "Set winning XP of " + stage.getFullName() + " to " + intValue);
            controller.save();
            return;
        }

        if (propertyName.equalsIgnoreCase("money_win")) {
            stage.setWinMoney(intValue);
            sender.sendMessage(ChatColor.AQUA + "Set winning money of " + stage.getFullName() + " to " + intValue);
            controller.save();
            return;
        }

        sender.sendMessage(ChatColor.RED + "Not a valid property: " + propertyName);
        sender.sendMessage(ChatColor.AQUA + "Options: " + StringUtils.join(STAGE_PROPERTIES, ", "));
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
                controller.save();
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
            boolean isAdd = propertyName.equalsIgnoreCase("add");
            boolean isRemove = propertyName.equalsIgnoreCase("remove");
            if (isAdd || isRemove)
            {
                String subItem = "spawn";
                if (args.length > 0) {
                    subItem = args[0];
                }

                if (subItem.equalsIgnoreCase("spawn")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "Must be used in-game");
                        return;
                    }
                    Player player = (Player) sender;
                    Location location = player.getLocation();

                    if (isAdd) {
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

                sender.sendMessage(ChatColor.RED + "Not a valid add/remove option: " + subItem);
                sender.sendMessage(ChatColor.AQUA + "Options: " + StringUtils.join(ARENA_LISTS, ", "));
                return;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Must be used in-game");
                return;
            }
            Player player = (Player) sender;
            Location location = player.getLocation();

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
                sender.sendMessage(ChatColor.AQUA + "Changed name of " + arena.getName() + " to " + propertyValue);
            }
            arena.setName(propertyValue);
            controller.save();
            return;
        }
        if (propertyName.equalsIgnoreCase("description")) {
            if (propertyValue == null || propertyValue.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Cleared description of " + arena.getName());
            } else {
                sender.sendMessage(ChatColor.AQUA + "Change description of " + arena.getName() + " to " + propertyValue);
            }
            arena.setDescription(propertyValue);
            controller.save();
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
            controller.save();
            return;
        }

        if (propertyName.equalsIgnoreCase("sudden_death_effect")) {
            if (arena.setSuddenDeathEffect(propertyValue)) {
                sender.sendMessage(ChatColor.AQUA + "Set sudden death effects for " + arena.getName());
            } else {
                sender.sendMessage(ChatColor.RED + "Cleared sudden death effects for " + arena.getName());
            }
            return;
        }

        if (propertyName.equalsIgnoreCase("start_commands")) {
            arena.setStartCommands(propertyValue);
            if (propertyValue == null || propertyValue.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Cleared start commands for " + arena.getName());
            } else {
                sender.sendMessage(ChatColor.AQUA + "Set start commands for " + arena.getName());
            }
            controller.save();
            return;
        }

        if (propertyName.equalsIgnoreCase("border")) {
            if (propertyValue == null || propertyValue.isEmpty()) {
                arena.setBorder(0, 0);
                sender.sendMessage(ChatColor.RED + "Cleared border for " + arena.getName());
            } else {
                int min = 0;
                int max = 0;
                try {
                    if (propertyValue.contains("-")) {
                        String[] pieces = StringUtils.split(propertyValue, '-');
                        max = Integer.parseInt(pieces[0]);
                        min = Integer.parseInt(pieces[1]);
                        if (min > max) {
                            int temp = min;
                            min = max;
                            max = temp;
                        }
                    } else {
                        max = Integer.parseInt(propertyValue);
                    }
                } catch (Exception ex) {

                }
                arena.setBorder(min, max);
                controller.save();
                sender.sendMessage(ChatColor.AQUA + "Set border for " + arena.getName() + " to " + max + "-" + min);
            }
            return;
        }

        if (propertyValue == null) {
            sender.sendMessage(ChatColor.RED + "Must specify a property value");
            return;
        }

        if (propertyName.equalsIgnoreCase("op_check"))
        {
            boolean checkOn = propertyValue.equalsIgnoreCase("true");
            if (checkOn) {
                sender.sendMessage(ChatColor.RED + "Enabled OP check for " + arena.getName());
            } else {
                sender.sendMessage(ChatColor.AQUA + "Disabled OP check for " + arena.getName());
            }
            arena.setOpCheck(checkOn);
            controller.save();
            return;
        }

        if (propertyName.equalsIgnoreCase("allow_interrupt"))
        {
            boolean checkOn = propertyValue.equalsIgnoreCase("true");
            if (checkOn) {
                sender.sendMessage(ChatColor.RED + "Allow joining mid-match for " + arena.getName());
            } else {
                sender.sendMessage(ChatColor.AQUA + "Don't allow joining mid-match for " + arena.getName());
            }
            arena.setAllowInterrupt(checkOn);
            controller.save();
            return;
        }

        if (propertyName.equalsIgnoreCase("keep_inventory"))
        {
            boolean keepOn = propertyValue.equalsIgnoreCase("true");
            if (keepOn) {
                sender.sendMessage(ChatColor.GREEN + "Enabled keep inventory for " + arena.getName());
            } else {
                sender.sendMessage(ChatColor.RED + "Disabled keep inventory for " + arena.getName());
            }
            arena.setKeepInventory(keepOn);
            controller.save();
            return;
        }

        if (propertyName.equalsIgnoreCase("item_wear"))
        {
            boolean wear = propertyValue.equalsIgnoreCase("true");
            if (wear) {
                sender.sendMessage(ChatColor.GREEN + "Enabled item wear for " + arena.getName());
            } else {
                sender.sendMessage(ChatColor.RED + "Disabled item wear for " + arena.getName());
            }
            arena.setItemWear(wear);
            controller.save();
            return;
        }

        if (propertyName.equalsIgnoreCase("allow_consuming"))
        {
            boolean consume = propertyValue.equalsIgnoreCase("true");
            if (consume) {
                sender.sendMessage(ChatColor.GREEN + "Enabled consuming for " + arena.getName());
            } else {
                sender.sendMessage(ChatColor.RED + "Disabled consuming for " + arena.getName());
            }
            arena.setAllowConsuming(consume);
            controller.save();
            return;
        }

        if (propertyName.equalsIgnoreCase("allow_melee"))
        {
            boolean allow = propertyValue.equalsIgnoreCase("true");
            if (allow) {
                sender.sendMessage(ChatColor.GREEN + "Enabled melee for " + arena.getName());
            } else {
                sender.sendMessage(ChatColor.RED + "Disabled melee for " + arena.getName());
            }
            arena.setAllowMelee(allow);
            controller.save();
            return;
        }

        if (propertyName.equalsIgnoreCase("allow_projectiles"))
        {
            boolean allow = propertyValue.equalsIgnoreCase("true");
            if (allow) {
                sender.sendMessage(ChatColor.GREEN + "Enabled projectile weapons for " + arena.getName());
            } else {
                sender.sendMessage(ChatColor.RED + "Disabled projectile weapons for " + arena.getName());
            }
            arena.setAllowProjectiles(allow);
            controller.save();
            return;
        }

        if (propertyName.equalsIgnoreCase("leaderboard_sign_type"))
        {
            try {
                Material signMaterial = Material.valueOf(propertyValue.toUpperCase());
                arena.setLeaderboardSignType(signMaterial);
                sender.sendMessage(ChatColor.RED + "Set leaderboard sign type to " + signMaterial.name().toLowerCase());
                arena.updateLeaderboard();
                controller.save();
            } catch (Exception ex) {
                sender.sendMessage(ChatColor.RED + "Invalid sign type: " + propertyValue);
            }
            return;
        }

        if (propertyName.equalsIgnoreCase("keep_level"))
        {
            boolean keepOn = propertyValue.equalsIgnoreCase("true");
            if (keepOn) {
                sender.sendMessage(ChatColor.GREEN + "Enabled keep XP levels for " + arena.getName());
            } else {
                sender.sendMessage(ChatColor.RED + "Disabled keep XP levels for " + arena.getName());
            }
            arena.setKeepLevel(keepOn);
            controller.save();
            return;
        }

        if (propertyName.equalsIgnoreCase("min") || propertyName.equalsIgnoreCase("max") ||
            propertyName.equalsIgnoreCase("portal_damage") || propertyName.equalsIgnoreCase("portal_enter_damage") ||
            propertyName.equalsIgnoreCase("leaderboard_games_required") || propertyName.equalsIgnoreCase("leaderboard_size") ||
            propertyName.equalsIgnoreCase("leaderboard_record_size") || propertyName.equalsIgnoreCase("max_teleport_distance") ||
            propertyName.equalsIgnoreCase("xp_win") || propertyName.equalsIgnoreCase("xp_lose") || propertyName.equalsIgnoreCase("xp_draw") ||
            propertyName.equalsIgnoreCase("sp_win") || propertyName.equalsIgnoreCase("sp_lose") || propertyName.equalsIgnoreCase("sp_draw") ||
            propertyName.equalsIgnoreCase("money_win") || propertyName.equalsIgnoreCase("money_lose") || propertyName.equalsIgnoreCase("money_draw") ||
            propertyName.equalsIgnoreCase("countdown") || propertyName.equalsIgnoreCase("countdown_max") || propertyName.equalsIgnoreCase("announcer_range") ||
            propertyName.equalsIgnoreCase("duration") || propertyName.equalsIgnoreCase("sudden_death")
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

            if (propertyName.equalsIgnoreCase("duration")) {
                arena.setDuration(intValue * 1000);
                sender.sendMessage(ChatColor.AQUA + "Set duration of " + arena.getName() + " to " + intValue + " seconds");
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("sudden_death")) {
                arena.setSuddenDeath(intValue * 1000);
                sender.sendMessage(ChatColor.AQUA + "Set sudden death time of " + arena.getName() + " to " + intValue + " seconds before end");
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("countdown")) {
                arena.setCountdown(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set countdown of " + arena.getName() + " to " + intValue + " seconds");
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("countdown_max")) {
                arena.setCountdownMax(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set max countdown of " + arena.getName() + " to " + intValue + " seconds");
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("max_teleport_distance")) {
                arena.setMaxTeleportDistance(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set max teleport distance of " + arena.getName() + " to " + intValue);
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("announcer_range")) {
                arena.setAnnouncerRange(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set announcer range of " + arena.getName() + " to " + intValue);
                controller.save();
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

            if (propertyName.equalsIgnoreCase("sp_win")) {
                arena.setWinSP(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set winning SP of " + arena.getName() + " to " + intValue);
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("sp_lose")) {
                arena.setLoseSP(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set losing SP of " + arena.getName() + " to " + intValue);
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("sp_draw")) {
                arena.setDrawSP(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set draw SP of " + arena.getName() + " to " + intValue);
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("money_win")) {
                arena.setWinMoney(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set winning money of " + arena.getName() + " to " + intValue);
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("money_lose")) {
                arena.setLoseMoney(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set losing money of " + arena.getName() + " to " + intValue);
                controller.save();
                return;
            }

            if (propertyName.equalsIgnoreCase("money_draw")) {
                arena.setDrawMoney(intValue);
                sender.sendMessage(ChatColor.AQUA + "Set draw money of " + arena.getName() + " to " + intValue);
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