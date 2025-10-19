package com.dervarex.PandaClient.GUI;

import com.dervarex.PandaClient.Minecraft.logger.ClientLogger;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class NotificationServer extends WebSocketServer {

    public enum NotificationType {
        INFO, WARNING, ERROR
    }

    // static instance so other classes can call notifications safely
    private static NotificationServer instance = null;

    // buffer for notifications sent before the server instance exists
    private static final List<PendingNotification> pending = new ArrayList<>();

    private static class PendingNotification {
        final NotificationType type;
        final String message;

        PendingNotification(NotificationType type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    public NotificationServer(int port) {
        super(new InetSocketAddress(port));
        // keep a reference to the created instance
        instance = this;
        // flush any pending notifications now that instance exists
        flushPending();
    }

    private static synchronized void flushPending() {
        if (instance == null) return;
        for (PendingNotification p : pending) {
            instance.showNotification(p.type, p.message);
        }
        pending.clear();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        ClientLogger.log("Client connected: " + conn.getRemoteSocketAddress(), "INFO", "NotificationServer");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        ClientLogger.log("From JS: " + message, "INFO", "NotificationServer");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        ClientLogger.log("Connection closed: " + reason, "INFO", "NotificationServer");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ClientLogger.log("WebSocket error: " + ex.getMessage(), "ERROR", "NotificationServer");
    }

    @Override
    public void onStart() {
        ClientLogger.log("NotificationServer started and running", "INFO", "NotificationServer");
        // ensure any pending notifications are flushed once started
        flushPending();
    }

    // Hauptmethode: JS-Funktion showNotification(type, message) aufrufen
    public void showNotification(NotificationType type, String message) {
        String json = String.format(
                "{\"action\":\"call\",\"function\":\"showNotification\",\"args\":[\"%s\",\"%s\"]}",
                type.name(), message.replace("\"", "\\\"")
        );
        broadcast(json);
        ClientLogger.log("Notification sent: " + json, "INFO", "NotificationServer");
    }

    // Static helper so other static contexts can notify without needing a reference
    public static synchronized void notify(NotificationType type, String message) {
        if (instance != null) {
            instance.showNotification(type, message);
        } else {
            // buffer until instance exists
            pending.add(new PendingNotification(type, message));
            // also log so messages are visible in logs
            ClientLogger.log("[NotificationServer] queued - " + type.name() + ": " + message, "WARN", "NotificationServer");
        }
    }

    // Optional getter for the instance if someone needs the concrete server
    public static NotificationServer getInstance() {
        return instance;
    }
}
