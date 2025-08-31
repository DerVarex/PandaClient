package com.dervarex.PandaClient.utils.Minecraft.forge;

import com.dervarex.PandaClient.Minecraft.MinecraftLauncher;
import com.dervarex.PandaClient.utils.WebUtils.downloadFromURL;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ForgeHelper {
    private String version;
    public ForgeHelper(String version) {
        this.version = version;
    }
    /*public static void main(String[] args) {
        ForgeHelper.installForge("1.20.1", new File("C:\\Users\\flori\\Desktop\\forge"), new File("C:\\Users\\flori\\Desktop\\Neuer Ordner"));
    } */
    public String getVersionID() {
        return version;
    }
    public static void installForge(String version, File target, File installerFolder, String username, String uuid, String accessToken) {
        String instanceName = target.getName();
        MinecraftLauncher.LaunchMinecraft(version, username, uuid, accessToken, new File(instanceName), false, true);
        ForgeHelper helper = new ForgeHelper(version);
        // Processing Version
        if (isBelow(version, "1.16")) {
            System.out.println("Version below 1.16!");
        } else {
            System.out.println("Version ok.");
            System.out.println("Downloading Forge InstallerJar...");

            // Downloading the Forge InstallerJar
            try {
                String forgeJarLink = GetLatestForgeVersionUrl.getLatestForgeInstallerUrl(version);
                if (forgeJarLink == null) {
                    System.out.println("No Forge InstallerJar found for " + version);
                    System.out.println("Please check your Minecraft Version.");
                }
                String targetJarFilePath = installerFolder.getAbsolutePath() + File.separator + "forge-" + version + "-installer.jar";
                downloadFromURL.download(forgeJarLink, targetJarFilePath);
                System.out.println("Done.");
                // Creating launcher_profiles.json

                File launcher_profiles = new File(target + File.separator + "launcher_profiles.json");
                if(!launcher_profiles.exists()) {

                    try (FileWriter writer = new FileWriter(launcher_profiles)) {
                        writer.write("{}"); // Leeres JSON-Objekt
                        System.out.println("Datei erstellt: " + launcher_profiles.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Launcher_profiles exists.");
                }
                // Installing forge
                System.out.println("Installing Forge...");
                ForgeInstall forgeInstall = new ForgeInstall();
                forgeInstall.install(version, target, installerFolder, new File(targetJarFilePath));
                System.out.println("Done.");
                
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error downloading Forge InstallerJar: " + e.getMessage());
                return;
            }
        }
    }
    //For Processing version
    public static boolean isBelow(String version, String minimum) {
        String[] v1 = version.split("\\.");
        String[] v2 = minimum.split("\\.");

        int length = Math.max(v1.length, v2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < v1.length ? Integer.parseInt(v1[i]) : 0;
            int num2 = i < v2.length ? Integer.parseInt(v2[i]) : 0;

            if (num1 < num2) return true;
            if (num1 > num2) return false;
            // wenn gleich → weitermachen
        }
        return false; // exakt gleich
    }

}
