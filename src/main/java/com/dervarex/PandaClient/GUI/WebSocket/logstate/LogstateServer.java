package com.dervarex.PandaClient.GUI.WebSocket.logstate;

import com.dervarex.PandaClient.Minecraft.logger.ClientLogger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LogstateServer extends WebSocketServer {

    // keep static instance so static contexts can publish logs
    private static LogstateServer instance = null;

    // buffer logs until the server instance is ready
    private static final List<String> pending = new ArrayList<>();

    public LogstateServer(int port) {
        super(new InetSocketAddress(port));
        instance = this;
        LogstateServer.flushPending();
    }

    private static synchronized void flushPending() {
        if (instance == null) return;
        for (String json : pending) instance.broadcast(json);
        pending.clear();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        ClientLogger.log("Client connected: " + conn.getRemoteSocketAddress(), "INFO", "LogstateServer");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        ClientLogger.log("From JS: " + message, "INFO", "LogstateServer");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        ClientLogger.log("Connection closed: " + reason, "INFO", "LogstateServer");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ClientLogger.log("WebSocket error: " + ex.getMessage(), "ERROR", "LogstateServer");
    }

    @Override
    public void onStart() {
        ClientLogger.log("LogstateServer started and running", "INFO", "LogstateServer");
        flushPending();
    }

    // Build the JSON payload once
    private static String buildLogJson(String level, String where, String message, String timestampIso) {
        String safeMsg = message == null ? "" : message.replace("\\", "\\\\").replace("\"", "\\\"");
        String safeWhere = where == null ? "" : where.replace("\\", "\\\\").replace("\"", "\\\"");
        String safeLevel = level == null ? "INFO" : level;
        String safeTs = timestampIso == null ? DateTimeFormatter.ISO_INSTANT.format(Instant.now()) : timestampIso;
        return String.format(
                "{\"action\":\"call\",\"function\":\"logstate\",\"args\":[\"%s\",\"%s\",\"%s\",\"%s\"]}",
                safeLevel, safeWhere, safeMsg, safeTs
        );
    }

    // instance method to send a single log event to all clients
    public void sendLog(String level, String where, String message, String timestampIso) {
        String json = buildLogJson(level, where, message, timestampIso);
        broadcast(json);
    }

    // static helper for callers
    public static synchronized void notifyLog(String level, String where, String message) {
        String json = buildLogJson(level, where, message, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        if (instance != null) {
            instance.broadcast(json);
        } else {
            pending.add(json);
        }
    }

    public static LogstateServer getInstance() {
        return instance;
    }
}
