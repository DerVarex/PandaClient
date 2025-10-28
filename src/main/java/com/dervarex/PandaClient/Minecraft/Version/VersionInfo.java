package com.dervarex.PandaClient.Minecraft.Version;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VersionInfo {
    public static final String MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";
    public static int getRequiredJavaVersion(String versionId) throws Exception {
        JSONObject manifest = null;
        manifest = new JSONObject(readUrl(MANIFEST_URL));
        JSONArray versions = manifest.getJSONArray("versions");

        String versionUrl = null;
        for (int i = 0; i < versions.length(); i++) {
            JSONObject obj = versions.getJSONObject(i);
            if (obj.getString("id").equalsIgnoreCase(versionId)) {
                versionUrl = obj.getString("url");
                break;
            }
        }

        if (versionUrl == null)
            throw new IllegalArgumentException("Version " + versionId + " not found in Mojang manifest");


        JSONObject versionData = new JSONObject(readUrl(versionUrl));


        if (!versionData.has("javaVersion"))
            return 8;

        JSONObject javaVersion = versionData.getJSONObject("javaVersion");
        return javaVersion.getInt("majorVersion");
    }

    private static String readUrl(String urlStr) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
        connection.setRequestProperty("User-Agent", "PandaClient-JavaChecker");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line);
            return sb.toString();
        }
    }
    public static Boolean supportsAddOpens(int javaVersion) {
        return javaVersion >= 9;
    }
}

