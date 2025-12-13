package com.dervarex.PandaClient.worldedit;

import java.io.File;
import java.util.List;

public class WorldUtils {
    public static List<World> getAvailableWorlds(File instanceDir) {
        List<World> worlds = new java.util.ArrayList<>();
        File worldsDir = new File(instanceDir, "saves");
        if (worldsDir.exists() && worldsDir.isDirectory()) {
            File[] worldDirs = worldsDir.listFiles(File::isDirectory);
            if (worldDirs != null) {
                for (File worldDir : worldDirs) {
                    try {
                        World world = new World(worldDir);
                        System.out.println("Found world: " + world.name + " at " + world.location.getAbsolutePath());
                        worlds.add(world);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Skipping invalid world directory: " + worldDir.getAbsolutePath());
                    }
                }
            }
        }
        return worlds;
    }
}
