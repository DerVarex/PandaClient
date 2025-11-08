package com.dervarex.PandaClient.utils.OS;

import java.io.IOException;
import java.nio.file.Path;

public class OSUtil {
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

    public static boolean openBrowser(String url) {
        OS os = getOS();
        if (os == OS.WINDOWS) {
            Runtime rt = Runtime.getRuntime();
            try {
                rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
                return true;
            } catch (IOException e) {
                return false;
            }
        } else if (os == OS.MAC) {
            Runtime rt = Runtime.getRuntime();
            try {
                rt.exec("open " + url);
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (os == OS.LINUX) {
            Runtime rt = Runtime.getRuntime();
            String[] browsers = { "xdg-open", "firedragon", "google-chrome", "chromium", "brave-browser", "firefox", "mozilla", "epiphany", "konqueror",
                    "netscape", "opera", "vivaldi", "falkon", "links", "lynx" };

            StringBuilder cmd = new StringBuilder();
            for (int i = 0; i < browsers.length; i++) {
                if (i == 0)
                    cmd.append(String.format("%s \"%s\"", browsers[i], url));
                else
                    cmd.append(String.format(" || %s \"%s\"", browsers[i], url));
            }

            try {
                rt.exec(new String[] { "sh", "-c", cmd.toString() });
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return false;
        }
    }
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
    public static Path getRunningJavaDir(){
        return Path.of(System.getProperty("java.home"));
    }

    public static Path getJavaFile(String root, boolean preferWindow){
        if (CURRENT_OS == OS.WINDOWS){
            Path winw = Path.of(root, "bin", "javaw.exe");
            Path win = Path.of(root, "bin", "java.exe");
            if (preferWindow && java.nio.file.Files.exists(winw)) return winw;
            if (java.nio.file.Files.exists(win)) return win;
            if (java.nio.file.Files.exists(winw)) return winw;
            // fall back to best-effort path
            return win;
        }
        else {
            // common Unix-like candidate locations
            Path p1 = Path.of(root, "bin", "java");
            Path p2 = Path.of(root, "jre", "bin", "java");
            Path p3 = Path.of(root, "Contents", "Home", "bin", "java");
            if (java.nio.file.Files.exists(p1)) return p1;
            if (java.nio.file.Files.exists(p2)) return p2;
            if (java.nio.file.Files.exists(p3)) return p3;
            // if none exist, return the primary bin/java path as default
            return p1;
        }
    }
}
