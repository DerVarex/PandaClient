package com.dervarex.PandaClient.utils.WebUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class downloadFromURL {
    public static void download(String fileURL, String targetFilePath) {
        try {
            File outFile = new File(targetFilePath);
            outFile.getParentFile().mkdirs(); // Ordner erstellen falls nicht vorhanden

            // Verbindung mit Redirect Support
            HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setInstanceFollowRedirects(true);

            try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(outFile)) {

                byte[] dataBuffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = in.read(dataBuffer, 0, 8192)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }

                System.out.println("Download abgeschlossen: " + outFile.getAbsolutePath());
            }

        } catch (IOException e) {
            System.out.println("Fehler beim Download: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
