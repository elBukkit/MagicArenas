package com.elmakers.mine.bukkit.arenas.dueling;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArenaCommandExecutor implements TabExecutor {
    private final static String[] SUB_COMMANDS = {
            "start", "stop", "add", "remove", "configure", "describe", "join", "leave"
    };

    private final ArenaController controller;

    public ArenaCommandExecutor(ArenaController controller) {
        this.controller = controller;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String completeCommand = args.length > 0 ? args[args.length - 1] : "";

        completeCommand = completeCommand.toLowerCase();
        List<String> allOptions = Arrays.asList(SUB_COMMANDS);
        List<String> options = new ArrayList<String>();
        for (String option : allOptions) {
            String lowercase = option.toLowerCase();
            if (lowercase.startsWith(completeCommand)) {
                options.add(option);
            }
        }

        return options;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("dueling")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.YELLOW + "---------" +  ChatColor.WHITE  + "Help: HP Dueling " + ChatColor.YELLOW +   "--------");
                sender.sendMessage(ChatColor.GOLD + "Player Commands:\n" + ChatColor.WHITE + " /dueling info\n /dueling help");
                if (sender.hasPermission("dueling.admin")) {
                    sender.sendMessage( "/dueling admin help");
                }
                sender.sendMessage(ChatColor.YELLOW + "-----------------------------------------------");
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("info")) {
                    sender.sendMessage(ChatColor.BLUE + "-----------------");
                    sender.sendMessage(ChatColor.GOLD + "Plugin Creator: KmanCrazy");
                    sender.sendMessage(ChatColor.GOLD + "Plugin Website: kplugins.weebly.com");
                    sender.sendMessage(ChatColor.GOLD + "Hope you enjoy!");
                    sender.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "Report any bugs or glitches to KmanCrazy, Dr00bles, NathanWolf! -Thanks!");
                    sender.sendMessage(ChatColor.BLUE + "-----------------");
                }
                if (args[0].equalsIgnoreCase("help")) {
                    sender.sendMessage(ChatColor.YELLOW + "---------" +  ChatColor.WHITE  + "Help: HP Dueling " + ChatColor.YELLOW +   "--------");
                    sender.sendMessage(ChatColor.GOLD + "Player Commands\n" + ChatColor.WHITE + " /dueling info\n /dueling help");
                    if (sender.hasPermission("dueling.admin")) {
                        sender.sendMessage( "/dueling admin help");
                    }
                    sender.sendMessage(ChatColor.YELLOW + "-----------------------------------------------");
                }
                if (args[0].equalsIgnoreCase("admin")) {
                    if (sender.hasPermission("dueling.admin")) {
                        sender.sendMessage(ChatColor.RED + "Please specify something to do! Create, Remove, Start, End");
                    }
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("admin") && (sender instanceof ConsoleCommandSender || sender.hasPermission("dueling.admin"))) {
                    if (args[1].equalsIgnoreCase("load")) {
                        controller.load();
                        sender.sendMessage("Configuration reloaded");
                    }
                    if (args[1].equalsIgnoreCase("create")) {
                        sender.sendMessage("You must specify an arena! (Optional: ArenaType) (Normal ArenaType: FFA)");
                    }
                    if (args[1].equalsIgnoreCase("join")) {
                        sender.sendMessage("You must specify a name and a player!");
                    }
                    if (args[1].equalsIgnoreCase("start")) {
                        sender.sendMessage("You must specify an arena!");
                    }
                    if (args[1].equalsIgnoreCase("remove")) {
                        sender.sendMessage("You must specify an arena!");
                    }
                    if (args[1].equalsIgnoreCase("help")){
                        sender.sendMessage(ChatColor.YELLOW + "---------" +  ChatColor.WHITE  + "Help: HP Dueling " + ChatColor.YELLOW +   "--------");
                        sender.sendMessage(ChatColor.GOLD + "Player Commands:\n" + ChatColor.WHITE + " /dueling info\n /dueling help");
                        sender.sendMessage(ChatColor.GOLD + "Admin Commands:\n" + ChatColor.WHITE + " /dueling admin help\n /dueling admin create <Arena Name> <Type>\n/dueling admin remove <Arena Name>\n /dueling admin join <Arena Name> <Player>\n dueling admin start <Arena Name>");
                        sender.sendMessage(ChatColor.GOLD + "Types:" + ChatColor.WHITE + " FFA, Spleef, OneVOne, TwoVTwo, ThreeVThree, FourVFour");
                        sender.sendMessage(ChatColor.GOLD + "Options:" + ChatColor.WHITE + " SetLobby, SetSpec, SetTreasureRoom, SetType, SetMinPlayers, SetMaxPlayers, AddSpawn, RemoveSpawn");
                        sender.sendMessage(ChatColor.YELLOW + "-----------------------------------------------");
                    }
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("admin")) {
                    if (args[1].equalsIgnoreCase("create")) {
                        if (sender instanceof Player) {
                            Player p = (Player) sender;
                            p.sendMessage(ChatColor.RED + "Please specify an arena type!");

                        } else {
                            sender.sendMessage(ChatColor.RED + "Silly console! Creating arenas are for players!");
                        }
                    }
                    if (args[1].equalsIgnoreCase("join")) {
                        sender.sendMessage(ChatColor.AQUA + "You must specify an arena!");
                    }

                    if (args[1].equalsIgnoreCase("start")) {
                        String arenaName = args[2];
                        final Arena arena = controller.getArena(arenaName);

                        if (arena != null) {
                            if (arena.isReady()) {
                                arena.startCountdown(10);
                            } else {
                                sender.sendMessage(ChatColor.AQUA + "Not enough players!");
                            }
                        }
                    }
                    if (args[1].equalsIgnoreCase("remove")) {
                        controller.remove(args[2]);
                        controller.save();
                    }
                    if (args[1].equalsIgnoreCase("stop")) {
                        Arena arena = controller.getArena(args[2]);
                        if (arena != null) {
                            if (arena.stop()) {
                                sender.sendMessage(ChatColor.AQUA + "Match stopped!");
                            } else {
                                sender.sendMessage(ChatColor.AQUA + "Arena not active");
                            }
                        } else {
                            sender.sendMessage(ChatColor.AQUA + "Unknown arena");
                        }
                    }
                }
            } else if (args.length == 4) {
                if (args[0].equalsIgnoreCase("admin")) {
                    if (sender.hasPermission("dueling.admin")) {
                        if (args[1].equalsIgnoreCase("join")) {
                            Arena arena = controller.getArena(args[2]);
                            if (arena != null) {
                                Player player = Bukkit.getPlayer(args[3]);
                                if (player != null) {
                                    if (!arena.has(player)) {
                                        if (!arena.isStarted()) {
                                            if (!arena.isFull()) {
                                                arena.add(player);
                                                Bukkit.broadcastMessage(ChatColor.AQUA + args[3] + " has joined the queue for " + args[2]);
                                                player.sendMessage(ChatColor.AQUA + "You have joined the game!");
                                                player.setHealth(20.0);
                                                player.setFoodLevel(20);
                                                player.setFireTicks(0);
                                                for (PotionEffect pt : player.getActivePotionEffects()) {
                                                    player.removePotionEffect(pt.getType());
                                                }
                                                final String ar = args[2];
                                                if (arena.isReady()) {
                                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dueling admin start " + ar);
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
                                } else {
                                    sender.sendMessage(ChatColor.AQUA + "Unknown player!");
                                }
                            } else {
                                sender.sendMessage(ChatColor.AQUA + "Unknown arena!");
                            }
                        } else if (args[1].equalsIgnoreCase("create")) {
                            if (sender instanceof Player) {
                                Player player = (Player) sender;
                                Location location = player.getLocation();
                                String arenaName = args[2];
                                Arena arena = controller.getArena(arenaName);
                                if (ArenaType.valueOf(args[3].toUpperCase()) != null) {
                                    if (arena == null) {
                                        arena = controller.addArena(arenaName, location, 2, 20,args[3].toUpperCase());
                                        controller.save();
                                        player.sendMessage(ChatColor.AQUA + "Arena Created now do /options setlobby, setspawn, setspec, settreasureroom!");
                                    } else {
                                        sender.sendMessage(ChatColor.AQUA + "Arena already exists!");
                                    }
                                } else {
                                    player.sendMessage(ChatColor.RED + "Unknown arena type please select one of the following: Spleef, FFA, 1v1, 2v2, 3v3");
                                }
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "Silly console! Creating arenas are for players!");
                        }
                    }
                }
            }
        }
        if (command.getName().equalsIgnoreCase("options")) {
            if (sender.hasPermission("dueling.options")) {
                if (sender instanceof Player) {

                    Player p = (Player) sender;
                    Location location = p.getLocation();
                    if (args.length == 0) {
                        sender.sendMessage(ChatColor.AQUA + "Please specify a setting and arena name!");
                    } else if (args.length == 1) {
                        sender.sendMessage(ChatColor.AQUA + "Please specify a arena name!");
                    } else if (args.length == 2) {
                        Arena arena = controller.getArena(args[1]);
                        if (arena != null) {
                            if (args[0].equalsIgnoreCase("setlobby")) {
                                arena.setLobby(location);
                                controller.save();
                                p.sendMessage(ChatColor.AQUA + "You have set the lobby!");
                            } else if (args[0].equalsIgnoreCase("addspawn")) {
                                arena.addSpawn(location);
                                controller.save();
                                p.sendMessage(ChatColor.AQUA + "You have added a spawn location!");
                            }  else if (args[0].equalsIgnoreCase("removespawn")) {
                                arena.removeSpawn(location);
                                controller.save();
                                p.sendMessage(ChatColor.AQUA + "You have removed a spawn location!");
                            } else if (args[0].equalsIgnoreCase("setspec")) {
                                arena.setSpectatingRoom(location);
                                controller.save();
                                p.sendMessage(ChatColor.AQUA + "You have set the spectating room!");
                            } else if (args[0].equalsIgnoreCase("settreasureroom")) {
                                arena.setTreasureRoom(location);
                                controller.save();
                                p.sendMessage(ChatColor.AQUA + "You have set the treasure room!");
                            } else if (args[0].equalsIgnoreCase("setminplayers")){
                                p.sendMessage(ChatColor.DARK_RED + "You must specify a number of minimum players!");
                            }
                            else if (args[0].equalsIgnoreCase("setmaxplayers")) {
                                p.sendMessage(ChatColor.DARK_RED + "You must specify a number of maximum players!");
                            } else if (args[0].equalsIgnoreCase("settype")) {
                                p.sendMessage(ChatColor.DARK_RED + "You must specify an arena type!");
                            } else if (args[0].equalsIgnoreCase("setrandomize")) {
                                p.sendMessage(ChatColor.DARK_RED + "You must specify a vector x,y,z");
                            } else {
                                sender.sendMessage(ChatColor.DARK_RED + "Unknown option!");
                            }
                        }
                    } else if (args.length == 3) {
                        Arena arena = controller.getArena(args[1]);
                        if (arena != null) {
                            if (args[0].equalsIgnoreCase("setminplayers")) {
                                arena.setMinPlayers(Integer.parseInt(args[2]));
                                sender.sendMessage(ChatColor.AQUA + "Set min players");
                                controller.save();
                            } else if (args[0].equalsIgnoreCase("setmaxplayers")) {
                                arena.setMaxPlayers(Integer.parseInt(args[2]));
                                sender.sendMessage(ChatColor.AQUA + "Set max players");
                                controller.save();
                            } else if (args[0].equalsIgnoreCase("settype")){
                                if (ArenaType.valueOf(args[2].toUpperCase() )!= null){
                                    arena.setType(ArenaType.valueOf(args[2]));
                                    sender.sendMessage(ChatColor.AQUA + "Set arena type");
                                    controller.save();
                                } else{
                                    p.sendMessage(ChatColor.RED + "Unknown ArenaType!");
                                }
                            } else if (args[0].equalsIgnoreCase("setrandomize")) {
                                if (args[2].isEmpty()) {
                                    p.sendMessage(ChatColor.RED + "Cleared randomized spawn!");
                                    arena.setRandomizeSpawn(null);
                                } else {
                                    sender.sendMessage(ChatColor.AQUA + "Set randomized spawn");
                                    arena.setRandomizeSpawn(Arena.toVector(args[2]));
                                }
                            }
                        } else{
                            p.sendMessage(ChatColor.RED + "Unknown arena!");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Invalid parameters!");
                    }
                }
            } else {
                sender.sendMessage(ChatColor.AQUA + "You don't have permissions!");
            }
        }
        return true;
    }
}