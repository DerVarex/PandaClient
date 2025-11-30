package com.dervarex.PandaClient.utils.NetUtils;

import com.dervarex.PandaClient.Minecraft.logger.ClientLogger;
import org.json.JSONObject;

import java.io.*;
import java.net.*;

public class NetUtils {
    /*
    * NetUtils
    * isOnline, isServerOnline,
    * TODO: add some utils here
     */

    public static void downloadFromUrl(String fileURL, String targetFilePath) {
        ClientLogger.log("Downloading " + fileURL + " -> " + targetFilePath, "INFO", "NetUtils.downloadFromURL");
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

                ClientLogger.log("Download completed: " + outFile.getAbsolutePath(), "INFO", "NetUtils.downloadFromURL");
            }

        } catch (IOException e) {
            ClientLogger.log("Download error: " + e.getMessage(), "ERROR", "NetUtils.downloadFromURL");
            e.printStackTrace();
        }
    }
    // in com.dervarex.PandaClient.utils.NetUtils.NetUtils
    public static File downloadToDirectoryPreserveName(String urlString, File targetDir) throws IOException {
        ClientLogger.log("Start download (preserve name): " + urlString + " -> " + targetDir, "INFO", "NetUtils.downloadToDirectoryPreserveName");

        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IOException("Could not create target dir: " + targetDir.getAbsolutePath());
        }

        String currentUrl = urlString;
        int redirects = 0;
        HttpURLConnection conn;

        // manuelle Redirect-Loop (max 10)
        while (true) {
            conn = (HttpURLConnection) new URL(currentUrl).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setInstanceFollowRedirects(false);
            conn.connect();

            int status = conn.getResponseCode();
            if (status >= 300 && status < 400) {
                String loc = conn.getHeaderField("Location");
                if (loc == null) throw new IOException("Redirect without Location header");
                // relativ -> absolut
                currentUrl = new URL(new URL(currentUrl), loc).toString();
                conn.disconnect();
                redirects++;
                if (redirects > 10) throw new IOException("Too many redirects");
                continue;
            }
            break;
        }

        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException("Download failed, status: " + status + " for url: " + currentUrl);
        }

        String contentType = conn.getContentType();
        // erlaubte content-types für JAR: archive/jar, application/java-archive, application/octet-stream
        if (contentType == null) contentType = "";
        boolean looksLikeJar = contentType.contains("java-archive") ||
                contentType.contains("application/octet-stream") ||
                contentType.contains("zip") ||
                contentType.contains("binary");

        // Bestimme Dateinamen: zuerst Content-Disposition, sonst Pfad der finalen URL
        String fileName = null;
        String cd = conn.getHeaderField("Content-Disposition");
        if (cd != null && cd.contains("filename=")) {
            // kann: attachment; filename="paper-1.21.8-60.jar"
            int idx = cd.indexOf("filename=");
            fileName = cd.substring(idx + 9).trim();
            if (fileName.startsWith("\"") && fileName.endsWith("\"")) {
                fileName = fileName.substring(1, fileName.length() - 1);
            }
        }

        if (fileName == null || fileName.isBlank()) {
            String path = new URL(currentUrl).getPath();
            fileName = new File(path).getName();
        }

        if (fileName == null || fileName.isBlank()) {
            throw new IOException("Could not determine filename for url: " + currentUrl);
        }

        // Wenn es nicht wie eine JAR aussieht, wirft Fehler (schutz)
        if (!looksLikeJar) {
            // Manche Server liefern keine content-type korrekt; erlauben trotzdem, aber loggen
            ClientLogger.log("Warning: downloaded content-type is '" + contentType + "' for " + currentUrl, "WARN", "NetUtils.downloadToDirectoryPreserveName");
        }

        File tmpFile = new File(targetDir, fileName + ".downloading");
        File outFile = new File(targetDir, fileName);

        try (InputStream in = conn.getInputStream();
             FileOutputStream fos = new FileOutputStream(tmpFile)) {

            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) != -1) {
                fos.write(buf, 0, read);
            }
            fos.flush();
        } finally {
            conn.disconnect();
        }

        // atomisch umbenennen
        if (!tmpFile.renameTo(outFile)) {
            // fallback: kopieren + löschen
            try (FileInputStream fis = new FileInputStream(tmpFile);
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = fis.read(buf)) != -1) fos.write(buf, 0, r);
            }
            tmpFile.delete();
        }

        ClientLogger.log("Download completed: " + outFile.getAbsolutePath(), "INFO", "NetUtils.downloadToDirectoryPreserveName");
        return outFile;
    }

    public static void downloadFromUrlWithRedirection(String fileURL, String targetFilePath) throws IOException {
        ClientLogger.log("Downloading " + fileURL + " -> " + targetFilePath, "INFO", "NetUtils.downloadFromUrlWithRedirection");

        File outFile = new File(targetFilePath);
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setInstanceFollowRedirects(false); // wir machen es manuell

        int status = connection.getResponseCode();
        if (status >= 300 && status < 400) {
            String redirect = connection.getHeaderField("Location");
            if (redirect == null) throw new IOException("Redirect without Location header!");
            ClientLogger.log("Redirecting to " + redirect, "INFO", "NetUtils.downloadFromUrlWithRedirection");
            connection.disconnect();
            downloadFromUrlWithRedirection(redirect, targetFilePath); // rekursiv weiter
            return;
        }

        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException("Download failed, HTTP status: " + status);
        }

        String contentType = connection.getContentType();
        if (!contentType.contains("application/java-archive") && !contentType.contains("octet-stream")) {
            throw new IOException("Downloaded file is not a JAR! Content-Type: " + contentType);
        }

        try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
             FileOutputStream out = new FileOutputStream(outFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        ClientLogger.log("Download completed: " + outFile.getAbsolutePath(), "INFO", "NetUtils.downloadFromUrlWithRedirection");
    }

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
