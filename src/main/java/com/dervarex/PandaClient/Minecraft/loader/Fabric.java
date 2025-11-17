package com.dervarex.PandaClient.Minecraft.loader;

import com.dervarex.PandaClient.Auth.AuthManager;
import com.dervarex.PandaClient.Minecraft.MinecraftLauncher;
import com.dervarex.PandaClient.Minecraft.Profile.ProfileManagement;
import com.dervarex.PandaClient.Minecraft.loader.fabric.FabricInstallerFetcher;

import java.io.*;
import java.util.Objects;
import java.util.Optional;

public class Fabric {

    private static final String BASE_URL = "https://meta.fabricmc.net/v2/";

    protected String getBaseUrl() {
        return BASE_URL;
    }

    protected String getInstallerUrl() {
        return getBaseUrl() + "versions/installer";
    }

    /**
     * Returns the latest Fabric installer, downloading it if necessary.
     */
    protected String getInstaller() throws Exception {
        File installerFolder = new File(System.getProperty("java.io.tmpdir"), "PandaClient/installer");
        if (!installerFolder.exists()) installerFolder.mkdirs();

        // Check for cached installer
        File[] cached = installerFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (cached != null && cached.length > 0) {
            return cached[0].getAbsolutePath();
        }

        // No installer cached, download the newest one
        return FabricInstallerFetcher.downloadLatestInstaller(installerFolder);
    }

    /**
     * Installs Fabric for a given Minecraft version and directory.
     */
    public void install(String version, File instanceDir, String name) {
        File verDir = new File(instanceDir + File.separator + "versions");
        Boolean alreadyInstalled = true;

        if (!verDir.exists() || !verDir.isDirectory()) {
            System.err.println("⚠️ 'versions' directory not found at: " + verDir.getAbsolutePath());
            // Fabric not installed
            alreadyInstalled = false; // this means it is not installed yet
        }

        if (alreadyInstalled) { //an check if it's a working installation hahaha
            for (File file : verDir.listFiles()) {
                if (file.isDirectory() && file.getName().contains("fabric")) {
                    return; // Fabric already installed
                }
            }
        }

        try {
            // 1. Create a basic vanilla profile first
            ProfileManagement pm = new ProfileManagement();
            pm.createProfile(name, version, LoaderType.FABRIC);
            MinecraftLauncher.LaunchMinecraft(
                    version,
                    LoaderType.VANILLA,
                    AuthManager.getUser().getUsername(),
                    AuthManager.getUser().getUuid(),
                    AuthManager.getUser().getAccessToken(),
                    instanceDir,
                    false,
                    Optional.empty()
            ); // Install vanilla to set up files

            // 2. Download or use cached Fabric installer
            String installerPath = getInstaller();

            // 3. Run Fabric installer
            ProcessBuilder pb = new ProcessBuilder(
                    "java", "-jar", installerPath,
                    "client",
                    "-dir", instanceDir.getAbsolutePath(),
                    "-mcversion", version,
                    "-noprofile",
                    "-loader", "latest",
                    "-installDir", instanceDir.getAbsolutePath()
            );

            pb.redirectErrorStream(true); // Merge stdout + stderr
            Process process = pb.start();

            // Print Fabric output to console
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(System.out::println);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("✅ Fabric installed successfully in " + instanceDir.getAbsolutePath());
            } else {
                System.out.println("❌ Fabric installation failed with exit code " + exitCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
