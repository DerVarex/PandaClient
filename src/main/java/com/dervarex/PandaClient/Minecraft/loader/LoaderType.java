package com.dervarex.PandaClient.Minecraft.loader;

import java.util.Locale;

public enum LoaderType {
    VANILLA,
    FABRIC,
    UNKNOWN;
    // FORGE, etc.

    public static LoaderType fromString(String s) {
        if (s == null) return UNKNOWN;
        try {
            return LoaderType.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
    public static String toString(LoaderType type) {
        return type.toString().toLowerCase(Locale.ROOT);
    }
}