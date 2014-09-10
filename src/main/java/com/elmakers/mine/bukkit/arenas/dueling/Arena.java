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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class Arena {
    static class ArenaPlayerComparator implements Comparator<ArenaPlayer>
    {
        public int compare(ArenaPlayer player1, ArenaPlayer player2)
        {
            return ((Float)player2.getWinRatio()).compareTo(player1.getWinRatio());
        }
    }

    private static Random random = new Random();

    private ArenaState state = ArenaState.LOBBY;
    private Queue<ArenaPlayer> queue = new LinkedList<ArenaPlayer>();
    private Collection<ArenaPlayer> players = new ArrayList<ArenaPlayer>();
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

    private int leaderboardSize = 5;
    private int leaderboardGamesRequired = 5;

    private List<ArenaPlayer> leaderboard = new ArrayList<ArenaPlayer>();

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

        leaderboardSize = configuration.getInt("leaderboard_size", 5);
        leaderboardGamesRequired = configuration.getInt("leaderboard_games_required", 5);

        arenaType = ArenaType.parse(configuration.getString("type"));
        if (arenaType == null) {
            arenaType = ArenaType.FFA;
        }
        leaderboard.clear();
        if (configuration.contains("leaderboard")) {
            ConfigurationSection leaders = configuration.getConfigurationSection("leaderboard");
            Collection<String> leaderboardKeys = leaders.getKeys(false);
            for (String key : leaderboardKeys) {
                ConfigurationSection leaderConfig = leaders.getConfigurationSection(key);
                ArenaPlayer loadedPlayer = new ArenaPlayer(this, leaderConfig);
                leaderboard.add(loadedPlayer);
            }
            Collections.sort(leaderboard, new ArenaPlayerComparator());
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

        configuration.set("leaderboard_size", leaderboardSize);
        configuration.set("leaderboard_games_required", leaderboardGamesRequired);

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

        if (leaderboard.size() > 0) {
            ConfigurationSection leaders = configuration.createSection("leaderboard");
            for (ArenaPlayer player : leaderboard) {
                String key = player.getUUID().toString();
                ConfigurationSection playerData = leaders.createSection(key);
                player.save(playerData);
            }
        }
    }

    public void start() {
        deathCount = 0;
        state = ArenaState.ACTIVE;
        Server server = controller.getPlugin().getServer();
        int num = 0;
        clearPlayers();
        while (queue.size() > 0 && players.size() < maxPlayers) {
            ArenaPlayer queuedPlayer = queue.remove();
            if (queuedPlayer.isValid() && !queuedPlayer.isDead()) {
                players.add(queuedPlayer);
            }
        }
        if (players.size() < minPlayers) {
            queue.addAll(players);
            players.clear();
            state = ArenaState.LOBBY;
            messagePlayers(ChatColor.RED + " the match did not have enough players to start.");
            return;
        }
        List<Location> spawns = getSpawns();
        for (ArenaPlayer arenaPlayer : players) {
            Player player = arenaPlayer.getPlayer();
            if (player == null) {
                continue;
            }

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
        Collection<ArenaPlayer> nextUpPlayers = getNextRoundPlayers();
        for (ArenaPlayer messagePlayer : nextUpPlayers) {
            Player player = messagePlayer.getPlayer();
            if (player != null) {
                String messagePlayerName = messagePlayer.getDisplayName();
                player.sendMessage(message);
                for (ArenaPlayer otherArenaPlayer : nextUpPlayers) {
                    String otherPlayerName = otherArenaPlayer.getDisplayName();
                    if (!otherPlayerName.equals(messagePlayerName)) {
                        Player otherPlayer = otherArenaPlayer.getPlayer();
                        if (otherPlayer != null) {
                            int winCount = otherArenaPlayer.getWins();
                            int lostCount = otherArenaPlayer.getLosses();

                            player.sendMessage(ChatColor.YELLOW + " with " + ChatColor.DARK_AQUA + otherPlayer.getDisplayName() + ChatColor.WHITE + " ("
                                + ChatColor.GREEN + winCount + "W" + ChatColor.WHITE + " / " + ChatColor.RED + lostCount + "L" + ChatColor.WHITE + ")");
                        }
                    }
                }
            }
        }
    }

    public ArenaPlayer remove(Player player) {
        ArenaPlayer removed = null;
        Collection<ArenaPlayer> currentPlayers = new ArrayList<ArenaPlayer>(players);
        players.clear();
        for (ArenaPlayer arenaPlayer : currentPlayers) {
            if (arenaPlayer.equals(player)) {
                removed = arenaPlayer;
            } else {
                players.add(arenaPlayer);
            }
        }
        Collection<ArenaPlayer> currentQueue = new ArrayList<ArenaPlayer>(queue);
        queue.clear();
        for (ArenaPlayer arenaPlayer : currentQueue) {
            if (arenaPlayer.equals(player)) {
                removed = arenaPlayer;
            } else {
                queue.add(arenaPlayer);
            }
        }
        player.removeMetadata("arena", controller.getPlugin());
        return removed;
    }

    public Player getWinner() {
        if (state == ArenaState.ACTIVE && players.size() == 1) {
            ArenaPlayer winner = players.iterator().next();
            return winner.getPlayer();
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

    protected void messagePlayers(String message, Collection<ArenaPlayer> players) {
        Server server = controller.getPlugin().getServer();
        for (ArenaPlayer arenaPlayer : players) {
            Player player = arenaPlayer.getPlayer();
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
        for (ArenaPlayer arenaPlayer : players) {
            Player player = arenaPlayer.getPlayer();
            if (player != null) {
                player.teleport(getExit());
                player.removeMetadata("arena", controller.getPlugin());
            }
        }
        players.clear();
    }

    protected void clearQueue() {
        for (ArenaPlayer arenaPlayer : queue) {
            Player player = arenaPlayer.getPlayer();
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

    public ArenaPlayer add(Player player) {
        ArenaPlayer arenaPlayer = new ArenaPlayer(this, player);
        queue.add(arenaPlayer);
        player.teleport(getLobby());
        player.setMetadata("arena", new FixedMetadataValue(controller.getPlugin(), arenaPlayer));
        return arenaPlayer;
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
        final Server server = controller.getPlugin().getServer();
        final Player winner = getWinner();
        if (winner != null) {
            final boolean won = deathCount >= requiredKills;
            Bukkit.getScheduler().runTaskLater(controller.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    ArenaPlayer arenaPlayer = remove(winner);
                    if (won && arenaPlayer != null) {
                        boolean draw = arenaPlayer.isDead() || !arenaPlayer.isValid();
                        if (draw) {
                            arenaPlayer.draw();
                            winner.teleport(getLoseLocation());
                            server.broadcastMessage(ChatColor.GRAY + "The " + ChatColor.YELLOW + getName() + ChatColor.GRAY + " match ended in a draw");
                        } else {
                            arenaPlayer.won();
                            updateLeaderboard(arenaPlayer);
                            winner.sendMessage(ChatColor.AQUA + "You have won! Congratulations!");
                            int winCount = arenaPlayer.getWins();
                            int lostCount = arenaPlayer.getLosses();
                            double health = winner.getHealth() / 2;
                            int hearts = (int)Math.floor(health);
                            String heartDescription = Integer.toString(hearts);
                            health = health - hearts;
                            if (health >= 0.5) {
                                heartDescription = heartDescription + " 1/2";
                            }
                            server.broadcastMessage(ChatColor.GOLD + winner.getDisplayName() + " is the champion of " + ChatColor.YELLOW + getName()
                                    + ChatColor.GOLD + " with " + ChatColor.DARK_RED + heartDescription + ChatColor.GOLD
                                    + " hearts, and a total of " + ChatColor.GREEN + Integer.toString(winCount) + ChatColor.GOLD + " wins and "
                                    + ChatColor.RED + Integer.toString(lostCount) + ChatColor.GOLD + " losses.");
                            winner.teleport(getWinLocation());
                        }
                    } else {
                        winner.teleport(getExit());
                        server.broadcastMessage(ChatColor.RED + "The " + ChatColor.YELLOW + getName() + ChatColor.RED + " match ended in a default");
                    }
                    winner.setHealth(20.0);
                    winner.setFoodLevel(20);
                    winner.setFireTicks(0);
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
        ArenaPlayer arenaPlayer = add(player);
        arenaPlayer.joined();

        int winCount = arenaPlayer.getWins();
        int lostCount = arenaPlayer.getLosses();

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

    protected Collection<ArenaPlayer> getAllPlayers() {
        List<ArenaPlayer> allPlayers = new ArrayList<ArenaPlayer>(players);
        allPlayers.addAll(queue);
        return allPlayers;
    }

    protected Collection<ArenaPlayer> getNextRoundPlayers() {
        List<ArenaPlayer> allPlayers = new ArrayList<ArenaPlayer>();
        for (ArenaPlayer queuedPlayer : queue) {
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
        for (ArenaPlayer player : players) {
            sender.sendMessage(ChatColor.GOLD + " " + player.getDisplayName());
        }
        int queuedPlayers = getQueuedPlayers();
        sender.sendMessage(ChatColor.AQUA + "Queued Players: " + ChatColor.DARK_AQUA + queuedPlayers);
        for (ArenaPlayer player : queue) {
            sender.sendMessage(ChatColor.YELLOW + " " + player.getDisplayName());
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
        ArenaPlayer arenaPlayer = remove(player);
        if (arenaPlayer != null) {
            arenaPlayer.lost();
            updateLeaderboard(arenaPlayer);
        }
        Location specroom = getLoseLocation();
        player.setMetadata("respawnLocation", new FixedMetadataValue(controller.getPlugin(), specroom));
        player.sendMessage(ChatColor.AQUA + "You have lost - Better luck next time!");
        check();
    }

    public ArenaController getController() {
        return controller;
    }

    public void updateLeaderboard(ArenaPlayer changedPlayer) {
        int wins = changedPlayer.getWins();
        int losses = changedPlayer.getLosses();

        if (wins + losses < leaderboardGamesRequired) {
            return;
        }

        leaderboard.remove(changedPlayer);
        leaderboard.add(changedPlayer);
        setLeaderboardSize(leaderboardSize);
        Collections.sort(leaderboard, new ArenaPlayerComparator());
    }

    public void setLeaderboardSize(int size) {
        while (leaderboard.size() > size) {
            leaderboard.remove(leaderboard.size() - 1);
        }
        leaderboardSize = size;
    }

    public void setLeaderboardGamesRequired(int required) {
        leaderboardGamesRequired = required;
        Collection<ArenaPlayer> currentLeaderboard = new ArrayList<ArenaPlayer>(leaderboard);
        leaderboard.clear();
        for (ArenaPlayer player : currentLeaderboard) {
            updateLeaderboard(player);
        }
    }

    public void describeStats(CommandSender sender, Player player) {
        ArenaPlayer arenaPlayer = new ArenaPlayer(this, player);

        int wins = arenaPlayer.getWins();
        int losses = arenaPlayer.getLosses();
        int draws = arenaPlayer.getDraws();
        int quits = arenaPlayer.getQuits();
        float ratio = arenaPlayer.getWinRatio();

        Integer rank = null;
        int ranking = 1;
        for (ArenaPlayer testPlayer : leaderboard) {
            if (testPlayer.equals(arenaPlayer)) {
                rank = ranking;
                break;
            }
            ranking++;
        }
        if (rank != null) {
            sender.sendMessage(ChatColor.DARK_PURPLE + player.getDisplayName() + ChatColor.DARK_PURPLE +
                    " is ranked " + ChatColor.AQUA + "#" + Integer.toString(rank) + ChatColor.DARK_PURPLE + " for " + ChatColor.GOLD + getName());
        } else {
            sender.sendMessage(ChatColor.DARK_PURPLE + player.getDisplayName() + ChatColor.DARK_RED +
                    " is not on the leaderboard for " + ChatColor.GOLD + getName());
        }

        sender.sendMessage(ChatColor.GREEN + "Wins: " + ChatColor.WHITE + Integer.toString(wins));
        sender.sendMessage(ChatColor.RED + "Losses: " + ChatColor.WHITE + Integer.toString(losses));
        sender.sendMessage(ChatColor.GOLD + "Win Ratio: " + ChatColor.WHITE + Integer.toString((int)(ratio * 100)) + "%");
        sender.sendMessage(ChatColor.YELLOW + "Draws: " + ChatColor.WHITE + Integer.toString(draws));
        sender.sendMessage(ChatColor.GRAY + "Defaults: " + ChatColor.WHITE + Integer.toString(quits));
    }

    public void describeLeaderboard(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + getName() + ChatColor.YELLOW +" Leaderboard: ");
        sender.sendMessage(ChatColor.AQUA + Integer.toString(leaderboard.size()) + ChatColor.DARK_AQUA + " players with at least " +
                ChatColor.AQUA + Integer.toString(leaderboardGamesRequired) + ChatColor.DARK_AQUA + " games:");
        int position = 1;
        for (ArenaPlayer arenaPlayer : leaderboard) {
            int wins = arenaPlayer.getWins();
            int losses = arenaPlayer.getLosses();
            float ratio = arenaPlayer.getWinRatio();
            sender.sendMessage(ChatColor.LIGHT_PURPLE + Integer.toString(position) + ": " + ChatColor.WHITE
                    + ChatColor.DARK_PURPLE + arenaPlayer.getDisplayName()
                    + ChatColor.WHITE + ": " + ChatColor.GREEN + Integer.toString(wins) + "W"
                    + ChatColor.WHITE + " / " + ChatColor.RED + Integer.toString(losses) + "L"
                    + ChatColor.WHITE + " = " + ChatColor.GOLD + Integer.toString((int)(ratio * 100)) + "%");
            position++;
        }
    }
}
