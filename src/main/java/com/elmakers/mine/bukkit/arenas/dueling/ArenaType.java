package com.elmakers.mine.bukkit.arenas.dueling;

import org.bukkit.ChatColor;

public enum ArenaType {
    FFA,
    SPLEEF,
    ONEVONE,
    TWOVTWO,
    THREEVTHREE,
    FOURVFOUR;

    public static ArenaType parse(String fromString) {
        ArenaType arenaType = null;
        try {
            arenaType = ArenaType.valueOf(fromString.toUpperCase());
        } catch (Exception ex) {
            return null;
        }

        return arenaType;
    }
}
