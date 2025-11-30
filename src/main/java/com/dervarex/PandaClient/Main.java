package com.dervarex.PandaClient;

import com.dervarex.PandaClient.Minecraft.logger.ClientLogger;
import com.dervarex.PandaClient.server.ServerConfig;
import com.dervarex.PandaClient.server.ui.CreateServer;
import com.dervarex.PandaClient.utils.file.getPandaClientFolder;
import de.Huskthedev.HusksStuff.DiscordRPC.DiscordManager;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Main {
    ServerConfig serverConfig = new ServerConfig();
    public static ServerConfig getServerConfig() {
        return new ServerConfig();
    }

    public static void main(String[] args) {
        // Start logger early so everything can be recorded
        ClientLogger.start();

        String home = getPandaClientFolder.getPandaClientFolder().getAbsolutePath();
        File defaultLogFile = new File(home + File.separator + "logs" + File.separator + "client-log.json");
        File parent = defaultLogFile.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        // Configure default file for ClientLogger so it auto-saves while running
        ClientLogger.setDefaultLogFile(defaultLogFile);

        // Register shutdown hook to save logs on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                ClientLogger.save(defaultLogFile);
            } catch (Throwable t) {
                StringBuilder sb = new StringBuilder();
                for (StackTraceElement ste : t.getStackTrace()) sb.append(ste.toString()).append('\n');
                ClientLogger.log("Shutdown hook error: " + sb.toString(), "ERROR", "Main");
            }
        }));

        // Start Discord Rich Presence
        try {
            DiscordManager.start();
            ClientLogger.log("Discord RPC started", "INFO", "Main");
        } catch (Throwable t) {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement ste : t.getStackTrace()) sb.append(ste.toString()).append('\n');
            ClientLogger.log("Discord RPC failed: " + sb.toString(), "WARN", "Main");
        }

        // The GUI
        new MainGUI();
    }
}