package com.dervarex.PandaClient.server;

import com.google.gson.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class PaperMC {

    public static int getLatestBuild(String version) throws IOException {
        String api = "https://api.papermc.io/v2/projects/paper/versions/" + version;
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
        return "https://api.papermc.io/v2/projects/paper/versions/" + version +
                "/builds/" + latest +
                "/downloads/paper-" + version + "-" + latest + ".jar";
    }

    public static void main(String[] args) throws IOException {
        System.out.println(getDownloadUrl("1.21.8"));
    }
}
