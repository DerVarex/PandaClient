package com.dervarex.PandaClient.GUI;

public class NotificationServerStart {

    private static NotificationServer notificationServer;

    public static void startNotificationServer() {
        notificationServer = new NotificationServer(8888) {
            @Override
            public void onOpen(org.java_websocket.WebSocket conn,
                               org.java_websocket.handshake.ClientHandshake handshake) {
                super.onOpen(conn, handshake);
                // Notification kann hier gesendet werden
                // this.showNotification(NotificationType.INFO, "INFO: Verbunden mit Java NotificationServer 🚀");
            }
        };

        try {
            notificationServer.start();
            System.out.println("NotificationServer läuft auf ws://127.0.0.1:8888");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // getter
    public static NotificationServer getNotificationServer() {
        return notificationServer;
    }
}
