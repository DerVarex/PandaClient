package com.dervarex.PandaClient.worldedit;

import java.util.HashMap;
import java.util.Map;

public enum Difficulty {
    PEACEFUL("peaceful"),
    EASY("easy"),
    NORMAL("normal"),
    HARD("hard"),
    HARDCORE("hardcore");

    private final String name;

    private static final Map<String, Difficulty> STRING_MAP = new HashMap<>();

    static {
        for (Difficulty d : values()) {
            STRING_MAP.put(d.name, d);
        }
    }

    Difficulty(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Difficulty fromString(String str) {
        return STRING_MAP.getOrDefault(str.toLowerCase(), NORMAL);
    }

    public static Difficulty fromInt(int num) {
        Difficulty[] values = Difficulty.values();
        if (num < 0 || num >= values.length) return NORMAL;
        return values[num];
    }
}
