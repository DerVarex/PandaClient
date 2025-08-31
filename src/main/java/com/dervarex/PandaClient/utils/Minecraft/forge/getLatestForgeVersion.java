package com.dervarex.PandaClient.utils.Minecraft.forge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class getLatestForgeVersion {

    /**
     * Gibt die Download-URL für den Forge-Installer der neuesten Version
     * einer angegebenen Minecraft-Version zurück.
     *
     * Versucht zuerst recommended, dann latest.
     *
     * @param mcVersion Die Minecraft-Version (z.B. "1.20.1")
     * @return Download-Link als String oder null, wenn keine Version gefunden wurde
     */
    public static String getLatestForgeInstallerVersion(String mcVersion, Boolean onlyForgeVersion) {
        try {
            String apiUrl = "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json";
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(in, JsonObject.class);
            in.close();

            JsonObject promos = root.getAsJsonObject("promos");

            String recommendedKey = mcVersion + "-recommended";
            String latestKey = mcVersion + "-latest";

            String forgeVersion = null;

            if (promos.has(recommendedKey)) {
                forgeVersion = promos.get(recommendedKey).getAsString();
            } else if (promos.has(latestKey)) {
                forgeVersion = promos.get(latestKey).getAsString();
            } else {
                System.out.println("No Forge version found for " + mcVersion);
                return null;
            }

            String fullForgeVersion = mcVersion + "-" + forgeVersion;
            if(onlyForgeVersion) {
                return forgeVersion;
            } else {
                return fullForgeVersion;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
