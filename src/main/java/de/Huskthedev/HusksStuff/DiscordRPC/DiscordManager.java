package de.Huskthedev.HusksStuff.DiscordRPC;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public final class DiscordManager {
    public static String APPLICATION_ID = "1397949230563463188"; // TODO: set your Discord App ID or pass to start(String)

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    // Provider support: either Arikia (net.arikia) or Minnced (club.minnced)
    private enum Provider { ARIKIA, MINNCED }
    private static Provider provider = null;

    // Arikia reflection
    private static Class<?> clsDiscordRPC;             // net.arikia.dev.drpc.DiscordRPC
    private static Class<?> clsDiscordRichPresence;    // net.arikia.dev.drpc.DiscordRichPresence
    private static Class<?> clsDRPBuilder;             // net.arikia.dev.drpc.DiscordRichPresence$Builder

    private static Method mDiscordInitialize;          // discordInitialize(String, DiscordEventHandlers, boolean)
    private static Method mDiscordUpdatePresence;      // discordUpdatePresence(DiscordRichPresence)
    private static Method mDiscordRunCallbacks;        // discordRunCallbacks()
    private static Method mDiscordShutdown;            // discordShutdown()

    private static Constructor<?> cPresenceBuilder;    // new DiscordRichPresence.Builder(String details)
    private static Method mSetState;                   // builder.setState(String)
    private static Method mSetStartTimestamps;         // builder.setStartTimestamps(long)
    private static Method mSetLargeImage;              // builder.setLargeImage(String key, String text)
    private static Method mBuild;                      // builder.build()

    // Minnced reflection
    private static Class<?> mzdDiscordRPC;             // club.minnced.discord.rpc.DiscordRPC
    private static Class<?> mzdRichPresence;           // club.minnced.discord.rpc.DiscordRichPresence
    private static Object mzdInstance;                 // DiscordRPC.INSTANCE
    private static Method mzInit;                      // Discord_Initialize(String, DiscordEventHandlers, boolean, String)
    private static Method mzUpdate;                    // Discord_UpdatePresence(DiscordRichPresence)
    private static Method mzRun;                       // Discord_RunCallbacks()
    private static Method mzShutdown;                  // Discord_Shutdown()

    private static Thread callbackThread;

    private static String lastDetails = "Launching PandaClient";
    private static String lastState = "";
    private static String lastLargeImageKey = null;
    private static String lastLargeImageText = null;

    private DiscordManager() {}

    public static void start() {
        start(APPLICATION_ID);
    }

    public static synchronized void start(String appId) {
        if (RUNNING.get()) {
            log("Discord RPC is already running.");
            return;
        }
        if (appId == null || appId.isBlank()) {
            logWarn("Discord RPC not started: Application ID is empty. Set DiscordManager.APPLICATION_ID or call start(appId).");
            return;
        }
        if (!prepareAnyReflection()) {
            logWarn("Discord RPC library not found on classpath (supported: net.arikia.dev:discord-rpc or club.minnced:java-discord-rpc). Place the jar into ./libraries and restart to enable RPC.");
            return;
        }
        try {
            // Initialize
            if (provider == Provider.ARIKIA) {
                mDiscordInitialize.invoke(null, appId, null, Boolean.TRUE);
            } else {
                // Minnced signature: (String appId, DiscordEventHandlers handlers, boolean autoRegister, String steamId)
                mzInit.invoke(mzdInstance, appId, null, Boolean.TRUE, null);
            }

            applyPresenceInternal(
                Objects.requireNonNullElse(lastDetails, "Using PandaClient"),
                lastState,
                lastLargeImageKey,
                lastLargeImageText,
                true
            );

            RUNNING.set(true);

            callbackThread = new Thread(() -> {
                try {
                    while (RUNNING.get()) {
                        try {
                            if (provider == Provider.ARIKIA) {
                                mDiscordRunCallbacks.invoke(null);
                            } else if (provider == Provider.MINNCED) {
                                mzRun.invoke(mzdInstance);
                            }
                        } catch (Throwable t) {
                        }
                        try { Thread.sleep(2000); } catch (InterruptedException ie) { /* ignore */ }
                    }
                } catch (Throwable t) {
                    logErr("Discord RPC callback thread crashed: " + t);
                }
            }, "Discord-RPC-Callback");
            callbackThread.setDaemon(true);
            callbackThread.start();

            Runtime.getRuntime().addShutdownHook(new Thread(DiscordManager::stop, "Discord-RPC-Shutdown"));

            log("Discord RPC initialized.");
        } catch (Throwable t) {
            logErr("Failed to initialize Discord RPC: " + t);
        }
    }

    public static synchronized void updatePresence(String details, String state) {
        updatePresence(details, state, null, null);
    }

    public static synchronized void updatePresence(String details, String state, String largeImageKey, String largeImageText) {
        lastDetails = details;
        lastState = state;
        lastLargeImageKey = largeImageKey;
        lastLargeImageText = largeImageText;
        if (!RUNNING.get()) return;
        applyPresenceInternal(details, state, largeImageKey, largeImageText, false);
    }

    public static synchronized void stop() {
        if (!RUNNING.get()) return;
        RUNNING.set(false);
        try {
            if (callbackThread != null) {
                callbackThread.interrupt();
                callbackThread = null;
            }
            if (provider == Provider.ARIKIA && mDiscordShutdown != null) {
                mDiscordShutdown.invoke(null);
            } else if (provider == Provider.MINNCED && mzShutdown != null) {
                mzShutdown.invoke(mzdInstance);
            }
            log("Discord RPC shutdown.");
        } catch (Throwable t) {
            logErr("Error during Discord RPC shutdown: " + t);
        }
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    private static boolean prepareAnyReflection() {
        if (prepareArikiaReflection()) {
            provider = Provider.ARIKIA;
            return true;
        }
        if (prepareMinncedReflection()) {
            provider = Provider.MINNCED;
            return true;
        }
        // Try to dynamically load from ./libraries directory
        if (tryLoadFromLibraries()) {
            return true;
        }
        return false;
    }

    private static boolean prepareArikiaReflection() {
        try {
            clsDiscordRPC = Class.forName("net.arikia.dev.drpc.DiscordRPC");
            clsDiscordRichPresence = Class.forName("net.arikia.dev.drpc.DiscordRichPresence");
            for (Class<?> c : clsDiscordRichPresence.getDeclaredClasses()) {
                if (c.getSimpleName().equals("Builder")) {
                    clsDRPBuilder = c;
                    break;
                }
            }
            if (clsDRPBuilder == null) return false;

            mDiscordInitialize = clsDiscordRPC.getMethod("discordInitialize", String.class, Class.forName("net.arikia.dev.drpc.DiscordEventHandlers"), boolean.class);
            mDiscordUpdatePresence = clsDiscordRPC.getMethod("discordUpdatePresence", clsDiscordRichPresence);
            mDiscordRunCallbacks = clsDiscordRPC.getMethod("discordRunCallbacks");
            mDiscordShutdown = clsDiscordRPC.getMethod("discordShutdown");

            cPresenceBuilder = clsDRPBuilder.getConstructor(String.class);
            mSetState = clsDRPBuilder.getMethod("setState", String.class);
            mSetStartTimestamps = clsDRPBuilder.getMethod("setStartTimestamps", long.class);
            try {
                mSetLargeImage = clsDRPBuilder.getMethod("setLargeImage", String.class, String.class);
            } catch (NoSuchMethodException e) {
                mSetLargeImage = null;
            }
            mBuild = clsDRPBuilder.getMethod("build");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean prepareMinncedReflection() {
        try {
            mzdDiscordRPC = Class.forName("club.minnced.discord.rpc.DiscordRPC");
            mzdRichPresence = Class.forName("club.minnced.discord.rpc.DiscordRichPresence");
            mzdInstance = mzdDiscordRPC.getField("INSTANCE").get(null);
            // Methods
            Class<?> handlers = Class.forName("club.minnced.discord.rpc.DiscordEventHandlers");
            mzInit = mzdDiscordRPC.getMethod("Discord_Initialize", String.class, handlers, boolean.class, String.class);
            mzUpdate = mzdDiscordRPC.getMethod("Discord_UpdatePresence", mzdRichPresence);
            mzRun = mzdDiscordRPC.getMethod("Discord_RunCallbacks");
            mzShutdown = mzdDiscordRPC.getMethod("Discord_Shutdown");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    // Attempt to load supported RPC libraries from ./libraries directory
    private static boolean tryLoadFromLibraries() {
        try {
            File libDir = new File("libraries");
            if (!libDir.exists() || !libDir.isDirectory()) return false;
            File[] jars = libDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (jars == null || jars.length == 0) return false;
            for (File jar : jars) {
                try {
                    URLClassLoader cl = new URLClassLoader(new URL[]{jar.toURI().toURL()}, DiscordManager.class.getClassLoader());
                    if (prepareArikiaReflection(cl)) {
                        provider = Provider.ARIKIA;
                        log("Loaded Discord RPC (Arikia) from: " + jar.getName());
                        return true;
                    }
                    if (prepareMinncedReflection(cl)) {
                        provider = Provider.MINNCED;
                        log("Loaded Discord RPC (Minnced) from: " + jar.getName());
                        return true;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean prepareArikiaReflection(ClassLoader cl) {
        try {
            clsDiscordRPC = Class.forName("net.arikia.dev.drpc.DiscordRPC", true, cl);
            clsDiscordRichPresence = Class.forName("net.arikia.dev.drpc.DiscordRichPresence", true, cl);
            Class<?>[] inner = clsDiscordRichPresence.getDeclaredClasses();
            clsDRPBuilder = null;
            for (Class<?> c : inner) {
                if (c.getSimpleName().equals("Builder")) {
                    clsDRPBuilder = c;
                    break;
                }
            }
            if (clsDRPBuilder == null) return false;

            mDiscordInitialize = clsDiscordRPC.getMethod("discordInitialize", String.class, Class.forName("net.arikia.dev.drpc.DiscordEventHandlers", true, cl), boolean.class);
            mDiscordUpdatePresence = clsDiscordRPC.getMethod("discordUpdatePresence", clsDiscordRichPresence);
            mDiscordRunCallbacks = clsDiscordRPC.getMethod("discordRunCallbacks");
            mDiscordShutdown = clsDiscordRPC.getMethod("discordShutdown");

            cPresenceBuilder = clsDRPBuilder.getConstructor(String.class);
            mSetState = clsDRPBuilder.getMethod("setState", String.class);
            mSetStartTimestamps = clsDRPBuilder.getMethod("setStartTimestamps", long.class);
            try {
                mSetLargeImage = clsDRPBuilder.getMethod("setLargeImage", String.class, String.class);
            } catch (NoSuchMethodException e) {
                mSetLargeImage = null;
            }
            mBuild = clsDRPBuilder.getMethod("build");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean prepareMinncedReflection(ClassLoader cl) {
        try {
            mzdDiscordRPC = Class.forName("club.minnced.discord.rpc.DiscordRPC", true, cl);
            mzdRichPresence = Class.forName("club.minnced.discord.rpc.DiscordRichPresence", true, cl);
            mzdInstance = mzdDiscordRPC.getField("INSTANCE").get(null);
            Class<?> handlers = Class.forName("club.minnced.discord.rpc.DiscordEventHandlers", true, cl);
            mzInit = mzdDiscordRPC.getMethod("Discord_Initialize", String.class, handlers, boolean.class, String.class);
            mzUpdate = mzdDiscordRPC.getMethod("Discord_UpdatePresence", mzdRichPresence);
            mzRun = mzdDiscordRPC.getMethod("Discord_RunCallbacks");
            mzShutdown = mzdDiscordRPC.getMethod("Discord_Shutdown");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void applyPresenceInternal(String details, String state, String largeImageKey, String largeImageText, boolean setStartTs) {
        try {
            if (provider == Provider.ARIKIA) {
                if (clsDRPBuilder == null) return;
                Object builder = cPresenceBuilder.newInstance(details == null ? "" : details);
                if (state != null) mSetState.invoke(builder, state);
                if (setStartTs) mSetStartTimestamps.invoke(builder, Instant.now().getEpochSecond());
                if (mSetLargeImage != null && largeImageKey != null) {
                    if (largeImageText == null) largeImageText = "";
                    mSetLargeImage.invoke(builder, largeImageKey, largeImageText);
                }
                Object presence = mBuild.invoke(builder);
                mDiscordUpdatePresence.invoke(null, presence);
            } else if (provider == Provider.MINNCED) {
                Object presence = mzdRichPresence.getDeclaredConstructor().newInstance();
                // Set fields directly
                try { mzdRichPresence.getField("details").set(presence, details == null ? "" : details); } catch (Throwable ignored) {}
                try { if (state != null) mzdRichPresence.getField("state").set(presence, state); } catch (Throwable ignored) {}
                try { if (setStartTs) mzdRichPresence.getField("startTimestamp").setLong(presence, Instant.now().getEpochSecond()); } catch (Throwable ignored) {}
                if (largeImageKey != null) {
                    try { mzdRichPresence.getField("largeImageKey").set(presence, largeImageKey); } catch (Throwable ignored) {}
                    try { mzdRichPresence.getField("largeImageText").set(presence, largeImageText == null ? "" : largeImageText); } catch (Throwable ignored) {}
                }
                mzUpdate.invoke(mzdInstance, presence);
            }
        } catch (Throwable t) {
            logErr("Failed to update Discord presence: " + t);
        }
    }

    private static void log(String msg) {
        System.out.println("[DiscordRPC] " + msg);
    }

    private static void logWarn(String msg) {
        System.out.println("[DiscordRPC][WARN] " + msg);
    }

    private static void logErr(String msg) {
        System.err.println("[DiscordRPC][ERROR] " + msg);
    }
}
