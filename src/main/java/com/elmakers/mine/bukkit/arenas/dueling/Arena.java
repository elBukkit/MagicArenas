package com.elmakers.mine.bukkit.arenas.dueling;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

public class Arena {
    private static Random random = new Random();

    private ArenaState state = ArenaState.LOBBY;
    private Queue<String> queue = new LinkedList<String>();
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

        if (configuration.contains("randomize.spawn")) {
            randomizeSpawn = toVector(configuration.getString("randomize.spawn"));
        }

        // Legacy backup check
        if (center == null) {
            center = lose;
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
        clearPlayers();
        while (queue.size() > 0 && players.size() < maxPlayers) {
            players.add(queue.remove());
        }
        List<Location> spawns = getSpawns();
        for (String playerName : players) {
            Player player = server.getPlayer(playerName);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setFireTicks(0);
            for (PotionEffect pt : player.getActivePotionEffects()) {
                player.removePotionEffect(pt.getType());
            }
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

    public void remove(Player player) {
        players.remove(player.getName());
        queue.remove(player.getName());
        player.removeMetadata("arena", controller.getPlugin());
    }

    public Player getWinner() {
        if (state == ArenaState.ACTIVE && players.size() == 1) {
            String winner = players.iterator().next();
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
        return state == ArenaState.LOBBY && queue.size() >= minPlayers;
    }

    public void lobbyMessage() {
        int playerCount = queue.size();
        if (playerCount < minPlayers) {
            String message = ChatColor.AQUA + String.valueOf(playerCount) + ChatColor.GOLD + "/" + ChatColor.AQUA + String.valueOf(minPlayers) + " players.";
            messagePlayers(message);
        }
    }

    protected void messagePlayers(String message, Collection<String> playerNames) {
        Server server = controller.getPlugin().getServer();
        for (String playerName : playerNames) {
            Player player = server.getPlayer(playerName);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    public void messagePlayers(String message) {
        messagePlayers(message, getAllPlayers());
    }

    public void messageInGamePlayers(String message) {
        messagePlayers(message, players);
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

        if (time % 10 == 0 || time <= 5) {
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
        messageInGamePlayers("This match has been cancelled!");
        finish();
        return true;
    }

    protected void finish() {
        state = ArenaState.LOBBY;
        clearPlayers();

        // Check for a new start
        checkStart();
    }

    protected void clearPlayers() {
        Server server = controller.getPlugin().getServer();
        for (String playerName : players) {
            Player player = server.getPlayer(playerName);
            if (player != null) {
                player.teleport(getExit());
                player.removeMetadata("arena", controller.getPlugin());
            }
        }
        players.clear();
    }

    protected void clearQueue() {
        Server server = controller.getPlugin().getServer();
        for (String playerName : queue) {
            Player player = server.getPlayer(playerName);
            if (player != null) {
                player.teleport(getExit());
                player.removeMetadata("arena", controller.getPlugin());
            }
        }
        queue.clear();
    }

    public boolean isStarted() {
        return state == ArenaState.ACTIVE;
    }

    public boolean isFull() {
        return queue.size() >= maxPlayers;
    }

    public void add(Player player) {
        queue.add(player.getName());
        player.teleport(getLobby());
        player.setMetadata("arena", new FixedMetadataValue(controller.getPlugin(), this));
    }

    public void setLoseLocation(Location location) {
        lose = location == null ? null : location.clone();
    }

    public void setExit(Location location) {
        exit = location == null ? null : exit.clone();
    }

    public void setCenter(Location location) {
        center = location.clone();
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
        int rangeSquared = 3 * 3;
        for (Location spawn : spawns) {
            if (spawn.distanceSquared(location) < rangeSquared) {
                spawns.remove(spawn);
                return spawn;
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
            double health = winner.getHealth() / 2;
            int hearts = (int)Math.floor(health);
            String heartDescription = Integer.toString(hearts);
            health = health - hearts;
            if (health >= 0.5) {
                heartDescription = heartDescription + " 1/2";
            }
            server.broadcastMessage(ChatColor.GOLD + winner.getDisplayName() + " has won a battle with "+ ChatColor.RED + heartDescription + " hearts!");
            Bukkit.getScheduler().runTaskLater(controller.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    winner.sendMessage(ChatColor.AQUA + "Enjoy the treasure!");
                    winner.teleport(getWinLocation());
                    winner.setHealth(20.0);
                    winner.setFoodLevel(20);
                    winner.setFireTicks(0);
                    finish();
                }
            }, 5 * 20);
        } else {
            checkActive();
        }
    }

    public void remove() {
        messagePlayers("This arena has been removed");
        stop();
        clearQueue();
    }

    public String getName() {
        return name;
    }

    public void join(Player player) {
        Arena currentArena = controller.getArena(player);
        if (currentArena != null) {
            if (currentArena == this) {
                player.sendMessage(ChatColor.RED + "You are already in " + currentArena.getName());
                return;
            } else {
                controller.leave(player);
            }
        }

        add(player);
        Bukkit.broadcastMessage(ChatColor.AQUA + player.getDisplayName() + " has joined the queue for " + name);
        player.sendMessage(ChatColor.AQUA + "You have joined the game!");
        checkStart();
    }

    protected void checkStart() {
        if (isStarted()) return;

        if (isReady()) {
            if (isFull()) {
                startCountdown(10);
            } else {
                startCountdown(30);
            }
        } else {
            lobbyMessage();
        }
    }

    protected Collection<String> getAllPlayers() {
        List<String> allPlayers = new ArrayList<String>(players);
        allPlayers.addAll(queue);
        return allPlayers;
    }

    public void describe(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_AQUA + getName() + ": ");
        sender.sendMessage(ChatColor.AQUA + "State: " + ChatColor.DARK_AQUA + state);
        int queuedPlayers = getQueuedPlayers();
        int inGamePlayers = getInGamePlayers();
        sender.sendMessage(ChatColor.AQUA + "Active / Queued: " + ChatColor.DARK_AQUA + inGamePlayers +
            ChatColor.WHITE + " / " + ChatColor.DARK_AQUA + queuedPlayers);
        int minPlayers = getMinPlayers();
        int maxPlayers = getMaxPlayers();
        sender.sendMessage(ChatColor.AQUA + "Min / Max: " + ChatColor.DARK_AQUA + minPlayers +
                ChatColor.WHITE + " / " + ChatColor.DARK_AQUA + maxPlayers);

        int spawnSize = spawns.size();
        if (spawnSize == 1) {
            sender.sendMessage(ChatColor.AQUA + "Spawn: " + ChatColor.DARK_AQUA + printLocation(spawns.get(0)));
        } else {
            sender.sendMessage(ChatColor.AQUA + "Spawns: " + ChatColor.DARK_AQUA + spawnSize);
        }
        sender.sendMessage(ChatColor.AQUA + "Lobby: " + ChatColor.DARK_AQUA + printLocation(lobby));
        sender.sendMessage(ChatColor.AQUA + "Win: " + ChatColor.DARK_AQUA + printLocation(win));
        sender.sendMessage(ChatColor.AQUA + "Lose: " + ChatColor.DARK_AQUA + printLocation(lose));
        sender.sendMessage(ChatColor.AQUA + "Exit: " + ChatColor.DARK_AQUA + printLocation(exit));
        sender.sendMessage(ChatColor.AQUA + "Center: " + ChatColor.DARK_AQUA + printLocation(center));
    }

    protected String printLocation(Location location) {
        if (location == null) return ChatColor.DARK_GRAY + "(None)";

        return location.toVector().toString() + " : " + location.getWorld().getName();
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getQueuedPlayers() {
        return queue.size();
    }

    public int getInGamePlayers() {
        return players.size();
    }
}
