package com.dervarex.PandaClient.Minecraft.logger;

import com.dervarex.PandaClient.GUI.WebSocket.NotificationServer.NotificationServer;
import com.dervarex.PandaClient.GUI.WebSocket.logstate.LogstateServer;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ClientLogger {
    // make everything static so callers don't need an instance
    private static boolean started = false;
    private static JSONObject log = null;
    private static final List<String> inMemory = new ArrayList<>();

    // default file where the logger will save automatically (set by Main)
    private static volatile File defaultLogFile = null;
    private static final long SAVE_INTERVAL_MS = 2000; // debounce interval
    private static volatile long lastSavedAt = 0L;

    public static synchronized boolean start() {
        try {
            if (log == null) log = new JSONObject();
            started = true;
            log.put("started", true);
            log.put("createdAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            // move any in-memory messages into the log
            if (!inMemory.isEmpty()) {
                org.json.JSONArray arr = new org.json.JSONArray();
                for (String s : inMemory) arr.put(s);
                log.put("logs", arr);
                inMemory.clear();
            }
            // try to save immediately on start if a default file is configured
            if (defaultLogFile != null) {
                save(defaultLogFile);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // allow the application to set a default file where logs are saved periodically
    public static synchronized void setDefaultLogFile(File file) {
        defaultLogFile = file;
        if (log == null) log = new JSONObject();
        // ensure the file's parent exists
        try {
            if (defaultLogFile != null) {
                File parent = defaultLogFile.getAbsoluteFile().getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                save(defaultLogFile);
            }
        } catch (Exception e) {
            System.err.println("ClientLogger: failed to set default log file: " + e.getMessage());
        }
    }

    // level could be INFO, WARNING, ERROR
    public static synchronized void log(String message, String level, String where) {
        try {
            if (log == null) {
                // lazy init minimal structure and store messages until start() is called
                inMemory.add(String.format("[%s] %s: %s", level, where, message));
            } else {
                String formatted = String.format("[%s] %s: %s", level, where, message);
                // append to logs array
                if (!log.has("logs")) {
                    org.json.JSONArray arr = new org.json.JSONArray();
                    arr.put(formatted);
                    log.put("logs", arr);
                } else {
                    // append
                    org.json.JSONArray arr = log.getJSONArray("logs");
                    arr.put(formatted);
                    log.put("logs", arr);
                }
                // also store last message/time
                log.put("last", formatted);
                log.put("lastTime", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

                // write to stdout/stderr based on level
                if ("ERROR".equalsIgnoreCase(level)) {
                    System.err.println(formatted);
                    // notify via NotificationServer (static helper)
                    NotificationServer.notify(NotificationServer.NotificationType.ERROR, formatted);
                } else if ("WARN".equalsIgnoreCase(level) || "WARNING".equalsIgnoreCase(level)) {
                    System.out.println(formatted);
                    NotificationServer.notify(NotificationServer.NotificationType.WARNING, formatted);
                } else {
                    System.out.println(formatted);
                }

//                // broadcast to Logstate WebSocket so UI can render live
//                try {
//                    LogstateServer.notifyLog(level, where, message);
//                } catch (Throwable t) {
//                    // never break logging if ws isn't ready
//                }

                // persist to disk: immediately for ERROR, debounced for others
                if (defaultLogFile != null) {
                    long now = System.currentTimeMillis();
                    if ("ERROR".equalsIgnoreCase(level)) {
                        // immediate save on error
                        save(defaultLogFile);
                        lastSavedAt = now;
                    } else if (now - lastSavedAt > SAVE_INTERVAL_MS) {
                        save(defaultLogFile);
                        lastSavedAt = now;
                    }
                }
            }
        } catch (Exception e) {
            // avoid throwing from logger; fallback to stderr
            System.err.println("ClientLogger internal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Save a JSONObject to disk with pretty printing
    public static synchronized void save(JSONObject json, File file) {
        if (file == null) {
            NotificationServer.notify(NotificationServer.NotificationType.WARNING, "ClientLogger.save called with null file");
            return;
        }
        try {
            File parent = file.getAbsoluteFile().getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json == null ? getDefaultLog().toString(2) : json.toString(2));
            }
        } catch (IOException e) {
            e.printStackTrace();
            NotificationServer.notify(NotificationServer.NotificationType.ERROR, "Couldn't save the Log file: " + e.getMessage());
        }
    }

    // Convenience overload: save the internally tracked log to the given file
    public static synchronized void save(File file) {
        save(log, file);
    }

    private static JSONObject getDefaultLog() {
        JSONObject d = new JSONObject();
        d.put("createdAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        d.put("logs", new org.json.JSONArray());
        return d;
    }

    // helper to get the current log object (may be null)
    public static synchronized JSONObject getLog() {
        return log;
    }
}
