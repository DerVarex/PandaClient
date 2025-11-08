package com.dervarex.PandaClient.GUI.WebSocket.logstate;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LogstateServer extends WebSocketServer {

    private static LogstateServer instance = null;
    private static final List<String> pending = new ArrayList<>();

    public LogstateServer(int port) {
        super(new InetSocketAddress(port));
        instance = this;
        flushPending();
    }

    private static synchronized void flushPending() {
        if (instance == null) return;
        for (String json : pending) instance.broadcast(json);
        pending.clear();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("[Logstate] Client connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Falls dein Electron-Frontend irgendwas sendet:
        System.out.println("[Logstate] From frontend: " + message);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("[Logstate] Connection closed: " + reason);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[Logstate] WebSocket error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("[Logstate] Server running on ws://127.0.0.1:9999");
        flushPending();
    }

    private static String buildLogJson(String level, String where, String message, String timestampIso) {
        String safeLevel = level == null ? "INFO" : level.replace("\"", "\\\"");
        String safeWhere = where == null ? "" : where.replace("\"", "\\\"");
        String safeMessage = message == null ? "" : message.replace("\"", "\\\"");
        String safeTs = timestampIso == null ? DateTimeFormatter.ISO_INSTANT.format(Instant.now()) : timestampIso;

        return String.format(
                "{\"action\":\"call\",\"function\":\"logstate\",\"args\":[\"%s\",\"%s\",\"%s\",\"%s\"]}",
                safeLevel, safeWhere, safeMessage, safeTs
        );
    }

    public void sendLog(String level, String where, String message) {
        String json = buildLogJson(level, where, message, null);
        broadcast(json);
    }

    public static synchronized void notifyLog(String level, String where, String message) {
        String json = buildLogJson(level, where, message, null);
        if (instance != null) instance.broadcast(json);
        else pending.add(json);
    }


    public static LogstateServer getInstance() {
        return instance;
    }
}
