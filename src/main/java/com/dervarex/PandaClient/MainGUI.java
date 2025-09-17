package com.dervarex.PandaClient;

import com.dervarex.PandaClient.GUI.ModServer;
import com.dervarex.PandaClient.GUI.NotificationServer;
import com.dervarex.PandaClient.GUI.NotificationServerStart;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;

public class MainGUI {
    // Starts the GUI and Initiales processes
    public MainGUI() {
        ModServer server = new ModServer(8800);
        try {
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            System.out.println("Server läuft auf http://localhost:8800");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Electron starten
        try {
            String npmCommand = System.getProperty("os.name").toLowerCase().contains("win") ? "npm.cmd" : "npm";
            ProcessBuilder pb = new ProcessBuilder(npmCommand, "start");
            pb.directory(new java.io.File("electron"));
            pb.inheritIO();
            pb.start();
            System.out.println("Electron gestartet");
        } catch (Exception e) {
            e.printStackTrace();
        }
        NotificationServerStart.startNotificationServer();
    }
}
