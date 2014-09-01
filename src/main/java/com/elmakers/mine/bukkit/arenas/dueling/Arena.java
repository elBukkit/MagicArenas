package com.elmakers.mine.bukkit.arenas.dueling;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Arena {
    private static Random random = new Random();

    private ArenaState state = ArenaState.LOBBY;
    private Set<String> players = new HashSet<String>();
    private List<Location> spawns = new ArrayList<Location>();
    private final ArenaController controller;

    private Location center;
    private Location exit;
    private Location lose;
    private Location win;
    private Location lobby;

    private Vector randomizeSpawn;

    private int maxPlayers;
    private int minPlayers;

    private ArenaType arenaType;
    private final String name;

    public Arena(final String name, final ArenaController controller) {
        this.name = name;
        this.controller = controller;
    }

    public Arena(final String name, final ArenaController plugin, Location location, int min, int max, ArenaType type) {
        this(name, plugin);
        center = location.clone();

        maxPlayers = max;
        minPlayers = min;

        arenaType = type;
    }

    public static String fromVector(Vector vector) {
        if (vector == null) return "";
        return vector.getX() + "," + vector.getY() + "," + vector.getZ();
    }

    public static Vector toVector(Object o) {
        if (o instanceof Vector) {
            return (Vector)o;
        }
        if (o instanceof String) {
            try {
                String[] pieces = StringUtils.split((String)o, ',');
                double x = Double.parseDouble(pieces[0]);
                double y = Double.parseDouble(pieces[1]);
                double z = Double.parseDouble(pieces[2]);
                return new Vector(x, y, z);
            } catch(Exception ex) {
                return null;
            }
        }
        return null;
    }

    public String fromLocation(Location location) {
        if (location == null) return "";
        if (location.getWorld() == null) return "";
        return location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getWorld().getName()
                + "," + location.getYaw() + "," + location.getPitch();
    }

    public Location toLocation(Object o) {
        if (o instanceof Location) {
            return (Location)o;
        }
        if (o instanceof String) {
            try {
                float pitch = 0;
                float yaw = 0;
                String[] pieces = StringUtils.split((String)o, ',');
                double x = Double.parseDouble(pieces[0]);
                double y = Double.parseDouble(pieces[1]);
                double z = Double.parseDouble(pieces[2]);
                World world = null;
                if (pieces.length > 3) {
                    world = Bukkit.getWorld(pieces[3]);
                } else {
                    world = Bukkit.getWorlds().get(0);
                }
                if (pieces.length > 5) {
                    yaw = Float.parseFloat(pieces[4]);
                    pitch = Float.parseFloat(pieces[5]);
                }
                return new Location(world, x, y, z, yaw, pitch);
            } catch(Exception ex) {
                return null;
            }
        }
        return null;
    }

    public void load(ConfigurationSection configuration) {
        minPlayers = configuration.getInt("minplayers");
        maxPlayers = configuration.getInt("maxplayers");

        arenaType = ArenaType.parse(configuration.getString("type"));
        if (arenaType == null) {
            arenaType = ArenaType.FFA;
        }

        lose = toLocation(configuration.getString("lose"));
        win = toLocation(configuration.getString("win"));
        lobby = toLocation(configuration.getString("lobby"));
        center = toLocation(configuration.getString("center"));
        exit = toLocation(configuration.getString("exit"));

        for (String s : configuration.getStringList("spawns")){
            spawns.add(toLocation(s));
        }
;
        if (configuration.contains("randomize.spawn")) {
            randomizeSpawn = toVector(configuration.getString("randomize.spawn"));
        }
    }

    public void save(ConfigurationSection configuration) {
        configuration.set("minplayers", minPlayers);
        configuration.set("maxplayers", maxPlayers);

        configuration.set("type", arenaType.name());

        configuration.set("lobby", fromLocation(lobby));
        configuration.set("win", fromLocation(win));
        configuration.set("lose", fromLocation(lose));
        configuration.set("center", fromLocation(center));
        configuration.set("exit", fromLocation(exit));

        List<String> spawnList = new ArrayList<String>();
        for (Location spawn : spawns) {
            spawnList.add(fromLocation(spawn));
        }
        configuration.set("spawns", spawnList);

        if (randomizeSpawn != null) {
            configuration.set("randomize.spawn", fromVector(randomizeSpawn));
        }
    }

    public void start() {
        state = ArenaState.ACTIVE;
        Server server = controller.getPlugin().getServer();
        int num = 0;
        List<Location> spawns = getSpawns();
        for (String playerName : players) {
            Player player = server.getPlayer(playerName);
            player.sendMessage("Begin!");

            Location spawn = spawns.get(num);
            if (randomizeSpawn != null) {
                spawn = spawn.clone();
                spawn.add
                (
                    (2 * random.nextDouble() - 1) * randomizeSpawn.getX(),
                    (2 * random.nextDouble() - 1) * randomizeSpawn.getY(),
                    (2 * random.nextDouble() - 1) * randomizeSpawn.getZ()
                );
            }

            // Wrap index around to player
            num = (num + 1) % spawns.size();
            player.teleport(spawn);
        }
    }

    public boolean has(Player player) {
        return players.contains(player.getName());
    }

    public void remove(Player player) {
        players.remove(player.getName());
        player.removeMetadata("arena", controller.getPlugin());
    }

    public Player getWinner() {
        if (state == ArenaState.ACTIVE && players.size() == 1) {
            state = ArenaState.LOBBY;
            String winner = players.iterator().next();
            players.clear();
            return controller.getPlugin().getServer().getPlayer(winner);
        }

        return null;
    }

    public Location getLobby() {
        return lobby == null ? center : lobby;
    }

    public Location getLoseLocation() {
        return lose == null ? center : lose;
    }

    public Location getWinLocation() {
        return win == null ? center : win;
    }

    public Location getCenter() {
        return center;
    }

    public Location getExit() {
        return exit == null ? center : exit;
    }

    public boolean checkActive() {
        if (state != ArenaState.ACTIVE) return false;
        if (players.size() == 0) {
            state = ArenaState.LOBBY;
            return false;
        }

        return true;
    }

    public boolean isReady() {
        return state == ArenaState.LOBBY && players.size() >= minPlayers;
    }

    public void lobbyMessage() {
        int playerCount = players.size();
        if (playerCount < minPlayers) {
            String message = ChatColor.AQUA + String.valueOf(playerCount) + ChatColor.GOLD + "/" + ChatColor.AQUA + String.valueOf(maxPlayers) + " players.";
            messagePlayers(message);
        }
    }

    public void messagePlayers(String message) {
        Server server = controller.getPlugin().getServer();
        Collection<String> names = new ArrayList<String>(players);
        for (String playerName : names) {
            Player player = server.getPlayer(playerName);
            if (player == null) {
                players.remove(playerName);
            } else {
                player.sendMessage(message);
            }
        }
    }

    public void startCountdown(int time) {
        if (state != ArenaState.LOBBY) return;
        state = ArenaState.COUNTDOWN;
        countdown(time);
    }

    private void countdown(final int time) {
        if (state != ArenaState.COUNTDOWN) {
            return;
        }

        if (time <= 0) {
            start();
            return;
        }

        if (time == 10 || time <= 5) {
            messagePlayers("Match is starting in " + time + " seconds");
        }
        BukkitScheduler scheduler = controller.getPlugin().getServer().getScheduler();
        scheduler.runTaskLater(controller.getPlugin(), new Runnable() {
            @Override
            public void run() {
                countdown(time - 1);
            }
        }, 20);
    }

    public boolean stop() {
        if (state == ArenaState.LOBBY) return false;
        messagePlayers("This match has been cancelled!");
        state = ArenaState.LOBBY;
        players.clear();
        return true;
    }

    public boolean isStarted() {
        return state == ArenaState.ACTIVE;
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public void add(Player player) {
        players.add(player.getName());
        player.teleport(getLobby());
        player.setMetadata("arena", new FixedMetadataValue(controller.getPlugin(), this));
    }

    public void setLoseLocation(Location location) {
        lose = location == null ? null : location.clone();
    }

    public void setExit(Location location) {
        exit = location == null ? null : exit.clone();
    }

    public void setLobby(Location location) {
        lobby = location == null ? null : location.clone();
    }

    public void setWinLocation(Location location) {
        win = location == null ? null : location.clone();
    }

    public void addSpawn(Location location) {
        spawns.add(location.clone());
    }

    public void setSpawn(Location location) {
        spawns.clear();
        if (location != null) {
            addSpawn(location);
        }
    }

    public Location removeSpawn(Location location) {
        Location l = location;
        int range = 3;
        int minX = l.getBlockX() - range / 2;
        int minY = l.getBlockY() - range / 2;
        int minZ = l.getBlockZ() - range / 2;

        for (int x = minX; x < minX + range; x++) {
            for (int y = minY; y < minY + range; y++) {
                for (int z = minZ; z < minZ + range; z++) {
                    Location loc = location.getWorld().getBlockAt(x, y, z).getLocation();
                    if (spawns.contains(loc)) {
                        spawns.remove(loc);
                        return loc;
                    }
                }
            }
        }

        return null;
    }

    public List<Location> getSpawns() {
        if (spawns.size() == 0) {
            List<Location> centerList = new ArrayList<Location>();
            centerList.add(center);
            return centerList;
        }

        return spawns;
    }

    public void setMinPlayers(int players) {
        minPlayers = players;
    }

    public void setMaxPlayers(int players) {
        maxPlayers = players;
    }

    public void setType(ArenaType types){
        switch (types) {
            case FFA:
                setMinPlayers(2);
                setMaxPlayers(20);
                arenaType = types;
                break;
            case FOURVFOUR:
                setMinPlayers(8);
                setMaxPlayers(8);
                arenaType = types;
                break;
            case ONEVONE:
                setMinPlayers(2);
                setMinPlayers(2);
                arenaType = types;
                break;
            case TWOVTWO:
                setMinPlayers(4);
                setMaxPlayers(4);
                arenaType = types;
                break;
            case THREEVTHREE:
                setMinPlayers(6);
                setMaxPlayers(6);
                arenaType = types;
                break;
            case SPLEEF:
                setMinPlayers(5);
                setMaxPlayers(15);
                arenaType = types;
                break;
        }
    }

    public ArenaType getType(){
        return arenaType;
    }

    public void setRandomizeSpawn(Vector vector) {
        randomizeSpawn = vector;
    }

    public void check() {
        final Player winner = getWinner();
        if (winner != null) {
            Server server = controller.getPlugin().getServer();
            winner.sendMessage(ChatColor.AQUA + "You have won! Congratulations!");
            server.broadcastMessage(ChatColor.GOLD + winner.getDisplayName() + " has won a battle with "+ ChatColor.RED +  + winner.getHealth()/2 + " hearts!");
            Bukkit.getScheduler().runTaskLater(controller.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    winner.sendMessage(ChatColor.AQUA + "Enjoy the treasure!");
                    winner.teleport(getWinLocation());
                    winner.setHealth(20.0);
                    winner.setFoodLevel(20);
                    winner.setFireTicks(0);
                }
            }, 5 * 20);
        } else {
            checkActive();
        }
    }

    public void cancel() {
        messagePlayers("This match has been cancelled");
        Server server = controller.getPlugin().getServer();
        for (String playerId : players) {
            Player player = server.getPlayer(playerId);
            if (player != null) {
                player.removeMetadata("arena", controller.getPlugin());
            }
        }
        players.clear();
    }

    public String getName() {
        return name;
    }

    public void join(Player player) {
        if (!has(player)) {
            if (!isStarted()) {
                if (!isFull()) {
                    add(player);
                    Bukkit.broadcastMessage(ChatColor.AQUA + player.getDisplayName() + " has joined the queue for " + name);
                    player.sendMessage(ChatColor.AQUA + "You have joined the game!");
                    player.setHealth(20.0);
                    player.setFoodLevel(20);
                    player.setFireTicks(0);
                    for (PotionEffect pt : player.getActivePotionEffects()) {
                        player.removePotionEffect(pt.getType());
                    }
                    if (isReady()) {
                        startCountdown(10);
                    } else {
                        lobbyMessage();
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "There are too many players! Wait until next round!");
                }
            } else {
                player.sendMessage(ChatColor.RED + "There are too many players! Wait until next round!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Already in game!");
        }
    }
}
