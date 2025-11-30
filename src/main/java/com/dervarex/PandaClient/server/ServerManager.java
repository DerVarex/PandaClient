package com.dervarex.PandaClient.server;

import com.dervarex.PandaClient.server.ui.MainDashboard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dervarex.PandaClient.utils.file.getAppdata.getAppDataFolder;
import static com.dervarex.PandaClient.utils.OS.OSUtil.getOS;

/**
 * Central registry for all managed servers and their dashboards.
 */
public class ServerManager {

    public static File getServerDir(String name) {
        File base = getBaseDir();
        File serverDir = new File(base, name);
        serverDir.mkdirs();
        return serverDir;
    }

    public static final Map<String, Server> SERVERS = Collections.synchronizedMap(new HashMap<>());

    /** Base folder where all server instances live. */
    public static File getBaseDir() {
        return new File(getAppDataFolder(getOS()), "Development/PandaClientServers");
    }

    /** Config file for a given server name. */
    public static File getConfigFile(String name) {
        return new File(getBaseDir(), name + ".json");
    }

    /** Load all known servers from disk into memory. Call once during startup. */
    public static synchronized void loadServersFromDisk() {
        File base = getBaseDir();
        if (!base.exists() || !base.isDirectory()) return;
        File[] files = base.listFiles((dir, n) -> n.toLowerCase().endsWith(".json"));
        if (files == null) return;
        for (File cfg : files) {
            try {
                Server loaded = new ServerConfig().loadConfig(cfg);
                if (loaded != null) {
                    SERVERS.put(loaded.getName(), loaded);
                }
            } catch (Exception ignored) {
            }
        }
    }

    /** Return a snapshot list of all known servers. */
    public static synchronized List<Server> listServers() {
        return new ArrayList<>(SERVERS.values());
    }

    // createAndStart is no longer used by the flow; keeping for backward compat but delegating
    @Deprecated
    public static synchronized Server createAndStart(String name, String version, ServerType type) throws IOException {
        // For backward compatibility, route through create + startExisting if ever called.
        File base = getBaseDir();
        File serverDir = new File(base, name);
        serverDir.mkdirs();
        File jarFile = new File(serverDir, name + ".jar");
        String url = type.getDownloadUrl(version);
        com.dervarex.PandaClient.utils.NetUtils.NetUtils.downloadFromUrl(url, jarFile.getAbsolutePath());
        Server server = new Server(name, version, serverDir, jarFile,
                "nogui", "-Dcom.mojang.eula.agree=true");
        new ServerConfig().saveConfig(server, getConfigFile(name));
        SERVERS.put(name, server);
        return startExisting(name);
    }

    /** Start an already-known server by name, opening a dashboard window if needed. */
    public static synchronized Server startExisting(String name) throws IOException {
        Server existing = SERVERS.get(name);
        if (existing == null) {
            // try to load from disk config
            File cfg = getConfigFile(name);
            if (cfg.exists()) {
                existing = new ServerConfig().loadConfig(cfg);
                if (existing != null) {
                    SERVERS.put(name, existing);
                }
            }
        }
        if (existing == null) return null;

        MainDashboard ui = new MainDashboard(existing.getName(), existing.getVersion());
        ui.attachServer(existing);
        existing.addConsoleListener(ui::addConsoleLog);
        ui.setVisible(true);

        existing.start();
        return existing;
    }

    public static synchronized Server get(String name) {
        return SERVERS.get(name);
    }

    public static synchronized void delete(String name) {
        Server s = SERVERS.remove(name);
        if (s != null) {
            s.shutdown();
        }
        // delete persisted config as well
        File cfg = getConfigFile(name);
        if (cfg.exists()) {
            // best-effort delete, ignore result
            // no recursive delete here to avoid wiping world data accidentally
            cfg.delete();
        }
    }
}
