package com.dervarex.PandaClient.GUI.WebSocket.logstate;

import com.dervarex.PandaClient.Minecraft.logger.ClientLogger;

public class LogstateServerStart {

    private static LogstateServer logstateServer;

    public static void startNotificationServer() {
        logstateServer = new LogstateServer(9999) {
            @Override
            public void onOpen(org.java_websocket.WebSocket conn,
                               org.java_websocket.handshake.ClientHandshake handshake) {
                super.onOpen(conn, handshake);
            }
        };

        try {
            logstateServer.start();
            ClientLogger.log("NotificationServer running on ws://127.0.0.1:9999", "INFO", "LogstateServerStart");
        } catch (Exception e) {
            ClientLogger.log("LogstateServer failed: " + e.getMessage(), "ERROR", "LogstateServerStart");
        }
    }

    // getter
    public static LogstateServer getLogstateServer() {
        return logstateServer;
    }
}
