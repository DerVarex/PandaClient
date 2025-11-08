package com.dervarex.PandaClient.utils.OS;

public enum OS {
    WINDOWS,
    MAC,
    LINUX,
    UNKNOWN;
    public static String getName(OS os) {
        switch (os) {
            case WINDOWS:
                return "Windows";
            case MAC:
                return "OSX";
            case LINUX:
                return "Linux";
            default:
                return "Unknown";
        }
    }
}
