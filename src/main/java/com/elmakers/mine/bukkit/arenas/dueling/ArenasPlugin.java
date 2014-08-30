package com.elmakers.mine.bukkit.arenas.dueling;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class ArenasPlugin extends JavaPlugin implements Listener {
    public Map<String, Arena> arenas = new HashMap<String, Arena>();
    public static Map<String,String> queue = new HashMap<String, String>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        // Make sure not to load configs until all worlds are loaded
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                load();
            }
        }, 2);
    }

    public void save() {
        Configuration configuration = getConfig();
        Collection<String> oldKeys = configuration.getKeys(false);
        for (String oldKey : oldKeys) {
            configuration.set(oldKey, null);
        }
        for (Map.Entry<String, Arena> entry : arenas.entrySet()) {
            ConfigurationSection arenaConfig = configuration.createSection(entry.getKey());
            entry.getValue().save(arenaConfig);
        }
        saveConfig();
    }

    public void load() {
        reloadConfig();
        Configuration configuration = getConfig();
        Collection<String> arenaKeys = configuration.getKeys(false);

        arenas.clear();
        for (String arenaKey : arenaKeys) {
            Arena arena = new Arena(this);
            arena.load(configuration.getConfigurationSection(arenaKey));
            arenas.put(arenaKey, arena);
        }

        Bukkit.getLogger().info("Loaded " + arenas.size() + " arenas");
    }
    public void spawnFirework(Player p){
        Firework firework = (Firework) p.getWorld().spawnEntity(p.getLocation(),EntityType.FIREWORK);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        fireworkMeta.setPower(1);
        FireworkEffect.Builder effect = FireworkEffect.builder();
        Random random = new Random();
        int n  = random.nextInt(4)+1;
        switch (n){
            case 1:
                effect.withColor(Color.BLUE);
                break;
            case 2:
                effect.withColor(Color.GREEN);
                break;
            case 3:
                effect.withColor(Color.RED);
                break;
            case 4:
                effect.withColor(Color.ORANGE);
                break;
        }
        Random rand = new Random();
        int m  = rand.nextInt(4)+1;
        switch (m){
            case 1:
                effect.withFade(Color.BLACK);
                break;
            case 2:
                effect.withFade(Color.PURPLE);
                break;
            case 3:
                effect.withFade(Color.AQUA);
                break;
            case 4:
                effect.withFade(Color.YELLOW);
                break;
        }
        Random ran = new Random();
        int o = ran.nextInt(5)+1;
        switch (o){
            case 1:
                effect.with(FireworkEffect.Type.CREEPER);
                break;
            case 2:
                effect.with(FireworkEffect.Type.BALL_LARGE);
                break;
            case 3:
                effect.with(FireworkEffect.Type.BALL);
                break;
            case 4:
                effect.with(FireworkEffect.Type.BURST);
                break;
            case 5:
                effect.with(FireworkEffect.Type.STAR);
                break;
        }
        fireworkMeta.addEffect(effect.build());
        firework.setFireworkMeta(fireworkMeta);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        if (player.hasMetadata("respawnLocation")) {
            Collection<MetadataValue> metadata = player.getMetadata("respawnLocation");
            for (MetadataValue value : metadata) {
                player.removeMetadata("respawnLocation", value.getOwningPlugin());
                e.setRespawnLocation((Location)value.value());
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        for (Arena arena : arenas.values()) {
            if (arena.has(player)) {
                arena.remove(player);
                Location specroom = arena.getSpectatingRoom();
                player.setMetadata("respawnLocation", new FixedMetadataValue(this, specroom));
                player.sendMessage(ChatColor.AQUA + "You have lost :( Better luck next time!");
                checkArena(arena);
            }
        }
    }

    @Override
    public void onDisable() {
    }

    protected void checkArena(final Arena arena) {
        final Player winner = arena.getWinner();
        if (winner != null) {
            Server server = getServer();
            winner.sendMessage(ChatColor.AQUA + "You have won! Congratulations!");
            spawnFirework(winner);
            server.broadcastMessage(ChatColor.GOLD + winner.getDisplayName() + " has won a battle with "+ ChatColor.RED +  + winner.getHealth()/2 + " hearts!");
            Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                @Override
                public void run() {
                    winner.sendMessage(ChatColor.AQUA + "Enjoy the treasure!");
                    winner.teleport(arena.getTreasureRoom());
                    winner.setHealth(20.0);
                    winner.setFoodLevel(20);
                    winner.setFireTicks(0);
                    for (String s : queue.keySet()){
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dueling admin join " + queue.get(s) + " " + s);
                    }
                }
            }, 5 * 20);
        } else {
            arena.checkActive();
        }
    }

    protected boolean playerLeft(Player player) {
        for (Arena arena : arenas.values()) {
            if (arena.has(player)) {
                player.teleport(arena.getSpectatingRoom());
                arena.remove(player);
                checkArena(arena);
                return true;
            }
        }

        return false;
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent e) {
        Player player = e.getPlayer();
        if (playerLeft(player)) {
            e.setLeaveMessage(ChatColor.AQUA + e.getPlayer().getName() + " was kicked out of the arena!");
        }


    }

    @EventHandler
    public void onSignChange(SignChangeEvent e){
        if (e.getLine(0).contains("Duel")) {
            if (!e.getLine(1).isEmpty()) {
                String arenaName = e.getLine(1);
                Arena arena = arenas.get(arenaName);
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
                for (Arena arenas1 : arenas.values()){
                    if (arenas1.has(e.getPlayer())){
                        arenas1.remove(e.getPlayer());
                    }
                }
            }
        }
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
                        load();
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
                        final Arena arena = arenas.get(arenaName);

                        if (arena != null) {
                            if (arena.isReady()) {
                                arena.startCountdown(10);
                            } else {
                                sender.sendMessage(ChatColor.AQUA + "Not enough players!");
                            }
                        }
                    }
                    if (args[1].equalsIgnoreCase("remove")) {
                        arenas.remove(args[2]);
                        save();
                    }
                    if (args[1].equalsIgnoreCase("stop")) {
                        Arena arena = arenas.get(args[2]);
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
                            Arena arena = arenas.get(args[2]);
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
                                                if (!queue.containsKey(args[3])) {
                                                    Bukkit.getPlayer(args[3]).sendMessage(ChatColor.AQUA + "You were added to the queue!");
                                                    queue.put(player.getName(), args[2]);
                                                }else{
                                                    Bukkit.getPlayer(args[3]).sendMessage(ChatColor.RED + "You are already in the queue!");
                                                }
                                            }
                                        } else {
                                            Bukkit.getPlayer(args[3]).sendMessage(ChatColor.RED + "There are too many players! Wait until next round!");
                                            if (!queue.containsKey(args[3])) {
                                                Bukkit.getPlayer(args[3]).sendMessage(ChatColor.AQUA + "You were added to the queue!");
                                                queue.put(player.getName(), args[2]);
                                            }else{
                                                Bukkit.getPlayer(args[3]).sendMessage(ChatColor.RED + "You are already in the queue!");
                                            }
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
                                if (!arenas.containsKey(arenaName)) {
                                    if (ArenaType.valueOf(args[3].toUpperCase()) != null) {
                                        if (!arenas.containsKey(arenaName)) {
                                            Arena arena = new Arena(this, location, 2, 20,args[3].toUpperCase());
                                            arenas.put(arenaName, arena);
                                            save();
                                            player.sendMessage(ChatColor.AQUA + "Arena Created now do /options setlobby, setspawn, setspec, settreasureroom!");
                                        } else {
                                            sender.sendMessage(ChatColor.AQUA + "Arena already exists!");
                                        }
                                    } else {
                                        player.sendMessage(ChatColor.RED + "Unknown arena type please select one of the following: Spleef, FFA, 1v1, 2v2, 3v3");
                                    }
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
                        Arena arena = arenas.get(args[1]);
                        if (arena != null) {
                            if (args[0].equalsIgnoreCase("setlobby")) {
                                arena.setLobby(location);
                                save();
                                p.sendMessage(ChatColor.AQUA + "You have set the lobby!");
                            } else if (args[0].equalsIgnoreCase("addspawn")) {
                                arena.addSpawn(location);
                                save();
                                p.sendMessage(ChatColor.AQUA + "You have added a spawn location!");
                            }  else if (args[0].equalsIgnoreCase("removespawn")) {
                                arena.removeSpawn(location);
                                save();
                                p.sendMessage(ChatColor.AQUA + "You have removed a spawn location!");
                            } else if (args[0].equalsIgnoreCase("setspec")) {
                                arena.setSpectatingRoom(location);
                                save();
                                p.sendMessage(ChatColor.AQUA + "You have set the spectating room!");
                            } else if (args[0].equalsIgnoreCase("settreasureroom")) {
                                arena.setTreasureRoom(location);
                                save();
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
                        Arena arena = arenas.get(args[1]);
                        if (arena != null) {
                            if (args[0].equalsIgnoreCase("setminplayers")) {
                                arena.setMinPlayers(Integer.parseInt(args[2]));
                                sender.sendMessage(ChatColor.AQUA + "Set min players");
                                save();
                            } else if (args[0].equalsIgnoreCase("setmaxplayers")) {
                                arena.setMaxPlayers(Integer.parseInt(args[2]));
                                sender.sendMessage(ChatColor.AQUA + "Set max players");
                                save();
                            } else if (args[0].equalsIgnoreCase("settype")){
                                if (ArenaType.valueOf(args[2].toUpperCase() )!= null){
                                    arena.setType(ArenaType.valueOf(args[2]));
                                    sender.sendMessage(ChatColor.AQUA + "Set arena type");
                                    save();
                                }else{
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
        return false;
    }
}