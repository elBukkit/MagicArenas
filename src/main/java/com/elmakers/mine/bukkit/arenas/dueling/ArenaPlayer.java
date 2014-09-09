package com.elmakers.mine.bukkit.arenas.dueling;

import org.bukkit.entity.Player;

import java.lang.ref.WeakReference;
import java.util.UUID;

public class ArenaPlayer {
    private final WeakReference<Player> playerReference;
    private final Arena arena;
    private final UUID uuid;
    private final String name;
    private final String displayName;

    private int wins;
    private int losses;
    private int quits;
    private int joins;
    private int draws;

    public ArenaPlayer(Arena arena, Player player) {
        playerReference = new WeakReference<Player>(player);
        uuid = player.getUniqueId();
        name = player.getName();
        displayName = player.getDisplayName();
        this.arena = arena;

        wins = arena.get(player, "won");
        losses = arena.get(player, "lost");
        quits = arena.get(player, "quit");
        joins = arena.get(player, "joined");
        draws = arena.get(player, "draw");
    }

    public boolean isValid() {
        Player player = playerReference.get();
        return player != null && player.isOnline();
    }

    public boolean isDead() {
        Player player = playerReference.get();
        return player != null && player.isDead();
    }

    public Player getPlayer() {
        return playerReference.get();
    }

    public String getDisplayName() {
        return displayName;
    }

    public void won() {
        Player player = getPlayer();
        if (player != null) {
            arena.increment(player, "won");
            wins = arena.get(player, "won");
        }
    }

    public void lost() {
        Player player = getPlayer();
        if (player != null) {
            arena.increment(player, "lost");
            losses = arena.get(player, "lost");
        }
    }

    public void quit() {
        Player player = getPlayer();
        if (player != null) {
            arena.increment(player, "quit");
            quits = arena.get(player, "quit");
        }
    }

    public void joined() {
        Player player = getPlayer();
        if (player != null) {
            arena.increment(player, "joined");
            joins = arena.get(player, "joined");
        }
    }

    public void draw() {
        Player player = getPlayer();
        if (player != null) {
            arena.increment(player, "draw");
            draws = arena.get(player, "draw");
        }
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getQuits() {
        return quits;
    }

    public int getJoins() {
        return joins;
    }

    public int getDraws() {
        return draws;
    }

    public boolean equals(Player player) {
        return (player != null && player.getUniqueId().equals(uuid));
    }
}
