package com.elmakers.mine.bukkit.arenas.dueling;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event that is called when a player joins (the queue of) an arena.
 */
public class PlayerJoinedArenaEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();

    private final Player player;
    private final ArenaPlayer arenaPlayer;
    private final Arena arena;

    public PlayerJoinedArenaEvent(
            Player player,
            ArenaPlayer arenaPlayer,
            Arena arena) {
        this.player = checkNotNull(player, "player");
        this.arenaPlayer = checkNotNull(arenaPlayer, "arenaPlayer");
        this.arena = checkNotNull(arena, "arena");
    }

    public Player getPlayer() {
        return player;
    }

    public ArenaPlayer getArenaPlayer() {
        return arenaPlayer;
    }

    public Arena getArena() {
        return arena;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
