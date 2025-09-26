package de.Huskthedev.HusksStuff.DiscordRPC;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DiscordManager {
    public static String APPLICATION_ID = "1397949230563463188"; // TODO: set your Discord App ID or pass to start(String)

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

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
        if (!prepareArikiaReflection()) {
            logWarn("Discord RPC library (net.arikia.dev:discord-rpc) not found on classpath. RPC will be disabled.");
            return;
        }
        try {
            // Initialize
            mDiscordInitialize.invoke(null, appId, null, Boolean.TRUE);

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
                            mDiscordRunCallbacks.invoke(null);
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
            if (mDiscordShutdown != null) {
                mDiscordShutdown.invoke(null);
            }
            log("Discord RPC shutdown.");
        } catch (Throwable t) {
            logErr("Error during Discord RPC shutdown: " + t);
        }
    }

    public static boolean isRunning() {
        return RUNNING.get();
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

    private static void applyPresenceInternal(String details, String state, String largeImageKey, String largeImageText, boolean setStartTs) {
        if (clsDRPBuilder == null) return;
        try {
            Object builder = cPresenceBuilder.newInstance(details == null ? "" : details);
            if (state != null) mSetState.invoke(builder, state);
            if (setStartTs) mSetStartTimestamps.invoke(builder, Instant.now().getEpochSecond());
            if (mSetLargeImage != null && largeImageKey != null) {
                if (largeImageText == null) largeImageText = "";
                mSetLargeImage.invoke(builder, largeImageKey, largeImageText);
            }
            Object presence = mBuild.invoke(builder);
            mDiscordUpdatePresence.invoke(null, presence);
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
