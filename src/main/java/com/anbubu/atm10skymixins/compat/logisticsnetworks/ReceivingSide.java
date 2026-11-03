package com.anbubu.atm10skymixins.compat.logisticsnetworks;

import java.util.Locale;

/** Side values mirrored from LogisticsNetworks' IoSide enum. */
public enum ReceivingSide {
    DOWN, UP, NORTH, SOUTH, WEST, EAST, ALL, NONE;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ReceivingSide parse(String value) {
        if (value == null) return null;
        for (ReceivingSide side : values()) {
            if (side.key().equals(value)) return side;
        }
        return null;
    }

    public static ReceivingSide next(ReceivingSide current) {
        int next = current == null ? 0 : current.ordinal() + 1;
        return next < values().length ? values()[next] : null;
    }

    public String mark() {
        return switch (this) {
            case DOWN -> "D";
            case UP -> "U";
            case NORTH -> "N";
            case SOUTH -> "S";
            case WEST -> "W";
            case EAST -> "E";
            case ALL -> "A";
            case NONE -> "0";
        };
    }
}
