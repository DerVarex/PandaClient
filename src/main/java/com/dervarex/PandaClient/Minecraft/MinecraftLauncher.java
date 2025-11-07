package com.dervarex.PandaClient.Minecraft;

import com.dervarex.PandaClient.GUI.WebSocket.NotificationServer.NotificationServer;
import com.dervarex.PandaClient.Minecraft.logger.LogWindow;
import com.dervarex.PandaClient.Minecraft.logger.ClientLogger;
import com.dervarex.PandaClient.utils.file.getPandaClientFolder;
import com.google.gson.*;
import com.dervarex.PandaClient.utils.Java.JavaManager;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.dervarex.PandaClient.Minecraft.Version.VersionInfo;
import org.apache.commons.io.FileUtils;

public class MinecraftLauncher {

    private static final String MINECRAFT_API_URL =
            "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final File BASE_DIR = getPandaClientFolder.getPandaClientFolder();
    private static final File INSTANCES_BASE_DIR = new File(BASE_DIR, "instances");
    private static final File VERSIONS_BASE_DIR = new File(BASE_DIR, "versions");

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
            ClientLogger.log("Starting Minecraft launcher", "INFO", "MinecraftLauncher");
            final File INSTANCE_DIR = new File(INSTANCES_BASE_DIR, instanceName.getName());

            // Create instance dir (saves / user-specific)
            Files.createDirectories(BASE_DIR.toPath());
            Files.createDirectories(INSTANCES_BASE_DIR.toPath());
            Files.createDirectories(INSTANCE_DIR.toPath());

            // Version ermitteln (immer mindestens 1.17)
            ClientLogger.log("Resolving version (min 1.17)...", "INFO", "MinecraftLauncher");
            String versionToLaunch = version.equals("latest") ? getLatestVersionAtLeast("1.17") : version;
            ClientLogger.log("Using version: " + versionToLaunch, "INFO", "MinecraftLauncher");

            // Version-Info laden
            JsonObject versionInfo = getVersionInfo(versionToLaunch);

            // Dateien herunterladen (werden in BASE_DIR/versions/<version>/... gespeichert)
            ClientLogger.log("Downloading Minecraft files...", "INFO", "MinecraftLauncher");
            downloadMinecraftFiles(versionInfo, INSTANCE_DIR);

            // Starten
            if (launchMc) {
                ClientLogger.log("Launching Minecraft process", "INFO", "MinecraftLauncher");
                LogWindow logWindow = new LogWindow();
                startMinecraft(versionInfo, username, uuid, accessToken, INSTANCE_DIR, logWindow, versionToLaunch);
            }
        } catch (Exception e) {
            ClientLogger.log("Error launching Minecraft: " + e.getMessage(), "ERROR", "MinecraftLauncher");
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement ste : e.getStackTrace()) sb.append(ste.toString()).append('\n');
            ClientLogger.log(sb.toString(), "ERROR", "MinecraftLauncher");
        }
    }

    /**
     * Liefert die neueste Release-Version, die mindestens minVersion entspricht (z.B. "1.17").
     * Falls nichts gefunden wird, fällt es auf das offizielle latest.release zurück.
     */
    public static String getLatestVersionAtLeast(String minVersion) throws IOException {
        String manifestJson = readUrlToString(MINECRAFT_API_URL);
        JsonObject manifest = JsonParser.parseString(manifestJson).getAsJsonObject();

        String latestRelease = manifest.getAsJsonObject("latest").get("release").getAsString();
        if (isVersionAtLeast(latestRelease, minVersion)) {
            ClientLogger.log("Latest release " + latestRelease + " satisfies min " + minVersion, "INFO", "MinecraftLauncher");
            return latestRelease;
        }

        // sonst: suche die höchstmögliche release-version mit id >= minVersion
        JsonArray versions = manifest.getAsJsonArray("versions");
        String bestId = null;
        Instant bestTime = null;

        for (JsonElement el : versions) {
            JsonObject vObj = el.getAsJsonObject();
            String type = vObj.has("type") ? vObj.get("type").getAsString() : "release";
            if (!"release".equalsIgnoreCase(type)) continue; // nur releases
            String id = vObj.get("id").getAsString();
            if (!isNumericDottedVersion(id)) continue; // skip snapshots / weird ids

            if (!isVersionAtLeast(id, minVersion)) continue;

            String t = vObj.has("releaseTime") ? vObj.get("releaseTime").getAsString() : null;
            Instant ti = null;
            if (t != null) {
                try {
                    ti = Instant.parse(t);
                } catch (Exception ignored) {
                    // fallback: keep null
                }
            }
            if (bestId == null) {
                bestId = id;
                bestTime = ti;
            } else {
                // prefer newer by releaseTime if present, else lex compare id
                if (bestTime != null && ti != null) {
                    if (ti.isAfter(bestTime)) {
                        bestId = id;
                        bestTime = ti;
                    }
                } else if (ti != null) {
                    bestId = id;
                    bestTime = ti;
                } else if (compareVersion(id, bestId) > 0) {
                    bestId = id;
                }
            }
        }

        if (bestId != null) {
            ClientLogger.log("Selected release >= " + minVersion + ": " + bestId, "INFO", "MinecraftLauncher");
            return bestId;
        }

        ClientLogger.log("No release >= " + minVersion + " found; falling back to manifest latest.release: " + latestRelease, "WARN", "MinecraftLauncher");
        return latestRelease;
    }

    public static String getLatestVersion() throws IOException {
        // default: require at least 1.17
        return getLatestVersionAtLeast("1.17");
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
                ClientLogger.log("Version info loaded for " + version, "INFO", "MinecraftLauncher");
                return JsonParser.parseString(versionJson).getAsJsonObject();
            }
        }
        ClientLogger.log("Version not found: " + version, "ERROR", "MinecraftLauncher");
        throw new IOException("Version not found: " + version);
    }

    /**
     * Lädt client.jar, libraries, natives und assets in BASE_DIR/versions/<version>/*
     * und sorgt dafür, dass instance/client.jar vorhanden ist (kopiert falls nötig).
     */
    private static void downloadMinecraftFiles(JsonObject versionInfo, File INSTANCE_DIR) {
        try {
            String versionId = versionInfo.get("id").getAsString();
            File VERSION_DIR = new File(VERSIONS_BASE_DIR, versionId); // <- use VERSIONS_BASE_DIR
            File LIB_DIR = new File(VERSION_DIR, "libraries");
            File NATIVES_DIR = new File(VERSION_DIR, "natives");
            File ASSETS_DIR = new File(VERSION_DIR, "assets");

            Files.createDirectories(VERSION_DIR.toPath());
            Files.createDirectories(LIB_DIR.toPath());
            Files.createDirectories(NATIVES_DIR.toPath());
            Files.createDirectories(ASSETS_DIR.toPath());

            // 1) client.jar (speichere als client.jar)
            if (versionInfo.has("downloads") && versionInfo.getAsJsonObject("downloads").has("client")) {
                JsonObject client = versionInfo.getAsJsonObject("downloads").getAsJsonObject("client");
                File clientFile = new File(VERSION_DIR, "client.jar");
                if (!clientFile.exists()) {
                    ClientLogger.log("Downloading client.jar...", "INFO", "MinecraftLauncher");
                    FileUtils.copyURLToFile(new URL(client.get("url").getAsString()), clientFile);
                } else {
                    ClientLogger.log("client.jar exists, skipping", "INFO", "MinecraftLauncher");
                }
            } else {
                ClientLogger.log("No client entry in version json", "WARN", "MinecraftLauncher");
            }

            // OS classifier key and platform folder
            String osClassifier = getOS(); // e.g. "natives-linux"
            String platformFolder = getPlatformFolder(); // "linux"/"windows"/"macos"
            File PLATFORM_NATIVES_DIR = new File(NATIVES_DIR, platformFolder);
            Files.createDirectories(PLATFORM_NATIVES_DIR.toPath());

            // 2) Libraries & Natives: go through libraries and download artifacts + platform natives
            if (versionInfo.has("libraries")) {
                JsonArray libraries = versionInfo.getAsJsonArray("libraries");

                Set<String> downloadedPaths = new HashSet<>();
                int count = 0;

                for (JsonElement el : libraries) {
                    JsonObject lib = el.getAsJsonObject();
                    if (!lib.has("downloads")) continue;
                    JsonObject downloads = lib.getAsJsonObject("downloads");

                    // artifact (normal jar)
                    if (downloads.has("artifact")) {
                        JsonObject artifact = downloads.getAsJsonObject("artifact");
                        if (artifact.has("url") && artifact.has("path")) {
                            String path = artifact.get("path").getAsString();
                            if (!downloadedPaths.contains(path)) {
                                File libFile = new File(LIB_DIR, path);
                                if (!libFile.exists()) {
                                    ClientLogger.log("Downloading lib: " + path, "INFO", "MinecraftLauncher");
                                    libFile.getParentFile().mkdirs();
                                    FileUtils.copyURLToFile(new URL(artifact.get("url").getAsString()), libFile);
                                    count++;
                                }
                                downloadedPaths.add(path);
                            }
                        }
                    }

                    // classifiers (natives) -> only download platform native
                    if (downloads.has("classifiers")) {
                        JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                        if (classifiers.has(osClassifier)) {
                            JsonObject nativeObj = classifiers.getAsJsonObject(osClassifier);
                            if (nativeObj.has("url") && nativeObj.has("path")) {
                                String nativePath = nativeObj.get("path").getAsString();
                                if (!downloadedPaths.contains(nativePath)) {
                                    File nativeJarFile = new File(LIB_DIR, nativePath); // store native jars under libraries (like official)
                                    if (!nativeJarFile.exists()) {
                                        ClientLogger.log("Downloading native jar: " + nativeJarFile.getName(), "INFO", "MinecraftLauncher");
                                        nativeJarFile.getParentFile().mkdirs();
                                        FileUtils.copyURLToFile(new URL(nativeObj.get("url").getAsString()), nativeJarFile);
                                        count++;
                                    } else {
                                        ClientLogger.log("Native jar exists, skipping: " + nativeJarFile.getName(), "INFO", "MinecraftLauncher");
                                    }
                                    downloadedPaths.add(nativePath);

                                    // Immediately unpack the .so/.dll/.dylib into platform natives folder (flatten)
                                    unpackNatives(nativeJarFile, PLATFORM_NATIVES_DIR);
                                }
                            }
                        }
                    }
                }
                ClientLogger.log("Libraries/natives downloaded/verified: " + count, "INFO", "MinecraftLauncher");
            }

            // 3) Assets (unchanged)
            if (versionInfo.has("assetIndex")) {
                JsonObject assetIndex = versionInfo.getAsJsonObject("assetIndex");
                String assetUrl = assetIndex.get("url").getAsString();
                String assetId = assetIndex.get("id").getAsString();
                File assetIndexFile = new File(ASSETS_DIR + "/indexes", assetId + ".json");
                Files.createDirectories(assetIndexFile.getParentFile().toPath());
                if (!assetIndexFile.exists()) {
                    downloadFile(assetUrl, assetIndexFile.getAbsolutePath());
                    ClientLogger.log("Asset index downloaded: " + assetId, "INFO", "MinecraftLauncher");
                } else {
                    ClientLogger.log("Asset index exists, skipping: " + assetId, "INFO", "MinecraftLauncher");
                }
                downloadAssets(assetIndexFile, new File(ASSETS_DIR, "objects"));
            }

            ClientLogger.log("All libraries and natives downloaded.", "INFO", "MinecraftLauncher");
        } catch (Exception e) {
            ClientLogger.log("Failed to download Minecraft files: " + e.getMessage(), "ERROR", "MinecraftLauncher");
            e.printStackTrace();
        }
    }
    private static void unzip(File zipFile, File destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        zis.transferTo(fos);
                    }
                }
            }
        }
    }



    /**
     * Entpackt native Bibliotheken aus einem natives-*.jar in ein flaches natives-Verzeichnis.
     * Extrahiert nur Dateitypen: .so, .dll, .dylib und benennt sie flach (keine Unterordner).
     */
    private static void unpackNatives(File nativeJar, File nativesDir) {
        try {
            if (!nativeJar.exists()) {
                ClientLogger.log("Native JAR not found for unpacking: " + nativeJar.getAbsolutePath(), "WARN", "MinecraftLauncher");
                return;
            }
            Files.createDirectories(nativesDir.toPath());

            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new FileInputStream(nativeJar))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;
                    String name = entry.getName();
                    String lower = name.toLowerCase();
                    if (lower.endsWith(".so") || lower.endsWith(".dll") || lower.endsWith(".dylib")) {
                        File outFile = new File(nativesDir, new File(name).getName()); // flatten
                        if (outFile.exists()) {
                            ClientLogger.log("Native already exists, skipping: " + outFile.getName(), "INFO", "MinecraftLauncher");
                            continue;
                        }
                        Files.createDirectories(outFile.getParentFile().toPath());
                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = zis.read(buffer)) > 0) fos.write(buffer, 0, len);
                        }
                        // Set permission (best effort)
                        try {
                            outFile.setReadable(true, false);
                            outFile.setExecutable(true, false);
                        } catch (Throwable t) {
                            ClientLogger.log("Could not set permissions on native: " + outFile.getName(), "WARN", "MinecraftLauncher");
                        }
                        ClientLogger.log("Extracted native: " + outFile.getName(), "INFO", "MinecraftLauncher");
                    }
                }
            }
        } catch (IOException e) {
            ClientLogger.log("Error unpacking natives: " + e.getMessage(), "ERROR", "MinecraftLauncher");
        }
    }

    /**
     * Startet Minecraft. Nutzt version-spezifische libraries/natives aus BASE_DIR/versions/<version>
     */
    private static void startMinecraft(JsonObject versionInfo,
                                       String username,
                                       String uuid,
                                       String accessToken,
                                       File INSTANCE_DIR,
                                       LogWindow logWindow,
                                       String version) throws IOException {

        String versionId = versionInfo.get("id").getAsString();
        File VERSION_DIR = new File(VERSIONS_BASE_DIR, versionId);
        File LIB_DIR = new File(VERSION_DIR, "libraries");
        File NATIVES_DIR = new File(VERSION_DIR, "natives");
        File ASSETS_DIR = new File(VERSION_DIR, "assets");

        // ensure client.jar exists in version dir (client.jar) and copy to instance
        File clientJarInVersion = new File(VERSION_DIR, "client.jar");
        File clientJarInInstance = new File(INSTANCE_DIR, "client.jar");
        if (!clientJarInVersion.exists()) {
            throw new FileNotFoundException("client.jar not found at " + clientJarInVersion.getAbsolutePath());
        }
        if (!clientJarInInstance.exists()) {
            Files.createDirectories(INSTANCE_DIR.toPath());
            Files.copy(clientJarInVersion.toPath(), clientJarInInstance.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // platform natives folder (natives/<platform>)
        String platformFolder = getPlatformFolder(); // linux/windows/macos
        File PLATFORM_NATIVES_DIR = new File(NATIVES_DIR, platformFolder);
        Files.createDirectories(PLATFORM_NATIVES_DIR.toPath());

        // If there are still .jar natives in libraries (rare), try to unpack them too
        // (some native jars were saved into LIB_DIR path structure)
        for (File libJar : findAllJars(LIB_DIR)) {
            String name = libJar.getName().toLowerCase();
            if (name.contains("natives") && name.endsWith(".jar")) {
                // unpack to platform natives (will skip existing files)
                unpackNatives(libJar, PLATFORM_NATIVES_DIR);
            }
        }

        // sanity check: look for liblwjgl.so (Linux) or corresponding files
        boolean hasNative = false;
        File[] nativeFiles = PLATFORM_NATIVES_DIR.listFiles();
        if (nativeFiles != null) {
            for (File f : nativeFiles) {
                String n = f.getName().toLowerCase();
                if (n.endsWith(".so") || n.endsWith(".dll") || n.endsWith(".dylib")) {
                    if (n.contains("lwjgl") || n.contains("openal") || n.contains("jemalloc") || n.contains("stb")) {
                        hasNative = true;
                        break;
                    }
                }
            }
        }
        if (!hasNative) {
            ClientLogger.log("WARNING: No platform natives found in " + PLATFORM_NATIVES_DIR.getAbsolutePath() + ". Minecraft will likely fail to load LWJGL.", "WARN", "MinecraftLauncher");
        } else {
            ClientLogger.log("Found platform natives in " + PLATFORM_NATIVES_DIR.getAbsolutePath(), "INFO", "MinecraftLauncher");
        }

        // Build command
        List<String> command = new ArrayList<>();
        String javaCmd;
        try {
            javaCmd = new JavaManager().getRequiredJavaVersionCommand(version);
        } catch (Throwable t) {
            ClientLogger.log("Failed to get Java command: " + t.getMessage(), "ERROR", "MinecraftLauncher");
            javaCmd = new JavaManager().getNewestJavaCommand();
        }
        command.add(javaCmd);

        // JVM Flags
        command.add("-Xmx2G");
        command.add("-Xms1G");
        // set native paths
        command.add("-Dorg.lwjgl.librarypath=" + PLATFORM_NATIVES_DIR.getAbsolutePath());
        command.add("-Djava.library.path=" + PLATFORM_NATIVES_DIR.getAbsolutePath());
        // optional debug - comment out if noisy
        // command.add("-Dorg.lwjgl.util.Debug=true");
        // command.add("-Dorg.lwjgl.util.DebugLoader=true");

        // add --add-opens if required for Java 16+
        try {
            if (VersionInfo.supportsAddOpens(VersionInfo.getRequiredJavaVersion(version))) {
                command.add("--add-opens");
                command.add("java.base/java.lang.invoke=ALL-UNNAMED");
            }
        } catch (Exception e) {
            ClientLogger.log("Failed to determine Java version for --add-opens: " + e.getMessage(), "ERROR", "MinecraftLauncher");
        }

        // Classpath - all library jars + instance client.jar
        StringJoiner cp = new StringJoiner(File.pathSeparator);
        cp.add(clientJarInInstance.getAbsolutePath());
        for (File jar : findAllJars(LIB_DIR)) cp.add(jar.getAbsolutePath());
        command.add("-cp");
        command.add(cp.toString());

        // Main class and MC args
        String mainClass = versionInfo.get("mainClass").getAsString();
        command.add(mainClass);

        command.add("--username"); command.add(username);
        command.add("--uuid"); command.add(uuid);
        command.add("--accessToken"); command.add(accessToken);
        command.add("--version"); command.add(versionId);
        command.add("--gameDir"); command.add(INSTANCE_DIR.getAbsolutePath());
        command.add("--assetsDir"); command.add(ASSETS_DIR.getAbsolutePath());
        if (versionInfo.has("assetIndex")) {
            JsonObject assetIndex = versionInfo.getAsJsonObject("assetIndex");
            command.add("--assetIndex");
            command.add(assetIndex.get("id").getAsString());
        }

        ClientLogger.log("Start args: " + String.join(" ", command), "INFO", "MinecraftLauncher");

        Process process = new ProcessBuilder(command).directory(INSTANCE_DIR).start();

        new Thread(() -> streamToLogger(process.getInputStream(), line -> {
            String lvl = parseLogLevel(line);
            if (logWindow != null) logWindow.log(lvl, line);
            else ClientLogger.log(line, lvl, "MinecraftProcess");
        }), "mc-stdout-reader").start();

        new Thread(() -> streamToLogger(process.getErrorStream(), line -> {
            if (logWindow != null) logWindow.log("ERROR", line);
            else ClientLogger.log(line, "ERROR", "MinecraftProcess");
        }), "mc-stderr-reader").start();
    }

    // ------------------- Helper: platform folder -------------------
    private static String getPlatformFolder() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac") || os.contains("darwin")) return "macos";
        return "linux";
    }


    // Helper: extract natives from a JAR
    private static void extractNatives(File nativesJar, File outputDir) throws IOException {
        try (JarFile jar = new JarFile(nativesJar)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                File outFile = new File(outputDir, entry.getName());
                outFile.getParentFile().mkdirs();
                try (InputStream is = jar.getInputStream(entry);
                     FileOutputStream os = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
                }
            }
        }
    }




    // Version comparator: returns negative if a < b
    private static int compareVersion(String a, String b) {
        try {
            String[] aa = a.split("\\.");
            String[] bb = b.split("\\.");
            int len = Math.max(aa.length, bb.length);
            for (int i = 0; i < len; i++) {
                int ai = i < aa.length ? parseLeadingInt(aa[i]) : 0;
                int bi = i < bb.length ? parseLeadingInt(bb[i]) : 0;
                if (ai != bi) return ai - bi;
            }
        } catch (Throwable t) {
            // fallback
            return a.compareTo(b);
        }
        return 0;
    }

    private static int parseLeadingInt(String tok) {
        try {
            Matcher m = Pattern.compile("^(\\d+)").matcher(tok);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Throwable ignored) {}
        return 0;
    }

    // Helper: is id like "1.17" or "1.17.1" (numeric dotted) — skip snapshots like "20w14a"
    private static boolean isNumericDottedVersion(String id) {
        return id != null && id.matches("^\\d+\\.\\d+.*");
    }

    // Checks if versionId >= minVersion numerically, expects numeric dotted ids
    private static boolean isVersionAtLeast(String versionId, String minVersion) {
        if (!isNumericDottedVersion(versionId) || !isNumericDottedVersion(minVersion)) return false;
        String[] va = versionId.split("\\.");
        String[] vb = minVersion.split("\\.");
        int len = Math.max(va.length, vb.length);
        for (int i = 0; i < len; i++) {
            int ai = i < va.length ? parseLeadingInt(va[i]) : 0;
            int bi = i < vb.length ? parseLeadingInt(vb[i]) : 0;
            if (ai != bi) return ai > bi;
        }
        return true; // equal
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
        ClientLogger.log("Assets downloaded", "INFO", "MinecraftLauncher");
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

    private static String getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) return "natives-linux";
        if (os.contains("windows")) return "natives-windows";
        if (os.contains("mac")) return "natives-macos";
        return "natives-linux";
    }
}
