package com.dervarex.PandaClient.worldedit;

public enum Gamemode {
    SURVIVAL,
    CREATIVE,
    ADVENTURE,
    SPECTATOR;

    public String toStringValue() {
        return switch (this) {
            case SURVIVAL -> "survival";
            case CREATIVE -> "creative";
            case ADVENTURE -> "adventure";
            case SPECTATOR -> "spectator";
            default -> "unknown";
        };
    }

    public static Gamemode fromInt(int num) {
        return switch (num) {
            case 0 -> SURVIVAL;
            case 1 -> CREATIVE;
            case 2 -> ADVENTURE;
            case 3 -> SPECTATOR;
            default -> SURVIVAL; // Default fallback
        };
    }
    public static int toInt(Gamemode gm) {
        return switch (gm) {
            case SURVIVAL -> 0;
            case CREATIVE -> 1;
            case ADVENTURE -> 2;
            case SPECTATOR -> 3;
            // Default fallback
        };
    }

    public static Gamemode fromString(String str) {
        return switch (str.toLowerCase()) {
            case "creative" -> CREATIVE;
            case "adventure" -> ADVENTURE;
            case "spectator" -> SPECTATOR;
            default -> SURVIVAL;
        };
    }
}
