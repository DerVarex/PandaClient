package com.dervarex.PandaClient.utils.NetUtils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;

public class NetUtils {
    /*
    * NetUtils
    * isOnline, isServerOnline,
    * TODO: add some utils here
     */

    public static boolean isOnline() {
        try {
            InetAddress inetAddress = InetAddress.getByName("8.8.8.8");
            return inetAddress.isReachable(2000);

        } catch (Exception e) {
            return false;
        }
    }

    public Boolean isServerOnline(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static JSONObject getJsonFromUrl(String urlString) throws Exception {
        // Verbindung aufbauen
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "PandaClient-Downloader"); // GitHub mag das
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        // Antwort lesen
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        // JSON parsen
        return new JSONObject(response.toString());
    }

}
