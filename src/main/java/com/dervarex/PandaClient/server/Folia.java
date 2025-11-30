package com.dervarex.PandaClient.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class Folia {
    public static int getLatestBuild(String version) throws IOException {
        String api = "https://api.papermc.io/v2/projects/folia/versions/" + version;
        HttpURLConnection connection = (HttpURLConnection) new URL(api).openConnection();
        connection.setRequestMethod("GET");

        String json = new String(connection.getInputStream().readAllBytes());
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        JsonArray builds = obj.getAsJsonArray("builds");

        if (builds == null || builds.size() == 0) {
            throw new RuntimeException("No builds found for version " + version);
        }

        return builds.get(builds.size() - 1).getAsInt();
    }

    public static String getDownloadUrl(String version) throws IOException {
        int latest = getLatestBuild(version);
        return "https://api.papermc.io/v2/projects/folia/versions/" + version +
                "/builds/" + latest +
                "/downloads/folia-" + version + "-" + latest + ".jar";
    }

    public static void main(String[] args) {
        try {
            String version = "1.20.4"; // Example version
            String url = getDownloadUrl(version);
            System.out.println("Download URL for Folia " + version + ": " + url);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
