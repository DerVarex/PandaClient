package com.dervarex.PandaClient.Minecraft.loader.fabric;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FabricInstallerFetcher {

    private static final String FABRIC_INSTALLER_META_URL =
            "https://meta.fabricmc.net/v2/versions/installer";

    /**
     * Downloads the latest Fabric Installer from the official Fabric Meta API.
     * Returns the local file path to the downloaded jar.
     *
     * @param targetFolder The folder where the installer should be saved.
     * @return Absolute path to the downloaded installer file.
     */
    public static String downloadLatestInstaller(File targetFolder) throws Exception {
        // Ensure the target folder exists
        if (!targetFolder.exists() && !targetFolder.mkdirs()) {
            throw new IOException("Failed to create target folder: " + targetFolder.getAbsolutePath());
        }

        // 1. Fetch latest version info from Fabric Meta API
        JsonObject latest = getLatestInstaller();

        String version = latest.get("version").getAsString();
        String downloadUrl = latest.get("url").getAsString();

        // 2. Target file path
        File targetFile = new File(targetFolder, "fabric-installer.jar");

        // 3. Download the jar file
        downloadFile(downloadUrl, targetFile);

        System.out.println("✅ Fabric Installer v" + version + " downloaded successfully!");
        return targetFile.getAbsolutePath();
    }

    /**
     * Fetches the latest Fabric installer info from the Fabric Meta API.
     */
    private static JsonObject getLatestInstaller() throws Exception {
        String json = getJsonFromUrl(FABRIC_INSTALLER_META_URL);

        JsonArray array = JsonParser.parseString(json).getAsJsonArray();

        if (array.size() == 0)
            throw new RuntimeException("No Fabric installers found!");

        // Fabric always lists the newest installer first
        return array.get(0).getAsJsonObject();
    }

    /**
     * Downloads any file from the given URL and saves it to the target file.
     */
    private static void downloadFile(String urlString, File targetFile) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestProperty("User-Agent", "PandaClient-Updater");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        if (conn.getResponseCode() != 200)
            throw new IOException("Failed to download: HTTP " + conn.getResponseCode());

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(targetFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        conn.disconnect();
    }

    /**
     * Retrieves the raw JSON string from a URL.
     */
    private static String getJsonFromUrl(String urlString) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestProperty("User-Agent", "PandaClient-Updater");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        if (conn.getResponseCode() != 200)
            throw new IOException("Failed to fetch JSON: HTTP " + conn.getResponseCode());

        StringBuilder response = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null)
                response.append(line);
        }

        conn.disconnect();
        return response.toString();
    }
}
