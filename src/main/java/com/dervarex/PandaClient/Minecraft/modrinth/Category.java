package com.dervarex.PandaClient.Minecraft.modrinth;

/**
 * Modrinth project categories (subset). Each enum maps to the API category string.
 * Full list: https://docs.modrinth.com/docs/api-spec/#tag/categories
 */
public enum Category {
    ADVENTURE("adventure"),
    MAGIC("magic"),
    TECHNOLOGY("technology"),
    UTILITY("utility"),
    FOOD("food"),
    DECORATION("decoration"),
    EQUIPMENT("equipment"),
    WORLDGEN("worldgen"),
    LIBRARY("library"),
    MISC("misc"),
    PERFORMANCE("performance"),
    OPTIMIZATION("optimization"),
    SERVER("server"),
    CHAT("chat"),
    MANAGEMENT("management"),
    STORAGE("storage"),
    ENERGY("energy"),
    MAGIC_TECH("magic-tech"),
    TRANSPORTATION("transportation"),
    TOOLS("tools"),
    ;

    private final String apiValue;
    Category(String apiValue){ this.apiValue = apiValue; }
    public String apiValue(){ return apiValue; }
}

