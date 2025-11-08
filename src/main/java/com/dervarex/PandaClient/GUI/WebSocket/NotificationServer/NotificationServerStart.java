package com.dervarex.PandaClient.GUI.WebSocket.NotificationServer;

import com.dervarex.PandaClient.Minecraft.logger.ClientLogger;

public class NotificationServerStart {

    private static NotificationServer notificationServer;

    public static void startNotificationServer() {
        notificationServer = new NotificationServer(8888) {
            @Override
            public void onOpen(org.java_websocket.WebSocket conn,
                               org.java_websocket.handshake.ClientHandshake handshake) {
                super.onOpen(conn, handshake);
                // Notification kann hier gesendet werden
                // this.showNotification(NotificationType.INFO, "INFO: Verbunden mit Java NotificationServer");
            }
        };

        try {
            notificationServer.start();
            ClientLogger.log("NotificationServer running on ws://127.0.0.1:8888", "INFO", "NotificationServerStart");
        } catch (Exception e) {
            ClientLogger.log("NotificationServer failed: " + e.getMessage(), "ERROR", "NotificationServerStart");
        }
    }

    // getter
    public static NotificationServer getNotificationServer() {
        return notificationServer;
    }
}
