package com.dervarex.PandaClient;

import de.Huskthedev.HusksStuff.DiscordRPC.DiscordManager;

public class Main {
    public static void main(String[] args) {
        // Start Discord Rich Presence as early as possible
        DiscordManager.start();
        // The GUI
        new MainGUI();
    }
}