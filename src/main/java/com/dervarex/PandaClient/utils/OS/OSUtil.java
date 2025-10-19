package com.dervarex.PandaClient.utils.OS;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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

    public static boolean openBrowser(String url) {
            OSUtil.OS os = OSUtil.getOS();
            if(os == OS.WINDOWS) {
                Runtime rt = Runtime.getRuntime();
                try {
                    rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
                    return true;
                } catch (IOException e) {
//                    throw new RuntimeException(e);
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
                // Prefer xdg-open, then fall back to common browsers
                String[] browsers = { "xdg-open", "firedragon", "google-chrome", "chromium", "brave-browser", "firefox", "mozilla", "epiphany", "konqueror",
                        "netscape", "opera", "vivaldi", "falkon", "links", "lynx" };


                StringBuilder cmd = new StringBuilder();
                for (int i = 0; i < browsers.length; i++)
                    if(i == 0)
                        cmd.append(String.format(    "%s \"%s\"", browsers[i], url));
                    else
                        cmd.append(String.format(" || %s \"%s\"", browsers[i], url));
                // If the first didn't work, try the next browser and so on

                try {
                    rt.exec(new String[] { "sh", "-c", cmd.toString() });
                    return true;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else { return false; }
    }
}
