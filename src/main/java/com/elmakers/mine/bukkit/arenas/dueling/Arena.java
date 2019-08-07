package com.elmakers.mine.bukkit.arenas.dueling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Sign;
import org.bukkit.material.Skull;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import com.elmakers.mine.bukkit.api.block.MaterialAndData;
import com.elmakers.mine.bukkit.api.entity.EntityData;
import com.elmakers.mine.bukkit.api.item.ItemUpdatedCallback;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.block.DefaultMaterials;
import com.elmakers.mine.bukkit.utility.ConfigurationUtils;

public class Arena {
    private static Random random = new Random();

    private ArenaState state = ArenaState.LOBBY;
    private long started;
    private long lastTick;
    private Queue<ArenaPlayer> queue = new LinkedList<>();
    private Set<ArenaPlayer> players = new HashSet<>();
    private Set<ArenaPlayer> deadPlayers = new HashSet<>();

    private List<Location> spawns = new ArrayList<>();
    private List<ArenaStage> stages = new ArrayList<>();
    private int currentStage = 0;
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
    private int winXP = 0;
    private int loseXP = 0;
    private int drawXP = 0;
    private int winSP = 0;
    private int loseSP = 0;
    private int drawSP = 0;
    private int winMoney = 0;
    private int loseMoney = 0;
    private int drawMoney = 0;

    private int countdown = 10;
    private int countdownMax = 10;

    private int leaderboardSize = 5;
    private int leaderboardRecordSize = 30;
    private int leaderboardGamesRequired = 5;

    private int maxTeleportDistance = 64;
    private int announcerRange = 64;

    private boolean opCheck = true;

    private int duration = 0;
    private int suddenDeath = 0;
    private PotionEffect suddenDeathEffect = null;
    private String startCommands;
    private int borderMin = 0;
    private int borderMax = 0;

    private List<ArenaPlayer> leaderboard = new ArrayList<>();
    private Location leaderboardLocation;
    private BlockFace leaderboardFacing;

    private int portalDamage;
    private int portalEnterDamage;
    private String portalDeathMessage;

    private ArenaType arenaType;
    private final String key;
    private String name;
    private String description;

    private boolean keepInventory;
    private boolean keepLevel;

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
        leaderboardRecordSize = configuration.getInt("leaderboard_record_size", 30);
        leaderboardGamesRequired = configuration.getInt("leaderboard_games_required", 5);

        maxTeleportDistance = configuration.getInt("max_teleport_distance", 64);
        announcerRange = configuration.getInt("announcer_range", 64);

        countdown = configuration.getInt("countdown", 10);
        countdownMax = configuration.getInt("countdown_max", 30);

        opCheck = configuration.getBoolean("op_check", true);
        startCommands = configuration.getString("start_commands");

        borderMin = configuration.getInt("border_min");
        borderMax = configuration.getInt("border_max");

        keepInventory = configuration.getBoolean("keep_inventory", false);
        keepLevel = configuration.getBoolean("keep_level", false);

        arenaType = ArenaType.parse(configuration.getString("type"));
        if (arenaType == null) {
            arenaType = ArenaType.FFA;
        }

        lose = ConfigurationUtils.toLocation(configuration.getString("lose"));
        win = ConfigurationUtils.toLocation(configuration.getString("win"));
        lobby = ConfigurationUtils.toLocation(configuration.getString("lobby"));
        center = ConfigurationUtils.toLocation(configuration.getString("center"));
        exit = ConfigurationUtils.toLocation(configuration.getString("exit"));

        winXP = configuration.getInt("win_xp", 0);
        loseXP = configuration.getInt("lose_xp", 0);
        drawXP = configuration.getInt("draw_xp", 0);

        winSP = configuration.getInt("win_sp", 0);
        loseSP = configuration.getInt("lose_sp", 0);
        drawSP = configuration.getInt("draw_sp", 0);

        winMoney = configuration.getInt("win_money", 0);
        loseMoney = configuration.getInt("lose_money", 0);
        drawMoney = configuration.getInt("draw_money", 0);

        duration = configuration.getInt("duration", 0);
        suddenDeath = configuration.getInt("sudden_death", 0);
        if (configuration.contains("sudden_death_effect")) {
            setSuddenDeathEffect(configuration.getString("sudden_death_effect"));
        }

        for (String s : configuration.getStringList("spawns")) {
            spawns.add(ConfigurationUtils.toLocation(s));
        }

        if (configuration.contains("randomize.spawn")) {
            randomizeSpawn = ConfigurationUtils.toVector(configuration.getString("randomize.spawn"));
        }

        if (configuration.contains("stages")) {
            Collection<ConfigurationSection> stageConfigurations = ConfigurationUtils.getNodeList(configuration, "stages");
            for (ConfigurationSection stageConfiguration : stageConfigurations) {
                stages.add(new ArenaStage(this, controller.getMagic(), stageConfiguration));
            }
        }

        if (configuration.contains("leaderboard_sign_location") && configuration.contains("leaderboard_sign_facing")) {
            leaderboardLocation = ConfigurationUtils.toLocation(configuration.getString("leaderboard_sign_location"));
            leaderboardFacing = ConfigurationUtils.toBlockFace(configuration.getString("leaderboard_sign_facing"));
        }

        // For migrating legacy data
        loadData(configuration);
    }

    public void loadData(ConfigurationSection configuration) {
        if (configuration.contains("leaderboard")) {
            leaderboard.clear();
            ConfigurationSection leaders = configuration.getConfigurationSection("leaderboard");
            Collection<String> leaderboardKeys = leaders.getKeys(false);
            for (String key : leaderboardKeys) {
                ConfigurationSection leaderConfig = leaders.getConfigurationSection(key);
                ArenaPlayer loadedPlayer = new ArenaPlayer(this, leaderConfig);
                leaderboard.add(loadedPlayer);
            }
            Collections.sort(leaderboard, new ArenaPlayerComparator());
        }
    }

    public boolean setSuddenDeathEffect(String value) {
        if (value == null || value.isEmpty()) {
            suddenDeathEffect = null;
            return false;
        }
        int ticks = 100;
        int power = 1;
        PotionEffectType effectType;
        try {
            String effectName;
            if (value.contains(":")) {
                String[] pieces = value.split(":");
                effectName = pieces[0];
                if (pieces.length > 1) {
                    power = (int)Float.parseFloat(pieces[1]);
                }
                if (pieces.length > 2) {
                    ticks = (int) Float.parseFloat(pieces[2]);
                }
            } else {
                effectName = value;
            }
            effectType = PotionEffectType.getByName(effectName.toUpperCase());
            suddenDeathEffect = new PotionEffect(effectType, ticks, power, true);
        } catch (Exception ex) {
            Bukkit.getLogger().warning("Error parsing potion effect: " + value);
            suddenDeathEffect = null;
        }

        return suddenDeathEffect != null;
    }

    public void save(ConfigurationSection configuration) {
        if (!isValid()) {
            return;
        }

        configuration.set("name", name);
        configuration.set("description", description);
        configuration.set("minplayers", minPlayers);
        configuration.set("maxplayers", maxPlayers);
        configuration.set("required_kills", requiredKills);

        configuration.set("lose_xp", loseXP);
        configuration.set("draw_xp", drawXP);
        configuration.set("win_xp", winXP);

        configuration.set("lose_sp", loseSP);
        configuration.set("draw_sp", drawSP);
        configuration.set("win_sp", winSP);

        configuration.set("lose_money", loseMoney);
        configuration.set("draw_money", drawMoney);
        configuration.set("win_money", winMoney);

        configuration.set("duration", duration);
        configuration.set("sudden_death", suddenDeath);
        if (suddenDeathEffect != null) {
            configuration.set("sudden_death_effect",
                    suddenDeathEffect.getType().getName().toLowerCase() + ":"
                            + suddenDeathEffect.getAmplifier() + ":"
                            + suddenDeathEffect.getDuration()
            );
        }
        configuration.set("border_min", borderMin);
        configuration.set("border_max", borderMax);
        configuration.set("start_commands", startCommands);

        configuration.set("keep_inventory", keepInventory);
        configuration.set("keep_level", keepLevel);

        configuration.set("leaderboard_size", leaderboardSize);
        configuration.set("leaderboard_record_size", leaderboardRecordSize);
        configuration.set("leaderboard_games_required", leaderboardGamesRequired);

        configuration.set("portal_damage", portalDamage);
        configuration.set("portal_enter_damage", portalEnterDamage);
        configuration.set("portal_death_message", portalDeathMessage);

        configuration.set("max_teleport_distance", maxTeleportDistance);
        configuration.set("announcer_range", announcerRange);

        configuration.set("countdown", countdown);
        configuration.set("countdown_max", countdownMax);
        configuration.set("op_check", opCheck);

        configuration.set("type", arenaType.name());

        configuration.set("lobby", ConfigurationUtils.fromLocation(lobby));
        configuration.set("win", ConfigurationUtils.fromLocation(win));
        configuration.set("lose", ConfigurationUtils.fromLocation(lose));
        configuration.set("center", ConfigurationUtils.fromLocation(center));
        configuration.set("exit", ConfigurationUtils.fromLocation(exit));

        List<String> spawnList = new ArrayList<>();
        for (Location spawn : spawns) {
            spawnList.add(ConfigurationUtils.fromLocation(spawn));
        }
        configuration.set("spawns", spawnList);

        if (!stages.isEmpty()) {
            List<ConfigurationSection> stageConfigurations = new ArrayList<>();
            for (ArenaStage stage : stages) {
                ConfigurationSection section = new MemoryConfiguration();
                stage.save(section);
                stageConfigurations.add(section);
            }
            configuration.set("stages", stageConfigurations);
        }

        if (randomizeSpawn != null) {
            configuration.set("randomize.spawn", ConfigurationUtils.fromVector(randomizeSpawn));
        }

        if (leaderboardLocation != null && leaderboardFacing != null) {
            configuration.set("leaderboard_sign_location", ConfigurationUtils.fromLocation(leaderboardLocation));
            configuration.set("leaderboard_sign_facing", ConfigurationUtils.fromBlockFace(leaderboardFacing));
        }
    }

    public void saveData(ConfigurationSection configuration) {
        if (!isValid()) {
            return;
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
        if (!isValid()) {
            return;
        }

        state = ArenaState.ACTIVE;
        started = System.currentTimeMillis();
        lastTick = started;

        if (startCommands != null && !startCommands.isEmpty()) {
            String[] commands = StringUtils.split(startCommands, ',');
            CommandSender sender = Bukkit.getConsoleSender();
            for (String command : commands) {
                org.bukkit.Bukkit.getLogger().info("RUNNING: " + command);
                controller.getPlugin().getServer().dispatchCommand(sender, command);
            }
        }
        if (borderMax > 0 && duration > 0) {
            World world = getCenter().getWorld();
            WorldBorder border = world.getWorldBorder();
            border.setSize(borderMax);
            border.setSize(borderMin, duration / 1000);
        }

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
        List<ArenaPlayer> players = new ArrayList<>(this.players);
        for (ArenaPlayer arenaPlayer : players) {
            Player player = arenaPlayer.getPlayer();
            if (player == null) {
                continue;
            }

            arenaPlayer.heal();
            player.sendMessage(ChatColor.GOLD + "BEGIN!");

            Location spawn = spawns.get(num);
            if (randomizeSpawn != null) {
                spawn = spawn.clone();
                spawn.add(
                    (2 * random.nextDouble() - 1) * randomizeSpawn.getX(),
                    (2 * random.nextDouble() - 1) * randomizeSpawn.getY(),
                    (2 * random.nextDouble() - 1) * randomizeSpawn.getZ()
                );
            }

            // Wrap index around to player
            num = (num + 1) % spawns.size();
            arenaPlayer.teleport(spawn);
        }

        ArenaStage currentStage = getCurrentStage();
        if (currentStage != null) {
            currentStage.start();
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
                        int winCount = otherArenaPlayer.getWins();
                        int lostCount = otherArenaPlayer.getLosses();

                        player.sendMessage(ChatColor.YELLOW + " with " + ChatColor.DARK_AQUA + otherArenaPlayer.getDisplayName() + ChatColor.WHITE + " ("
                            + ChatColor.GREEN + winCount + "W" + ChatColor.WHITE + " / " + ChatColor.RED + lostCount + "L" + ChatColor.WHITE + ")");
                    }
                }
            }
        }
    }

    public void removePlayer(Player player) {
        ArenaPlayer removePlayer = new ArenaPlayer(this, player);
        players.remove(removePlayer);
        queue.remove(removePlayer);
        removePlayer.clearMetadata();
    }

    public ArenaPlayer getWinner() {
        if (players.size() == 1) {
            ArenaPlayer winner = players.iterator().next();
            return winner;
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

    public void messagePlayers(String message) {
        messagePlayers(message, getAllPlayers());
    }

    protected void messagePlayers(String message, Collection<ArenaPlayer> players) {
        for (ArenaPlayer arenaPlayer : players) {
            Player player = arenaPlayer.getPlayer();
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    public void announce(String message) {
        int rangeSquared = announcerRange * announcerRange;
        Collection<? extends Player> players = controller.getPlugin().getServer().getOnlinePlayers();
        for (Player player : players) {
            Location playerLocation = player.getLocation();
            if (!playerLocation.getWorld().equals(center.getWorld())) {
                continue;
            }
            if (playerLocation.distanceSquared(center) < rangeSquared) {
                player.sendMessage(message);
            }
        }
    }

    public void messageInGamePlayers(String message) {
        messagePlayers(message, players);
    }

    public void messageNextRoundPlayers(String message) {
        messagePlayers(message, getNextRoundPlayers());
    }

    public void startCountdown() {
        startCountdown(countdown);
    }

    public void startCountdown(int time) {
        if (state != ArenaState.LOBBY) {
            return;
        }
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
        if (state == ArenaState.LOBBY) {
            return false;
        }
        messageInGamePlayers(ChatColor.DARK_RED + "This match has been cancelled!");
        finish();
        return true;
    }

    protected void finish() {
        ArenaStage currentStage = getCurrentStage();
        if (currentStage != null) {
            currentStage.finish();
        }
        state = ArenaState.LOBBY;
        clearPlayers();

        // Check for a new start
        checkStart();
    }

    protected void exitPlayers() {
        for (ArenaPlayer arenaPlayer : players) {
            arenaPlayer.teleport(getExit());
        }
    }

    protected void clearPlayers() {
        for (ArenaPlayer arenaPlayer : players) {
            Player player = arenaPlayer.getPlayer();
            if (player != null) {
                player.removeMetadata("arena", controller.getPlugin());
            }
        }
        players.clear();
        deadPlayers.clear();
    }

    protected void clearQueue() {
        for (ArenaPlayer arenaPlayer : queue) {
            Player player = arenaPlayer.getPlayer();
            if (player != null) {
                arenaPlayer.teleport(getExit());
                player.removeMetadata("arena", controller.getPlugin());
            }
        }
        queue.clear();
    }

    public boolean isStarted() {
        return state == ArenaState.ACTIVE || state == ArenaState.WON;
    }

    public boolean isFull() {
        return queue.size() >= maxPlayers;
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
            List<Location> centerList = new ArrayList<>();
            centerList.add(center);
            return centerList;
        }

        return spawns;
    }

    public void addMobSpawn(Location location) {
        getOrCreateCurrentStage().addMobSpawn(location);
    }

    public Location removeMobSpawn(Location location) {
        return getOrCreateCurrentStage().removeMobSpawn(location);
    }

    public void setStartSpell(String startSpell) {
        ArenaStage stage = getOrCreateCurrentStage();
        stage.setStartSpell(startSpell);
    }

    public void setEndSpell(String endSpell) {
        ArenaStage stage = getOrCreateCurrentStage();
        stage.setEndSpell(endSpell);
    }

    public void addMob(EntityData mobType, int count) {
        ArenaStage stage = getOrCreateCurrentStage();
        stage.addMob(mobType, count);
    }

    public ArenaStage getCurrentStage() {
        if (currentStage >= 0 && currentStage < stages.size()) {
            return stages.get(currentStage);
        }
        return null;
    }

    public ArenaStage getOrCreateCurrentStage() {
        if (stages.isEmpty()) {
            stages.add(new ArenaStage(this));
            currentStage = 0;
        }

        return stages.get(currentStage);
    }

    public void setMinPlayers(int players) {
        minPlayers = players;
    }

    public void setMaxPlayers(int players) {
        maxPlayers = players;
    }

    public void setType(ArenaType types) {
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

        if (state == ArenaState.LOBBY) {
            return;
        }

        final Server server = controller.getPlugin().getServer();
        if (players.size() == 0 && state != ArenaState.WON) {
            if (isMobArena()) {
                announce(ChatColor.RED + "The " + ChatColor.YELLOW + getName() + ChatColor.RED + " match has ended, better luck next time!");
            } else {
                announce(ChatColor.RED + "The " + ChatColor.YELLOW + getName() + ChatColor.RED + " match ended in a default");
            }
            exitPlayers();
            finish();
            return;
        }

        if (state != ArenaState.WON && isMobArena()) {
            ArenaStage currentStage = getCurrentStage();
            if (currentStage.isFinished()) {
                state = ArenaState.WON;
                server.getScheduler().runTaskLater(controller.getPlugin(), new Runnable() {
                    @Override
                    public void run() {
                        for (final ArenaPlayer winner : players) {
                            if (winner != null) {
                                playerWon(winner);
                                winner.heal();
                            }
                        }
                        finish();
                    }
                }, 5 * 20);
            }
        } else if (players.size() == 1 && state != ArenaState.WON) {
            state = ArenaState.WON;
            server.getScheduler().runTaskLater(controller.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    final ArenaPlayer winner = getWinner();
                    final boolean defaulted = deadPlayers.size() < requiredKills;
                    final boolean won = winner != null && winner.isValid() && !winner.isDead();
                    if (defaulted) {
                        if (winner != null) {
                            winner.teleport(getExit());
                        }
                        announce(ChatColor.RED + "The " + ChatColor.YELLOW + getName() + ChatColor.RED + " match ended in a default");
                    } else if (won) {
                        playerWon(winner);
                    } else {
                        if (winner != null) {
                            winner.draw();
                            winner.teleport(getLoseLocation());
                        }
                        for (ArenaPlayer loser : deadPlayers) {
                            loser.draw();
                        }
                        announce(ChatColor.GRAY + "The " + ChatColor.YELLOW + getName() + ChatColor.GRAY + " match ended in a draw");
                    }
                    if (winner != null) {
                        winner.heal();
                    }
                    finish();
                }
            }, 5 * 20);
        }
    }

    protected void playerWon(ArenaPlayer winner) {
        winner.won();
        updateLeaderboard(winner);
        for (ArenaPlayer loser : deadPlayers) {
            loser.lost();
            updateLeaderboard(loser);
        }
        updateLeaderboard();
        winner.sendMessage(ChatColor.AQUA + "You have won! Congratulations!");
        int winCount = winner.getWins();
        int lostCount = winner.getLosses();
        double health = winner.getHealth() / 2;
        int hearts = (int)Math.floor(health);
        String heartDescription = Integer.toString(hearts);
        health = health - hearts;
        if (health >= 0.5) {
            heartDescription = heartDescription + " 1/2";
        }
        announce(ChatColor.GOLD + winner.getDisplayName() + " is the champion of " + ChatColor.YELLOW + getName());
        announce(ChatColor.GOLD + " with " + ChatColor.DARK_RED + heartDescription + ChatColor.GOLD
                + " hearts, and a total of " + ChatColor.GREEN + Integer.toString(winCount) + ChatColor.GOLD + " wins and "
                + ChatColor.RED + Integer.toString(lostCount) + ChatColor.GOLD + " losses.");
        winner.teleport(getWinLocation());
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

        ArenaPlayer arenaPlayer = new ArenaPlayer(this, player);
        queue.add(arenaPlayer);

        arenaPlayer.teleport(getLobby());
        player.setMetadata("arena",
                new FixedMetadataValue(controller.getPlugin(), arenaPlayer));

        arenaPlayer.joined();

        Bukkit.getPluginManager().callEvent(
                new PlayerJoinedArenaEvent(player, arenaPlayer, this));

        // Announce join
        int winCount = arenaPlayer.getWins();
        int lostCount = arenaPlayer.getLosses();

        if (winCount == 0 && lostCount == 0) {
            announce(ChatColor.AQUA + arenaPlayer.getDisplayName() + ChatColor.DARK_AQUA + " has joined " + ChatColor.AQUA + getName() + ChatColor.DARK_AQUA + " for the first time");
        } else {
            announce(ChatColor.AQUA + arenaPlayer.getDisplayName() + ChatColor.DARK_AQUA + " has joined " + ChatColor.AQUA + getName());
            announce(ChatColor.DARK_AQUA + " with " + ChatColor.GREEN + Integer.toString(winCount) + ChatColor.DARK_AQUA + " wins and "
                    + ChatColor.RED + Integer.toString(lostCount) + ChatColor.DARK_AQUA + " losses.");
        }

        // Check if we have enough players
        checkStart();
    }

    protected void checkStart() {
        if (isStarted()) {
            return;
        }

        if (isReady()) {
            if (isFull()) {
                startCountdown(countdown);
            } else {
                startCountdown(countdownMax);
            }
        } else {
            lobbyMessage();
        }
    }

    protected Collection<ArenaPlayer> getAllPlayers() {
        List<ArenaPlayer> allPlayers = new ArrayList<>(players);
        allPlayers.addAll(queue);
        allPlayers.addAll(deadPlayers);
        return allPlayers;
    }

    protected Collection<ArenaPlayer> getNextRoundPlayers() {
        List<ArenaPlayer> allPlayers = new ArrayList<>();
        for (ArenaPlayer queuedPlayer : queue) {
            if (allPlayers.size() >= maxPlayers) {
                break;
            }
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
        if (opCheck) {
            sender.sendMessage(ChatColor.RED + "OP Wand Check Enabled");
        }
        if (keepInventory) {
            sender.sendMessage(ChatColor.GREEN + "Players keep their inventory on death");
        }
        if (keepLevel) {
            sender.sendMessage(ChatColor.GREEN + "Players keep their XP levels on death");
        }
        int minPlayers = getMinPlayers();
        int maxPlayers = getMaxPlayers();
        sender.sendMessage(
                ChatColor.AQUA + "Min / Max Players: "
                        + ChatColor.DARK_AQUA + minPlayers
                        + ChatColor.WHITE + " / " + ChatColor.DARK_AQUA
                        + maxPlayers);
        sender.sendMessage(
                ChatColor.AQUA + "Required Kills: "
                        + ChatColor.DARK_AQUA + requiredKills);
        sender.sendMessage(
                ChatColor.AQUA + "Countdown: " + ChatColor.DARK_AQUA + countdown
                        + ChatColor.WHITE + " / " + ChatColor.DARK_AQUA
                        + countdownMax);

        if (duration > 0) {
            int minutes = (int)Math.ceil((double)duration / 60 / 1000);
            int sd = (int)Math.ceil((double)suddenDeath / 1000);
            sender.sendMessage(
                    ChatColor.AQUA + "Duration: "
                            + ChatColor.DARK_AQUA + minutes
                            + ChatColor.WHITE + " minutes");
            if (suddenDeathEffect != null && suddenDeath > 0) {
                sender.sendMessage(
                        ChatColor.DARK_RED + " Sudden death "
                                + ChatColor.RED + sd
                                + ChatColor.DARK_RED
                                + " seconds before end with " + ChatColor.RED
                                + suddenDeathEffect.getType().getName()
                                        .toLowerCase()
                + "@" + suddenDeathEffect.getAmplifier());
            }
        }

        if (startCommands != null && !startCommands.isEmpty()) {
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Start Commands: " + ChatColor.AQUA + startCommands);
        }
        if (borderMax > 0) {
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Border: " + ChatColor.AQUA + borderMax + ChatColor.LIGHT_PURPLE + " to " + ChatColor.AQUA + borderMin);
        }

        if (winXP > 0) {
            sender.sendMessage(ChatColor.AQUA + "Winning Reward: " + ChatColor.LIGHT_PURPLE + winXP + ChatColor.AQUA + " xp");
        }
        if (loseXP > 0) {
            sender.sendMessage(ChatColor.AQUA + "Losing Reward: " + ChatColor.LIGHT_PURPLE + loseXP + ChatColor.AQUA + " xp");
        }
        if (drawXP > 0) {
            sender.sendMessage(ChatColor.AQUA + "Draw Reward: " + ChatColor.LIGHT_PURPLE + drawXP + ChatColor.AQUA + " xp");
        }

        if (winSP > 0) {
            sender.sendMessage(ChatColor.AQUA + "Winning Reward: " + ChatColor.LIGHT_PURPLE + winSP + ChatColor.AQUA + " sp");
        }
        if (loseSP > 0) {
            sender.sendMessage(ChatColor.AQUA + "Losing Reward: " + ChatColor.LIGHT_PURPLE + loseSP + ChatColor.AQUA + " sp");
        }
        if (drawSP > 0) {
            sender.sendMessage(ChatColor.AQUA + "Draw Reward: " + ChatColor.LIGHT_PURPLE + drawSP + ChatColor.AQUA + " sp");
        }

        if (winMoney > 0) {
            sender.sendMessage(ChatColor.AQUA + "Winning Reward: $" + ChatColor.LIGHT_PURPLE + winMoney + ChatColor.AQUA);
        }
        if (loseMoney > 0) {
            sender.sendMessage(ChatColor.AQUA + "Losing Reward: $" + ChatColor.LIGHT_PURPLE + loseMoney + ChatColor.AQUA);
        }
        if (drawMoney > 0) {
            sender.sendMessage(ChatColor.AQUA + "Draw Reward: $" + ChatColor.LIGHT_PURPLE + drawMoney + ChatColor.AQUA);
        }

        int spawnSize = spawns.size();
        if (spawnSize == 1) {
            sender.sendMessage(ChatColor.BLUE + "Spawn: " + printLocation(spawns.get(0)));
        } else {
            sender.sendMessage(ChatColor.BLUE + "Spawns: " + ChatColor.GRAY + spawnSize);
            for (Location spawn : spawns) {
                sender.sendMessage(ChatColor.GRAY + " " + printLocation(spawn));
            }
        }
        if (randomizeSpawn != null) {
            sender.sendMessage(ChatColor.DARK_BLUE + " Randomize: " + ChatColor.BLUE + randomizeSpawn);
        }
        sender.sendMessage(ChatColor.BLUE + "Lobby: " + printLocation(lobby));
        sender.sendMessage(ChatColor.BLUE + "Win: " + printLocation(win));
        sender.sendMessage(ChatColor.BLUE + "Lose: " + printLocation(lose));
        sender.sendMessage(ChatColor.BLUE + "Exit: " + printLocation(exit));
        sender.sendMessage(ChatColor.BLUE + "Center: " + printLocation(center));
        int numStages = stages.size();
        if (numStages > 0) {
            if (numStages == 1) {
                stages.get(0).describe(sender, " ");
            } else {
                sender.sendMessage(ChatColor.BLUE + "Stages: " + ChatColor.GRAY + numStages);
                for (ArenaStage stage : stages) {
                    stage.describe(sender, " ");
                }
            }
        }

        if (portalDamage > 0 || portalEnterDamage > 0) {
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Portal Entry Damage: " + ChatColor.DARK_PURPLE + portalEnterDamage);
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Portal Damage: " + ChatColor.DARK_PURPLE + portalDamage);
            if (portalDeathMessage != null && !portalDeathMessage.isEmpty()) {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "Portal Death Message: " + ChatColor.DARK_PURPLE + portalDeathMessage);
            }
        }
        sender.sendMessage(ChatColor.YELLOW + "Announcer Range: " + ChatColor.GOLD + announcerRange);
        sender.sendMessage(ChatColor.YELLOW + "Leaderboard Size: " + ChatColor.GOLD + leaderboardSize + ChatColor.WHITE + "/" + ChatColor.GOLD + leaderboardRecordSize);
        sender.sendMessage(ChatColor.AQUA + "State: " + ChatColor.DARK_AQUA + state);

        int inGamePlayers = getInGamePlayers();
        sender.sendMessage(ChatColor.DARK_GREEN + "Active Players: " + ChatColor.GREEN + inGamePlayers);
        for (ArenaPlayer player : players) {
            sender.sendMessage(ChatColor.GOLD + " " + player.getDisplayName());
        }
        int deathCount = deadPlayers.size();
        sender.sendMessage(ChatColor.DARK_RED + "Dead Players: " + ChatColor.RED + deathCount);
        for (ArenaPlayer player : deadPlayers) {
            sender.sendMessage(ChatColor.RED + " " + player.getDisplayName());
        }
        int queuedPlayers = getQueuedPlayers();
        sender.sendMessage(ChatColor.YELLOW + "Queued Players: " + ChatColor.GOLD + queuedPlayers);
        for (ArenaPlayer player : queue) {
            sender.sendMessage(ChatColor.YELLOW + " " + player.getDisplayName());
        }
    }

    protected String printLocation(Location location) {
        if (location == null) {
            return ChatColor.DARK_GRAY + "(None)";
        }

        return "" + ChatColor.GRAY + location.getBlockX()
                + ChatColor.DARK_GRAY + ","
                + ChatColor.GRAY + location.getBlockY()
                + ChatColor.DARK_GRAY + ","
                + ChatColor.GRAY + location.getBlockZ()
                + ChatColor.DARK_GRAY + " : "
                + ChatColor.GRAY + location.getWorld().getName();
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

    public void mobDied(LivingEntity entity) {
        ArenaStage currentStage = getCurrentStage();
        if (currentStage != null) {
            currentStage.mobDied(entity);
            if (isStarted()) {
                check();
            }
        }
    }

    public void died(Player player) {
        ArenaPlayer arenaPlayer = new ArenaPlayer(this, player);
        if (isStarted()) {
            if (players.contains(arenaPlayer)) {
                deadPlayers.add(arenaPlayer);
                players.remove(arenaPlayer);
                Location specroom = getLoseLocation();
                player.setMetadata("respawnLocation", new FixedMetadataValue(controller.getPlugin(), specroom));
                player.sendMessage(ChatColor.AQUA + "You have lost - Better luck next time!");
            }
        } else {
            if (queue.contains(arenaPlayer)) {
                player.sendMessage(ChatColor.RED + "You died before the match even started!");
            }
            queue.remove(arenaPlayer);
        }
        player.removeMetadata("arena", controller.getPlugin());
        check();
    }

    public ArenaController getController() {
        return controller;
    }

    public void removeFromLeaderboard(ArenaPlayer removePlayer) {
        leaderboard.remove(removePlayer);
    }

    public void updateLeaderboard(ArenaPlayer changedPlayer) {
        int wins = changedPlayer.getWins();
        int losses = changedPlayer.getLosses();

        leaderboard.remove(changedPlayer);
        if (wins + losses < leaderboardGamesRequired) {
            return;
        }

        leaderboard.add(changedPlayer);
        Collections.sort(leaderboard, new ArenaPlayerComparator());
        setLeaderboardSize(leaderboardSize);
    }

    public void updateLeaderboard() {
        MaterialAndData skullMaterial = DefaultMaterials.getPlayerSkullWallBlock();
        Block leaderboardBlock = getLeaderboardBlock();
        if (leaderboardBlock != null && leaderboardFacing != null) {
            BlockFace rightDirection = goLeft(leaderboardFacing);
            leaderboardBlock = leaderboardBlock.getRelative(BlockFace.UP);
            int size = Math.min(leaderboard.size(), leaderboardSize);
            BlockFace skullFace = leaderboardFacing;
            for (int i = size - 1; i >= 0; i--) {
                ArenaPlayer player = leaderboard.get(i);
                if (canReplace(leaderboardBlock)) {
                    skullMaterial.modify(leaderboardBlock);
                    BlockState blockState = leaderboardBlock.getState();
                    MaterialData data = blockState.getData();
                    if (data instanceof Skull) {
                        Skull skull = (Skull)data;
                        skull.setFacingDirection(skullFace);
                    }
                    if (blockState instanceof org.bukkit.block.Skull) {
                        org.bukkit.block.Skull skullBlock = (org.bukkit.block.Skull)blockState;
                        skullBlock.setSkullType(SkullType.PLAYER);
                        controller.getMagic().setSkullOwner(skullBlock, player.getUUID());
                    }
                }
                Block neighborBlock = leaderboardBlock.getRelative(rightDirection);
                if (canReplace(neighborBlock)) {
                    neighborBlock.setType(Material.WALL_SIGN);
                    BlockState blockState = neighborBlock.getState();
                    MaterialData data = blockState.getData();
                    if (data instanceof Sign) {
                        Sign sign = (Sign)data;
                        sign.setFacingDirection(leaderboardFacing);
                    }
                    if (blockState instanceof org.bukkit.block.Sign) {
                        org.bukkit.block.Sign signBlock = (org.bukkit.block.Sign)blockState;
                        signBlock.setLine(0, ChatColor.DARK_PURPLE
                                + player.getDisplayName());
                        signBlock.setLine(1, ChatColor.LIGHT_PURPLE + "#"
                                + Integer.toString(i + 1) + " "
                                + ChatColor.WHITE + " : "
                                + ChatColor.BLACK + Integer.toString(
                                        (int) (player.getWinRatio() * 100))
                                + "% "
                                + String.format(
                                        "(%.2f)",
                                        player.getWinConfidence()));
                        signBlock.setLine(2, ChatColor.GREEN + "Wins   : "
                                + ChatColor.DARK_GREEN + player.getWins());
                        signBlock.setLine(3, ChatColor.RED + "Losses : "
                                + ChatColor.DARK_RED + player.getLosses());
                    }
                    blockState.update();
                }
                leaderboardBlock = leaderboardBlock.getRelative(BlockFace.UP);
            }
        }
    }

    protected void clearLeaderboardBlock(Block block) {
        Material blockType = block.getType();
        MaterialAndData skullMaterial = DefaultMaterials.getPlayerSkullWallBlock();
        if (blockType == skullMaterial.getMaterial() || blockType == Material.WALL_SIGN) {
            block.setType(Material.AIR);
        }
    }

    protected boolean canReplace(Block block) {
        Material blockType = block.getType();
        MaterialAndData skullMaterial = DefaultMaterials.getPlayerSkullWallBlock();
        return blockType == Material.AIR || blockType == Material.WALL_SIGN || blockType == skullMaterial.getMaterial();
    }

    protected Block getLeaderboardBlock() {
        Block block = null;
        if (leaderboardLocation != null) {
            Block testBlock = leaderboardLocation.getBlock();
            if (testBlock.getType() == Material.WALL_SIGN) {
                block = testBlock;
            } else {
                leaderboardLocation = null;
                leaderboardFacing = null;
            }
        }
        return block;
    }

    /**
     * A helper function to go change a given direction to the direction "to the right".
     *
     * <p>There's probably some better matrix-y, math-y way to do this.
     * It'd be nice if this was in BlockFace.
     *
     * @param direction The current direction
     * @return The direction to the left
     */
    public static BlockFace goLeft(BlockFace direction) {
        switch (direction) {
        case EAST:
            return BlockFace.NORTH;
        case NORTH:
            return BlockFace.WEST;
        case WEST:
            return BlockFace.SOUTH;
        case SOUTH:
            return BlockFace.EAST;
        default:
            return direction;
        }
    }

    /**
     * A helper function to go change a given direction to the direction "to the right".
     *
     * <p>There's probably some better matrix-y, math-y way to do this.
     * It'd be nice if this was in BlockFace.
     *
     * @param direction The current direction
     * @return The direction to the right
     */
    public static BlockFace goRight(BlockFace direction) {
        switch (direction) {
        case EAST:
            return BlockFace.SOUTH;
        case SOUTH:
            return BlockFace.WEST;
        case WEST:
            return BlockFace.NORTH;
        case NORTH:
            return BlockFace.EAST;
        default:
            return direction;
        }
    }

    public void removeLeaderboard() {
        Block leaderboardBlock = getLeaderboardBlock();
        if (leaderboardBlock != null && leaderboardFacing != null) {
            BlockFace rightDirection = goLeft(leaderboardFacing);
            for (int y = 0; y <= leaderboardSize; y++) {
                Block neighborBlock = leaderboardBlock.getRelative(rightDirection);
                clearLeaderboardBlock(neighborBlock);
                if (y != 0) {
                    clearLeaderboardBlock(leaderboardBlock);
                }
                leaderboardBlock = leaderboardBlock.getRelative(BlockFace.UP);
            }
        }
    }

    protected void trimLeaderboard() {
        removeLeaderboard();
        while (leaderboard.size() > leaderboardRecordSize) {
            leaderboard.remove(leaderboard.size() - 1);
        }
        updateLeaderboard();
    }

    public void setLeaderboardRecordSize(int size) {
        leaderboardRecordSize = size;
        if (leaderboardSize > leaderboardRecordSize) {
            leaderboardSize = leaderboardRecordSize;
        }
        trimLeaderboard();
    }

    public void setLeaderboardSize(int size) {
        leaderboardSize = size;
        if (leaderboardSize > leaderboardRecordSize) {
            leaderboardRecordSize = leaderboardSize;
        }
        trimLeaderboard();
    }

    public void setLeaderboardGamesRequired(int required) {
        leaderboardGamesRequired = required;
        Collection<ArenaPlayer> currentLeaderboard = new ArrayList<>(leaderboard);
        leaderboard.clear();
        for (ArenaPlayer player : currentLeaderboard) {
            updateLeaderboard(player);
        }
        updateLeaderboard();
    }

    public void describeStats(CommandSender sender, Player player) {
        ArenaPlayer arenaPlayer = new ArenaPlayer(this, player);

        int wins = arenaPlayer.getWins();
        int losses = arenaPlayer.getLosses();
        int draws = arenaPlayer.getDraws();
        int quits = arenaPlayer.getQuits();
        float ratio = arenaPlayer.getWinRatio();
        double confidence = arenaPlayer.getWinConfidence();

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
            sender.sendMessage(
                    ChatColor.DARK_PURPLE + player.getDisplayName()
                            + ChatColor.DARK_PURPLE
                            + " is ranked " + ChatColor.AQUA + "#"
                            + Integer.toString(rank) + ChatColor.DARK_PURPLE
                            + " for " + ChatColor.GOLD + getName());
        } else {
            sender.sendMessage(
                    ChatColor.DARK_PURPLE + player.getDisplayName()
                            + ChatColor.DARK_RED
                            + " is not on the leaderboard for "
                            + ChatColor.GOLD + getName());
        }

        Arena currentArena = controller.getArena(player);
        if (currentArena != null) {
            sender.sendMessage(ChatColor.DARK_PURPLE + player.getDisplayName() + ChatColor.LIGHT_PURPLE + " is currently in " + ChatColor.GOLD + currentArena.getName());
        }

        sender.sendMessage(ChatColor.GREEN + "Wins: " + ChatColor.WHITE + Integer.toString(wins));
        sender.sendMessage(ChatColor.RED + "Losses: " + ChatColor.WHITE + Integer.toString(losses));
        sender.sendMessage(ChatColor.GOLD + "Win Ratio: " + ChatColor.WHITE + Integer.toString((int)(ratio * 100)) + "% " + String.format("(%.2f)", confidence));
        sender.sendMessage(ChatColor.YELLOW + "Draws: " + ChatColor.WHITE + Integer.toString(draws));
        sender.sendMessage(ChatColor.GRAY + "Defaults: " + ChatColor.WHITE + Integer.toString(quits));
    }

    public void describeLeaderboard(CommandSender sender) {
        sender.sendMessage(
                ChatColor.GOLD + getName()
                        + ChatColor.YELLOW + " Leaderboard: ");
        sender.sendMessage(
                ChatColor.AQUA + Integer.toString(leaderboard.size())
                        + ChatColor.DARK_AQUA + " players with at least "
                        + ChatColor.AQUA
                        + Integer.toString(leaderboardGamesRequired)
                        + ChatColor.DARK_AQUA + " games:");
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

    public boolean placeLeaderboard(Block leaderboardBlock) {
        if (leaderboardBlock.getType() != Material.WALL_SIGN) {
            return false;
        }
        MaterialData signData = leaderboardBlock.getState().getData();
        if (!(signData instanceof Sign)) {
            controller.getPlugin().getLogger().warning("Block at " + leaderboardBlock.getLocation() + " has no sign data! " + signData.getClass());
            return false;
        }
        Sign sign = (Sign)signData;
        BlockFace signDirection = sign.getFacing();
        BlockFace rightDirection = goLeft(signDirection);
        Block checkBlock = leaderboardBlock;
        for (int y = 0; y <= leaderboardSize; y++) {
            Block neighborBlock = checkBlock.getRelative(rightDirection);
            if (!canReplace(neighborBlock)) {
                return false;
            }
            if (y != 0 && !canReplace(checkBlock)) {
                return false;
            }
            checkBlock = checkBlock.getRelative(BlockFace.UP);
        }

        removeLeaderboard();
        leaderboardLocation = leaderboardBlock.getLocation();
        leaderboardFacing = signDirection;
        updateLeaderboard();

        return true;
    }

    public int getLeaderboardSize() {
        return leaderboardSize;
    }

    public void reset(Player player) {
        removePlayer(player);
        ArenaPlayer arenaPlayer = new ArenaPlayer(this, player);
        removeFromLeaderboard(arenaPlayer);
        // Note that we don't rebuild the leaderboard here, just let that happen later.
        // You can force a rebuild with a break and re-place of the block
        arenaPlayer.reset();
    }

    public void reset() {
        // TODO.. need to cycle the id or something :(
    }

    public void setWinXP(int xp) {
        winXP = Math.max(xp, 0);
    }

    public void setLoseXP(int xp) {
        loseXP = Math.max(xp, 0);
    }

    public void setDrawXP(int xp) {
        drawXP = Math.max(xp, 0);
    }

    public int getWinXP() {
        return winXP;
    }

    public int getLoseXP() {
        return loseXP;
    }

    public int getDrawXP() {
        return drawXP;
    }

    public void setWinSP(int sp) {
        winSP = Math.max(sp, 0);
    }

    public void setLoseSP(int sp) {
        loseSP = Math.max(sp, 0);
    }

    public void setDrawSP(int sp) {
        drawSP = Math.max(sp, 0);
    }

    public int getWinSP() {
        return winSP;
    }

    public int getLoseSP() {
        return loseSP;
    }

    public int getDrawSP() {
        return drawSP;
    }

    public void setWinMoney(int money) {
        winMoney = Math.max(money, 0);
    }

    public void setLoseMoney(int money) {
        loseMoney = Math.max(money, 0);
    }

    public void setDrawMoney(int money) {
        drawMoney = Math.max(money, 0);
    }

    public int getWinMoney() {
        return winMoney;
    }

    public int getLoseMoney() {
        return loseMoney;
    }

    public int getDrawMoney() {
        return drawMoney;
    }

    public void setMaxTeleportDistance(int distance) {
        maxTeleportDistance = distance;
    }

    public int getMaxTeleportDistance() {
        return maxTeleportDistance;
    }

    public void showLeaderboard(Player player) {
        int inventorySize = leaderboard.size() + 1;
        int leaderboardRows = (int)Math.ceil((float)inventorySize / 9);
        leaderboardRows = Math.min(8, leaderboardRows);
        inventorySize = leaderboardRows * 9;
        boolean shownPlayer = false;
        String arenaName = ChatColor.DARK_AQUA + "Leaderboard: " + ChatColor.GOLD + getName();
        if (arenaName.length() > 32) {
            arenaName = arenaName.substring(0, 31);
        }
        Inventory leaderboardInventory = Bukkit.createInventory(null, inventorySize, arenaName);
        int leaderboardSize = Math.min(leaderboard.size(), inventorySize);
        for (int i = 0; i < leaderboardSize; i++) {
            ArenaPlayer arenaPlayer = leaderboard.get(i);
            final int slot = i;
            createLeaderboardIcon(i + 1, arenaPlayer, new ItemUpdatedCallback() {
                @Override
                public void updated(ItemStack itemStack) {
                    leaderboardInventory.setItem(slot, itemStack);
                }
            });
            if (player.getUniqueId().equals(arenaPlayer.getUUID())) {
                shownPlayer = true;
            }
        }

        if (!shownPlayer && leaderboardSize > 0) {
            ArenaPlayer arenaPlayer = new ArenaPlayer(this, player);
            createLeaderboardIcon(null, arenaPlayer, new ItemUpdatedCallback() {
                @Override
                public void updated(ItemStack itemStack) {
                    leaderboardInventory.setItem(leaderboardSize - 1, itemStack);
                }
            });
        }

        player.openInventory(leaderboardInventory);
    }

    protected void createLeaderboardIcon(Integer rank, ArenaPlayer player, ItemUpdatedCallback callback) {
        controller.getMagic().getSkull(
                player.getUUID(),
                ChatColor.GOLD + player.getDisplayName(),
                new ItemUpdatedCallback() {
                    @Override
                    public void updated(ItemStack itemStack) {
                        ItemMeta meta = itemStack.getItemMeta();
                        List<String> lore = new ArrayList<>();

                        if (rank != null) {
                            lore.add(ChatColor.DARK_PURPLE + "Ranked " + ChatColor.AQUA + "#" + Integer.toString(rank) + ChatColor.DARK_PURPLE + " for " + ChatColor.GOLD + getName());
                        } else {
                            lore.add(ChatColor.DARK_PURPLE + "Not ranked for " + ChatColor.GOLD + getName());
                        }

                        lore.add(ChatColor.GREEN + "Wins: " + ChatColor.WHITE + Integer.toString(player.getWins()));
                        lore.add(ChatColor.RED + "Losses: " + ChatColor.WHITE + Integer.toString(player.getLosses()));
                        lore.add(ChatColor.GOLD + "Win Ratio: " + ChatColor.WHITE + Integer.toString((int) (player.getWinRatio() * 100)) + "% " + String.format("(%.2f)", player.getWinConfidence()));
                        lore.add(ChatColor.YELLOW + "Draws: " + ChatColor.WHITE + Integer.toString(player.getDraws()));
                        lore.add(ChatColor.GRAY + "Defaults: " + ChatColor.WHITE + Integer.toString(player.getQuits()));
                        meta.setLore(lore);
                        itemStack.setItemMeta(meta);
                        callback.updated(itemStack);
                    }
                });
    }

    public void setCountdown(int countdown) {
        this.countdown = countdown;
    }

    public void setCountdownMax(int countdownMax) {
        this.countdownMax = countdownMax;
    }

    public boolean hasOpCheck() {
        return opCheck;
    }

    public void setOpCheck(boolean check) {
        opCheck = check;
    }

    public void setKeepInventory(boolean keep) {
        keepInventory = keep;
    }

    public void setKeepLevel(boolean keep) {
        keepLevel = keep;
    }

    public void setAnnouncerRange(int range) {
        this.announcerRange = range;
    }

    public int getAnnouncerRange() {
        return announcerRange;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setSuddenDeath(int suddenDeath) {
        this.suddenDeath = suddenDeath;
    }

    public void setStartCommands(String commands) {
        startCommands = commands;
    }

    public void setBorder(int min, int max) {
        borderMin = min;
        borderMax = max;
    }

    public void tick() {
        if (duration <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long previousTime = lastTick - started;
        long currentTime = now - started;
        lastTick = now;

        if (currentTime > duration) {
            announce(ChatColor.GRAY + "The " + ChatColor.YELLOW + getName() + ChatColor.GRAY + " match timed out in a draw");
            for (ArenaPlayer player : players) {
                player.draw();
                player.heal();
            }
            for (ArenaPlayer loser : deadPlayers) {
                loser.draw();
                loser.heal();
            }
            finish();
            return;
        }

        boolean hasSuddenDeath = suddenDeath > 0 && suddenDeathEffect != null && suddenDeath < duration;
        if (currentTime >= duration - 120000 && previousTime < duration - 1200000) {
            announce(ChatColor.GOLD + "The " + ChatColor.YELLOW + getName() + ChatColor.GOLD + " match will "
                + ChatColor.RED + "END" + ChatColor.GOLD + " in " + ChatColor.RED + "two minutes!");
        }
        if (currentTime >= duration - 60000 && previousTime < duration - 60000) {
            announce(ChatColor.GOLD + "The " + ChatColor.YELLOW + getName() + ChatColor.GOLD + " match will "
                    + ChatColor.RED + "END" + ChatColor.GOLD + " in " + ChatColor.RED + "one minute!");
        }
        if (currentTime >= duration - 30000 && previousTime < duration - 30000) {
            announce(ChatColor.GOLD + "The " + ChatColor.YELLOW + getName() + ChatColor.GOLD + " match will "
                    + ChatColor.RED + "END" + ChatColor.GOLD + " in " + ChatColor.RED + "thirty seconds!");
        }
        if (currentTime >= duration - 10000 && previousTime < duration - 10000) {
            announce(ChatColor.GOLD + "The " + ChatColor.YELLOW + getName() + ChatColor.GOLD + " match will "
                    + ChatColor.RED + "END" + ChatColor.GOLD + " in " + ChatColor.RED + "ten seconds!");
        }
        if (currentTime >= duration - 5000 && previousTime < duration - 5000) {
            announce(ChatColor.GOLD + "The " + ChatColor.YELLOW + getName() + ChatColor.GOLD + " match will "
                    + ChatColor.RED + "END" + ChatColor.GOLD + " in " + ChatColor.RED + "five seconds!");
        }

        if (hasSuddenDeath) {
            long suddenDeathDuration = duration - suddenDeath;
            if (currentTime >= suddenDeathDuration) {
                if (previousTime < suddenDeathDuration) {
                    announce(ChatColor.RED + "SUDDEN DEATH!");
                }
                for (ArenaPlayer player : players) {
                    player.getPlayer().addPotionEffect(suddenDeathEffect, true);
                }
            }
        }
    }

    public boolean isValid() {
        return center != null && center.getWorld() != null;
    }

    public boolean isKeepInventory() {
        return keepInventory;
    }

    public boolean isKeepLevel() {
        return keepLevel;
    }

    public boolean isMobArena() {
        ArenaStage currentStage = getCurrentStage();
        if (currentStage == null) {
            return false;
        }
        return currentStage.hasMobs();
    }

    public Mage getMage() {
       return controller.getMagic().getMage("ARENA: " + getKey(), getName());
    }
}
