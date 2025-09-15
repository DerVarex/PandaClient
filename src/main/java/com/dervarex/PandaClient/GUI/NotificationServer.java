package com.dervarex.PandaClient.GUI;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.util.EnumSet;

public class NotificationServer extends WebSocketServer {

    public enum NotificationType {
        INFO, WARNING, ERROR
    }

    public NotificationServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Client verbunden: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Von JS empfangen: " + message);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Verbindung geschlossen: " + reason);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("NotificationServer gestartet und läuft!");
    }

    // Hauptmethode: JS-Funktion showNotification(type, message) aufrufen
    public void showNotification(NotificationType type, String message) {
        String json = String.format(
                "{\"action\":\"call\",\"function\":\"showNotification\",\"args\":[\"%s\",\"%s\"]}",
                type.name(), message.replace("\"", "\\\"")
        );
        broadcast(json);
        System.out.println("Gesendet: " + json);
    }
}
