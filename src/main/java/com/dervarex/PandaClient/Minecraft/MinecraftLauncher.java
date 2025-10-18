package com.dervarex.PandaClient.Minecraft;

import com.dervarex.PandaClient.Minecraft.logger.LogWindow;
import com.dervarex.PandaClient.utils.file.getPandaClientFolder;
import com.google.gson.*;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Consumer;

public class MinecraftLauncher {

    private static final String MINECRAFT_API_URL =
            "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final File BASE_DIR = getPandaClientFolder.getPandaClientFolder();
    private static final File INSTANCES_BASE_DIR = new File(BASE_DIR, "instances");

    /** Startet Vanilla Minecraft */
    public static void LaunchMinecraft(
            String version,
            String username,
            String uuid,
            String accessToken,
            File instanceName,
            boolean launchMc
    ) {
        try {
            System.out.println("Starte Minecraft-Launcher...");
            final File INSTANCE_DIR = new File(INSTANCES_BASE_DIR, instanceName.getName());
            final File LIB_DIR = new File(INSTANCE_DIR, "libraries");
            final File NATIVES_DIR = new File(INSTANCE_DIR, "natives");
            final File ASSETS_DIR = new File(INSTANCE_DIR, "assets");

            Files.createDirectories(BASE_DIR.toPath());
            Files.createDirectories(LIB_DIR.toPath());
            Files.createDirectories(NATIVES_DIR.toPath());
            Files.createDirectories(INSTANCES_BASE_DIR.toPath());
            Files.createDirectories(ASSETS_DIR.toPath());
            Files.createDirectories(INSTANCE_DIR.toPath());

            // Version ermitteln
            System.out.println("🔍 Suche Version...");
            String versionToLaunch = version.equals("latest") ? getLatestVersion() : version;
            System.out.println("📦 Lade Version: " + versionToLaunch);

            // Version-Info laden
            JsonObject versionInfo = getVersionInfo(versionToLaunch);

            // Dateien herunterladen
            System.out.println("⬇️ Lade Minecraft-Dateien...");
            downloadMinecraftFiles(versionInfo, INSTANCE_DIR, LIB_DIR, ASSETS_DIR);

            // Natives (Platzhalter)
            System.out.println("📦 Entpacke Natives (Platzhalter)");

            // Starten
            if (launchMc) {
                System.out.println("🚀 Starte Minecraft...");
                // create a LogWindow and pass it to the starter so each MC line is forwarded with levels
                LogWindow logWindow = new LogWindow();
                startMinecraft(ASSETS_DIR, versionInfo, username, uuid, accessToken,
                        LIB_DIR, INSTANCE_DIR.getPath(), logWindow);
            }
        } catch (Exception e) {
            System.err.println("❌ Fehler beim Starten von Minecraft: " + e.getMessage());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            System.err.println(sw.toString());
        }
    }

    public static String getLatestVersion() throws IOException {
        String json = readUrlToString(MINECRAFT_API_URL);
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        return jsonObject.getAsJsonObject("latest").get("release").getAsString();
    }

    public static JsonObject getVersionInfo(String version) throws IOException {
        String manifestJson = readUrlToString(MINECRAFT_API_URL);
        JsonObject manifest = JsonParser.parseString(manifestJson).getAsJsonObject();
        JsonArray versions = manifest.getAsJsonArray("versions");

        for (JsonElement v : versions) {
            JsonObject vObj = v.getAsJsonObject();
            if (vObj.get("id").getAsString().equals(version)) {
                String url = vObj.get("url").getAsString();
                String versionJson = readUrlToString(url);
                return JsonParser.parseString(versionJson).getAsJsonObject();
            }
        }
        throw new IOException("Version not found: " + version);
    }

    private static void downloadMinecraftFiles(JsonObject versionInfo,
                                               File INSTANCE_DIR,
                                               File LIB_DIR,
                                               File ASSETS_DIR) throws IOException {
        // client.jar
        File clientJarPath = new File(INSTANCE_DIR, "client.jar");
        if (!clientJarPath.exists()) {
            String clientJarUrl = versionInfo.getAsJsonObject("downloads")
                    .getAsJsonObject("client")
                    .get("url").getAsString();
            downloadFile(clientJarUrl, clientJarPath.getAbsolutePath());
            System.out.println("✅ client.jar downloaded.");
        }

        // Libraries
        if (versionInfo.has("libraries")) {
            JsonArray libraries = versionInfo.getAsJsonArray("libraries");
            for (JsonElement element : libraries) {
                JsonObject lib = element.getAsJsonObject();
                if (!lib.has("downloads")) continue;
                JsonObject downloads = lib.getAsJsonObject("downloads");
                if (!downloads.has("artifact")) continue;
                JsonObject artifact = downloads.getAsJsonObject("artifact");
                if (!artifact.has("url") || !artifact.has("path")) continue;

                String url = artifact.get("url").getAsString();
                String path = artifact.get("path").getAsString();
                File libFile = new File(LIB_DIR, path);

                if (!libFile.exists()) {
                    Files.createDirectories(libFile.getParentFile().toPath());
                    try (InputStream in = new URL(url).openStream();
                         FileOutputStream out = new FileOutputStream(libFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        }

        // Assets
        if (versionInfo.has("assetIndex")) {
            JsonObject assetIndex = versionInfo.getAsJsonObject("assetIndex");
            String assetUrl = assetIndex.get("url").getAsString();
            String assetId = assetIndex.get("id").getAsString();
            File assetIndexFile = new File(ASSETS_DIR + "/indexes", assetId + ".json");
            Files.createDirectories(assetIndexFile.getParentFile().toPath());
            downloadFile(assetUrl, assetIndexFile.getAbsolutePath());
            System.out.println("🖼️ Asset index downloaded: " + assetId);
            downloadAssets(assetIndexFile, new File(ASSETS_DIR, "objects"));
        }
    }

    private static void startMinecraft(File ASSETS_DIR,
                                       JsonObject versionInfo,
                                       String username,
                                       String uuid,
                                       String accessToken,
                                       File LIB_DIR,
                                       String INSTANCE_DIR,
                                       LogWindow logWindow) throws IOException {

        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-Xmx2G");
        command.add("-Xms1G");
        command.add("--add-opens");
        command.add("java.base/java.lang.invoke=ALL-UNNAMED");

        // Classpath
        StringJoiner cp = new StringJoiner(File.pathSeparator);
        cp.add(new File(INSTANCE_DIR, "client.jar").getAbsolutePath());
        for (File jar : findAllJars(LIB_DIR))
            cp.add(jar.getAbsolutePath());

        command.add("-cp");
        command.add(cp.toString());

        // Main class
        command.add(versionInfo.get("mainClass").getAsString());

        // Standard MC-Args
        command.add("--username");
        command.add(username);
        command.add("--uuid");
        command.add(uuid);
        command.add("--accessToken");
        command.add(accessToken);
        command.add("--version");
        command.add(versionInfo.get("id").getAsString());
        command.add("--gameDir");
        command.add(INSTANCE_DIR);
        command.add("--assetsDir");
        command.add(ASSETS_DIR.getAbsolutePath());

        JsonObject assetIndex = versionInfo.getAsJsonObject("assetIndex");
        command.add("--assetIndex");
        command.add(assetIndex.get("id").getAsString());

        System.out.println("Start args: " + command);
        Process process = new ProcessBuilder(command).directory(LIB_DIR).start();

        // Forward stdout -> parsed level
        new Thread(() -> streamToLogger(process.getInputStream(), line -> {
            String lvl = parseLogLevel(line);
            if (logWindow != null) logWindow.log(lvl, line);
            else System.out.println("[" + lvl + "] " + line);
        }), "mc-stdout-reader").start();

        // Forward stderr -> ERROR
        new Thread(() -> streamToLogger(process.getErrorStream(), line -> {
            if (logWindow != null) logWindow.log("ERROR", line);
            else System.err.println("[ERROR] " + line);
        }), "mc-stderr-reader").start();
    }

    // Helper to read a stream line-by-line and forward to logger
    private static void streamToLogger(InputStream in, Consumer<String> logger) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = r.readLine()) != null) {
                logger.accept(line);
            }
        } catch (IOException ignored) {}
    }

    // Simple heuristic to determine log level from a line
    private static String parseLogLevel(String line) {
        if (line == null) return "INFO";
        String up = line.toUpperCase(Locale.ROOT);
        if (up.contains("[ERROR]") || up.contains(" ERROR ") || up.contains("EXCEPTION") || up.contains("TRACE")) return "ERROR";
        if (up.contains("[WARN]") || up.contains(" WARN ") || up.contains("[WARNING]")) return "WARN";
        if (up.contains("[INFO]") || up.contains(" INFO ") || up.contains("FINE") || up.contains("DEBUG")) return "INFO";
        return "INFO";
    }

    private static String readUrlToString(String url) throws IOException {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(new URL(url).openStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static void downloadFile(String fileURL, String savePath) throws IOException {
        try (InputStream in = new URL(fileURL).openStream()) {
            Files.copy(in, Paths.get(savePath), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void downloadAssets(File assetIndexFile, File objectsDir) throws IOException {
        JsonObject index = JsonParser.parseReader(new FileReader(assetIndexFile)).getAsJsonObject();
        JsonObject objects = index.getAsJsonObject("objects");
        for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
            JsonObject obj = entry.getValue().getAsJsonObject();
            String hash = obj.get("hash").getAsString();
            String subDir = hash.substring(0, 2);
            File outFile = new File(objectsDir, subDir + "/" + hash);
            if (outFile.exists()) continue;
            Files.createDirectories(outFile.getParentFile().toPath());
            String url = "https://resources.download.minecraft.net/" + subDir + "/" + hash;
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static List<File> findAllJars(File dir) {
        List<File> jars = new ArrayList<>();
        if (!dir.exists()) return jars;
        for (File f : Objects.requireNonNull(dir.listFiles())) {
            if (f.isDirectory()) jars.addAll(findAllJars(f));
            else if (f.getName().toLowerCase().endsWith(".jar")) jars.add(f);
        }
        return jars;
    }
}
