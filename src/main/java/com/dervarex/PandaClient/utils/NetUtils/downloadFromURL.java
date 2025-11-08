package com.dervarex.PandaClient.utils.NetUtils;

import com.dervarex.PandaClient.Minecraft.logger.ClientLogger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class downloadFromURL {
    public static void download(String fileURL, String targetFilePath) {
        ClientLogger.log("Downloading " + fileURL + " -> " + targetFilePath, "INFO", "downloadFromURL");
        try {
            File outFile = new File(targetFilePath);
            File parent = outFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs(); // ensure folder exists

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

                ClientLogger.log("Download completed: " + outFile.getAbsolutePath(), "INFO", "downloadFromURL");
            }

        } catch (IOException e) {
            ClientLogger.log("Download error: " + e.getMessage(), "ERROR", "downloadFromURL");
            e.printStackTrace();
        }
    }
}
