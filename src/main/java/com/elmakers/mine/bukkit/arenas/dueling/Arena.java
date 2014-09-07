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
    private int deathCount = 0;

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
    private int requiredKills = 1;

    private int portalDamage;
    private int portalEnterDamage;
    private String portalDeathMessage;

    private ArenaType arenaType;
    private final String key;
    private String name;
    private String description;

    public Arena(final String key, final ArenaController controller) {
        this.key = key;
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
                String[] pieces = StringUtils.split((String) o, ',');
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
        name = configuration.getString("name", null);
        description = configuration.getString("description", null);
        minPlayers = configuration.getInt("minplayers");
        maxPlayers = configuration.getInt("maxplayers");
        requiredKills = configuration.getInt("required_kills", 1);

        portalDamage = configuration.getInt("portal_damage", 0);
        portalEnterDamage = configuration.getInt("portal_enter_damage", 0);
        portalDeathMessage = configuration.getString("portal_death_message");

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
        configuration.set("name", name);
        configuration.set("description", description);
        configuration.set("minplayers", minPlayers);
        configuration.set("maxplayers", maxPlayers);
        configuration.set("required_kills", requiredKills);

        configuration.set("portal_damage", portalDamage);
        configuration.set("portal_enter_damage", portalEnterDamage);
        configuration.set("portal_death_message", portalDeathMessage);

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
        deathCount = 0;
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
            player.sendMessage(ChatColor.GOLD + "BEGIN!");

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

        messageNextRoundPlayerList(ChatColor.GOLD + "You are up for the next round!");
    }

    protected void messageNextRoundPlayerList(String message) {
        Collection<String> nextUpPlayers = getNextRoundPlayers();
        for (String messagePlayer : nextUpPlayers) {
            Player player = Bukkit.getPlayer(messagePlayer);
            if (player != null) {
                player.sendMessage(message);
                for (String otherPlayerName : nextUpPlayers) {
                    if (!otherPlayerName.equals(messagePlayer)) {
                        Player otherPlayer = Bukkit.getPlayer(otherPlayerName);
                        if (otherPlayer != null) {
                            int winCount = get(otherPlayer, "won");
                            int lostCount = get(otherPlayer, "lost");

                            player.sendMessage(ChatColor.YELLOW + " with " + ChatColor.DARK_AQUA + otherPlayer.getDisplayName() + ChatColor.WHITE + " ("
                                + ChatColor.GREEN + winCount + " W " + ChatColor.WHITE + " / " + ChatColor.RED + lostCount + " L" + ChatColor.WHITE + ")");
                        }
                    }
                }
            }
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

    public boolean isReady() {
        return state == ArenaState.LOBBY && queue.size() >= minPlayers;
    }

    public void lobbyMessage() {
        int playerCount = queue.size();
        if (playerCount < minPlayers) {
            int playersRemaining = minPlayers - playerCount;
            String playerDescription = playersRemaining == 1 ? "1 more player" : (playersRemaining + " more players");
            messageNextRoundPlayers(ChatColor.AQUA + "Waiting for " + playerDescription);
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

    public void messageNextRoundPlayers(String message) {
        messagePlayers(message, getNextRoundPlayers());
    }

    public void startCountdown(int time) {
        if (state != ArenaState.LOBBY) return;
        state = ArenaState.COUNTDOWN;
        messageNextRoundPlayerList(ChatColor.YELLOW + "A round of " + getName() + " is about to start!");
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
            messageNextRoundPlayers(ChatColor.DARK_AQUA + "Match is starting in " + ChatColor.AQUA + Integer.toString(time) + ChatColor.DARK_AQUA + " seconds");
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
        messageInGamePlayers(ChatColor.DARK_RED + "This match has been cancelled!");
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

    protected void increment(Player player, String statName) {
        String arenaKey = "arena." + key + "." + statName;
        ConfigurationSection data = controller.getMagic().getMage(player).getData();
        int currentValue = data.getInt(arenaKey, 0);
        data.set(arenaKey, currentValue + 1);
    }

    protected int get(Player player, String statName) {
        String arenaKey = "arena." + key + "." + statName;
        ConfigurationSection data = controller.getMagic().getMage(player).getData();
        return data.getInt(arenaKey, 0);
    }

    public void setLoseLocation(Location location) {
        lose = location == null ? null : location.clone();
    }

    public void setExit(Location location) {
        exit = location == null ? null : location.clone();
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

    public ArenaType getType() {
        return arenaType;
    }

    public void setRandomizeSpawn(Vector vector) {
        randomizeSpawn = vector;
    }

    public void check() {
        if (state == ArenaState.COUNTDOWN) {
            if (!isReady()) {
                messagePlayers(ChatColor.RED + " Countdown cancelled");
                state = ArenaState.LOBBY;
                checkStart();
            }
            return;
        }
        Server server = controller.getPlugin().getServer();
        final Player winner = getWinner();
        if (winner != null) {
            final boolean won = deathCount >= requiredKills;
            double health = winner.getHealth() / 2;
            int hearts = (int)Math.floor(health);
            String heartDescription = Integer.toString(hearts);
            health = health - hearts;
            if (health >= 0.5) {
                heartDescription = heartDescription + " 1/2";
            }
            if (won) {
                increment(winner, "won");
                winner.sendMessage(ChatColor.AQUA + "You have won! Congratulations!");
                int winCount = get(winner, "won");
                int lostCount = get(winner, "lost");
                server.broadcastMessage(ChatColor.GOLD + winner.getDisplayName() + " is the champion of " + ChatColor.YELLOW + getName()
                       + ChatColor.GOLD + " with " + ChatColor.DARK_RED + heartDescription + ChatColor.GOLD
                       + " hearts, and a total of " + ChatColor.GREEN + Integer.toString(winCount) + ChatColor.GOLD + "wins and "
                       + ChatColor.RED + Integer.toString(lostCount) + ChatColor.GOLD + " losses.");
            } else {
                server.broadcastMessage(ChatColor.RED + "The " + ChatColor.YELLOW + getName() + ChatColor.RED + " match ended in a default");
            }
            Bukkit.getScheduler().runTaskLater(controller.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    if (won) {
                        winner.sendMessage(ChatColor.YELLOW + "Enjoy the treasure!");
                        winner.teleport(getWinLocation());
                    } else {
                        winner.teleport(getExit());
                    }
                    winner.setHealth(20.0);
                    winner.setFoodLevel(20);
                    winner.setFireTicks(0);
                    remove(winner);
                    finish();
                }
            }, 5 * 20);
        } else if (state == ArenaState.ACTIVE && players.size() == 0) {
            state = ArenaState.LOBBY;
            server.broadcastMessage(ChatColor.RED + "The " + ChatColor.YELLOW + getName() + ChatColor.RED + " match ended in a default");
        }
    }

    public void remove() {
        messagePlayers(ChatColor.RED + "This arena has been removed");
        stop();
        clearQueue();
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public String getName() {
        return name == null ? key : name;
    }

    public String getKey() {
        return key;
    }

    public void join(Player player) {
        Arena currentArena = controller.getArena(player);
        if (currentArena != null) {
            if (currentArena == this) {
                player.sendMessage(ChatColor.RED + "You are already in " + ChatColor.AQUA + currentArena.getName());
                return;
            } else {
                controller.leave(player);
            }
        }

        if (isFull()) {
            player.sendMessage(ChatColor.GOLD + "You have joined the queue for the next round of " + ChatColor.AQUA + getName());
        } else {
            player.sendMessage(ChatColor.YELLOW + "You have entered the current round of " + ChatColor.AQUA + getName());
        }
        if (description != null) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + getDescription());
        }
        increment(player, "joined");
        add(player);

        int winCount = get(player, "won");
        int lostCount = get(player, "lost");

        if (winCount == 0 && lostCount == 0) {
            Bukkit.broadcastMessage(ChatColor.AQUA + player.getDisplayName() + ChatColor.DARK_AQUA + " has joined " + ChatColor.AQUA + getName() + ChatColor.DARK_AQUA + " for the first time");
        } else {
            Bukkit.broadcastMessage(ChatColor.AQUA + player.getDisplayName() + ChatColor.DARK_AQUA + " has joined " + ChatColor.AQUA + getName());
            Bukkit.broadcastMessage(ChatColor.DARK_AQUA + " with " + ChatColor.GREEN + Integer.toString(winCount) + ChatColor.DARK_AQUA + " wins and "
            + ChatColor.RED + Integer.toString(lostCount) + ChatColor.DARK_AQUA + " losses.");
        }
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

    protected Collection<String> getNextRoundPlayers() {
        List<String> allPlayers = new ArrayList<String>();
        for (String queuedPlayer : queue) {
            if (allPlayers.size() >= maxPlayers) break;
            allPlayers.add(queuedPlayer);
        }
        return allPlayers;
    }

    public void describe(CommandSender sender) {
        if (name == null) {
            sender.sendMessage(ChatColor.DARK_AQUA + getName());
        } else {
            sender.sendMessage(ChatColor.DARK_AQUA + getName() + ChatColor.GRAY + " (" + getKey() + ")");
        }
        if (description != null) {
            sender.sendMessage(ChatColor.LIGHT_PURPLE + getDescription());
        }
        sender.sendMessage(ChatColor.AQUA + "State: " + ChatColor.DARK_AQUA + state);
        int inGamePlayers = getInGamePlayers();
        sender.sendMessage(ChatColor.AQUA + "Active Players: " + ChatColor.DARK_AQUA + inGamePlayers);
        for (String playerName : players) {
            sender.sendMessage(ChatColor.GOLD + " " + playerName);
        }
        int queuedPlayers = getQueuedPlayers();
        sender.sendMessage(ChatColor.AQUA + "Queued Players: " + ChatColor.DARK_AQUA + queuedPlayers);
        for (String playerName : queue) {
            sender.sendMessage(ChatColor.YELLOW + " " + playerName);
        }
        int minPlayers = getMinPlayers();
        int maxPlayers = getMaxPlayers();
        sender.sendMessage(ChatColor.AQUA + "Min / Max: " + ChatColor.DARK_AQUA + minPlayers +
                ChatColor.WHITE + " / " + ChatColor.DARK_AQUA + maxPlayers);
        sender.sendMessage(ChatColor.AQUA + "Required Kills: " + ChatColor.DARK_AQUA + requiredKills);

        int spawnSize = spawns.size();
        if (spawnSize == 1) {
            sender.sendMessage(ChatColor.AQUA + "Spawn: " + ChatColor.DARK_AQUA + printLocation(spawns.get(0)));
        } else {
            sender.sendMessage(ChatColor.AQUA + "Spawns: " + ChatColor.DARK_AQUA + spawnSize);
            for (Location spawn : spawns) {
                sender.sendMessage(ChatColor.DARK_AQUA + " " + printLocation(spawn));
            }
        }
        sender.sendMessage(ChatColor.AQUA + "Lobby: " + ChatColor.DARK_AQUA + printLocation(lobby));
        sender.sendMessage(ChatColor.AQUA + "Win: " + ChatColor.DARK_AQUA + printLocation(win));
        sender.sendMessage(ChatColor.AQUA + "Lose: " + ChatColor.DARK_AQUA + printLocation(lose));
        sender.sendMessage(ChatColor.AQUA + "Exit: " + ChatColor.DARK_AQUA + printLocation(exit));
        sender.sendMessage(ChatColor.AQUA + "Center: " + ChatColor.DARK_AQUA + printLocation(center));
        if (randomizeSpawn != null) {
            sender.sendMessage(ChatColor.AQUA + "Randomize: " + ChatColor.DARK_AQUA + randomizeSpawn);
        }
        if (portalDamage > 0 || portalEnterDamage > 0) {
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Portal Entry Damage: " + ChatColor.DARK_PURPLE + portalEnterDamage);
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Portal Damage: " + ChatColor.DARK_PURPLE + portalDamage);
            if (portalDeathMessage != null && !portalDeathMessage.isEmpty()) {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "Portal Death Message: " + ChatColor.DARK_PURPLE + portalDeathMessage);
            }
        }
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

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPortalEnterDamage(int damage) {
        this.portalEnterDamage = damage;
    }

    public int getPortalEnterDamage() {
        return portalEnterDamage;
    }

    public void setPortalDamage(int damage) {
        this.portalDamage = damage;
    }

    public int getPortalDamage() {
        return portalDamage;
    }

    public String getPortalDeathMessage() {
        return portalDeathMessage;
    }

    public void setPortalDeathMessage(String message) {
        this.portalDeathMessage = message;
    }

    public void died(Player player) {
        deathCount++;
        increment(player, "lost");
        remove(player);
        Location specroom = getLoseLocation();
        player.setMetadata("respawnLocation", new FixedMetadataValue(controller.getPlugin(), specroom));
        player.sendMessage(ChatColor.AQUA + "You have lost - Better luck next time!");
        check();
    }
}
