package com.dervarex.PandaClient.server;

import java.io.IOException;

public enum ServerType {

    VANILLA("Vanilla") {
        @Override
        public String getFileName(String version) {
            return "vanilla-" + version + ".jar";
        }

        @Override
        public String getDownloadUrl(String version) {
            // Example: https://www.mcjars.com/get/vanilla-1.21.10.jar
            return "https://www.mcjars.com/get/vanilla-" + version + ".jar";
        }
    },

    PAPER("Paper") {
        @Override
        public String getFileName(String version) throws IOException {
            int build = PaperMC.getLatestBuild(version);
            return "paper-" + version + "-" + build + ".jar";
        }

        @Override
        public String getDownloadUrl(String version) throws IOException {
            return PaperMC.getDownloadUrl(version);
        }
    },

    FOLIA("Folia") {
        @Override
        public String getFileName(String version) throws IOException {
            int build = Folia.getLatestBuild(version);
            return "folia-" + version + "-" + build + ".jar";
        }

        @Override
        public String getDownloadUrl(String version) throws IOException {
            return Folia.getDownloadUrl(version);
        }
    };

    private final String displayName;

    ServerType(String displayName) {
        this.displayName = displayName;
    }

    // =============================
    // PUBLIC API
    // =============================

    public static ServerType fromName(String name) {
        for (ServerType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return VANILLA; // default
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gibt den Dateinamen zurück, unter dem die JAR gespeichert werden soll.
     * Beispiel:
     *  vanilla-1.21.8.jar
     *  paper-1.21.8-123.jar
     */
    public abstract String getFileName(String version) throws IOException;

    /**
     * Download-URL für den Server-Typ.
     */
    public abstract String getDownloadUrl(String version) throws IOException;
}
