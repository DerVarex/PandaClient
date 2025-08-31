package com.dervarex.PandaClient.utils.OS;

public class OSUtil {
        public enum OS {
            WINDOWS,
            MAC,
            LINUX,
            UNKNOWN
        }

        private static final OS CURRENT_OS = detectOS();

        private static OS detectOS() {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) return OS.WINDOWS;
            if (os.contains("mac")) return OS.MAC;
            if (os.contains("nux") || os.contains("nix")) return OS.LINUX;
            return OS.UNKNOWN;
        }

        public static OS getOS() {
            return CURRENT_OS;
        }
}
