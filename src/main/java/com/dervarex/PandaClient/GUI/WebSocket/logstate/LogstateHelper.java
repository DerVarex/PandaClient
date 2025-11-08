package com.dervarex.PandaClient.GUI.WebSocket.logstate;

public final class LogstateHelper {
    private LogstateHelper() {}

    /**
     * Send a log state message to the frontend (e.g. progress indication).
     */
    public static void logstate(String message) {
        Logstate.send("INFO", "Backend", message);
    }

    /**
     * Clear the log state in the frontend.
     */
    public static void clearlogstate() {
        Logstate.send("INFO", "Backend", "__CLEAR__");
    }
}
