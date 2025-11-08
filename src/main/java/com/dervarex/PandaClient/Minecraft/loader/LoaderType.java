package com.dervarex.PandaClient.Minecraft.loader;

public enum LoaderType {
    VANILLA,
    FABRIC,
    UNKNOWN;

    public static LoaderType fromString(String loader) {
        for (LoaderType type : LoaderType.values()) {
            if (type.name().equalsIgnoreCase(loader)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
