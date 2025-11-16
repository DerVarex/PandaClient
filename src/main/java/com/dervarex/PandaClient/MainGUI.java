package com.dervarex.PandaClient;

import com.dervarex.PandaClient.GUI.ModServer;
import com.dervarex.PandaClient.GUI.WebSocket.NotificationServer.NotificationServerStart;
import com.dervarex.PandaClient.Minecraft.logger.ClientLogger;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;

public class MainGUI {
    // Starts the GUI and Initiales processes :)
    public MainGUI() {
        ModServer server = new ModServer(8800);
        try {
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            ClientLogger.log("Server running on http://localhost:8800", "INFO", "MainGUI");
        } catch (IOException e) {
            ClientLogger.log("Server start failed: " + e.getMessage(), "ERROR", "MainGUI");
            throw new RuntimeException(e);
        }
        // Start electron
        try {
            String npmCommand = System.getProperty("os.name").toLowerCase().contains("win") ? "npm.cmd" : "npm";
            ProcessBuilder pb = new ProcessBuilder(npmCommand, "start");
            pb.directory(new java.io.File("electron"));
            pb.inheritIO();
            pb.start();
            ClientLogger.log("Electron started", "INFO", "MainGUI");
        } catch (Exception e) {
            ClientLogger.log("Electron start failed: " + e.getMessage(), "ERROR", "MainGUI");
        }
        NotificationServerStart.startNotificationServer();
        ClientLogger.log("Notification server started", "INFO", "MainGUI");
    }
}
