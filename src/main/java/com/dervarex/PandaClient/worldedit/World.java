package com.dervarex.PandaClient.worldedit;

import com.dervarex.PandaClient.Auth.AuthManager;
import com.dervarex.PandaClient.worldedit.GUI.SelectWorldDashboard;
import com.formdev.flatlaf.FlatDarkLaf;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class World {
    public String name;
    public File location;
    public Difficulty difficulty;
    public Boolean commands;
    public long DayTime;
    public String version;
    public long seed;
    public Gamemode gamemode;

    public World(File location) {
        try {
        File levelDat = new File(location, "level.dat");
        if (!levelDat.exists()) {
            throw new IllegalArgumentException("The provided location does not contain a valid Minecraft world.");
        }

            NamedTag namedTag = NBTUtil.read(levelDat);
            CompoundTag root = (CompoundTag) namedTag.getTag();
            if (!root.containsKey("Data")) {
                throw new IllegalArgumentException("Invalid level.dat: missing Data tag.");
            }
            CompoundTag data = root.getCompoundTag("Data");
            this.name = data.getString("LevelName");
            this.difficulty = Difficulty.fromInt(data.getByte("Difficulty"));
            this.location = location;
            this.commands = data.getBoolean("allowCommands");
            this.DayTime = data.getLong("DayTime");
            this.version = data.getCompoundTag("Version").getString("Name");
            this.seed = data.getCompoundTag("WorldGenSettings").getLong("seed");
            this.gamemode = Gamemode.fromInt(data.getInt("GameType"));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void changeName(String newName) {
        this.name = newName;
        try {
            File levelDat = new File(location, "level.dat");
            NamedTag namedTag = NBTUtil.read(levelDat);
            CompoundTag root = (CompoundTag) namedTag.getTag();
            CompoundTag data = root.getCompoundTag("Data");
            data.putString("LevelName", newName);
            NBTUtil.write(namedTag, levelDat);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void changeDifficulty(Difficulty newDifficulty) {
        this.difficulty = newDifficulty;
        try {
            File levelDat = new File(location, "level.dat");
            NamedTag namedTag = NBTUtil.read(levelDat);
            CompoundTag root = (CompoundTag) namedTag.getTag();
            CompoundTag data = root.getCompoundTag("Data");
            data.putByte("Difficulty", (byte) newDifficulty.ordinal());
            NBTUtil.write(namedTag, levelDat);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void changeCommands(Boolean allowCommands) {
        this.commands = allowCommands;
        try {
            File levelDat = new File(location, "level.dat");
            NamedTag namedTag = NBTUtil.read(levelDat);
            CompoundTag root = (CompoundTag) namedTag.getTag();
            CompoundTag data = root.getCompoundTag("Data");
            data.putBoolean("allowCommands", allowCommands);
            NBTUtil.write(namedTag, levelDat);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void changeGamemode(Gamemode gameMode) {
        try {
            this.gamemode = gamemode;
            File levelDat = new File(location, "level.dat");
            NamedTag namedTag = NBTUtil.read(levelDat);
            CompoundTag root = (CompoundTag) namedTag.getTag();
            CompoundTag data = root.getCompoundTag("Data");
            data.putInt("GameType", Gamemode.toInt(gameMode));
            NBTUtil.write(namedTag, levelDat);
            File playerDat = new File(location, "playerdata" + File.separator + AuthManager.getUser().getUuid() + ".dat");
            if (playerDat.exists()) {
                NamedTag playerTag = NBTUtil.read(playerDat);
                CompoundTag playerRoot = (CompoundTag) playerTag.getTag();
                playerRoot.putInt("playerGameType", Gamemode.toInt(gameMode));
                NBTUtil.write(playerTag, playerDat);
            } // if the player data doesn't exist yet, we can skip it because it will be created on first join and use the Gamemode set before
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void changeDayTime(long dayTime) {
        this.DayTime = dayTime;
        try {
            File levelDat = new File(location, "level.dat");
            NamedTag namedTag = NBTUtil.read(levelDat);
            CompoundTag root = (CompoundTag) namedTag.getTag();
            CompoundTag data = root.getCompoundTag("Data");
            data.putLong("DayTime", dayTime);
            NBTUtil.write(namedTag, levelDat);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getName() {
        return this.name;
    }

    public File getInstance() {
        return this.location.getParentFile().getParentFile(); // myworld -> saves -> instance
    }

    public static void main(String[] args) {
//        AuthManager.User user = AuthManager.loginWithSavedSession();
//        World world = new World(new File("/home/dervarex/.local/share/PrismLauncher/instances/1.21.8(1)/minecraft/saves/One Chunk/"));
//        System.out.println("World Name: " + world.name);
//        System.out.println("Difficulty: " + world.difficulty);
//        System.out.println("Commands Allowed: " + world.commands);
//        System.out.println("Day Time: " + world.DayTime);
//        System.out.println("Version: " + world.version);
//        System.out.println("Seed: " + world.seed);
//
//        world.changeGamemode(Gamemode.SPECTATOR);
        FlatDarkLaf.setup();

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Launcher");
            f.setSize(500, 400);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setLocationRelativeTo(null);

            f.setContentPane(new SelectWorldDashboard(
                    Arrays.asList(
                            new World(new File("/home/dervarex/.local/share/PrismLauncher/instances/1.21.8(1)/minecraft/saves/One Chunk/")),
                            new World(new File("/home/dervarex/.local/share/PrismLauncher/instances/1.21.8(1)/minecraft/saves/Skyblockkk"))
                    )
            ));

            f.setVisible(true);
        });
    }
}
