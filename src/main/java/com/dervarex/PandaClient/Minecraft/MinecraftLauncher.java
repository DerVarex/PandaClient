package com.dervarex.PandaClient.Minecraft;

import com.dervarex.PandaClient.Minecraft.loader.LoaderType;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
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
    // central assets dir (Vanilla expects /assets at root)
    private static final File GLOBAL_ASSETS_DIR = new File(BASE_DIR, "assets");

    /** Startet Vanilla Minecraft */
    public static void LaunchMinecraft(
            String version,
            LoaderType loader,
            String username,
            String uuid,
            String accessToken,
            File instanceName,
            boolean launchMc,
            Optional<Runnable> onComplete
    ) {
        try {
            ClientLogger.log("Starting Minecraft launcher", "INFO", "MinecraftLauncher");
            final File INSTANCE_DIR = new File(INSTANCES_BASE_DIR, instanceName.getName());

            // Create instance dir (saves / user-specific)
            Files.createDirectories(BASE_DIR.toPath());
            Files.createDirectories(INSTANCES_BASE_DIR.toPath());
            Files.createDirectories(INSTANCE_DIR.toPath());
            Files.createDirectories(GLOBAL_ASSETS_DIR.toPath());

            // Version ermitteln (immer mindestens 1.17)
            ClientLogger.log("Resolving version (min 1.17)...", "INFO", "MinecraftLauncher");
            String versionToLaunch = version.equals("latest") ? getLatestVersionAtLeast("1.17") : version;
            ClientLogger.log("Using version: " + versionToLaunch, "INFO", "MinecraftLauncher");

            // Version-Info laden
            JsonObject versionInfo = getVersionInfo(versionToLaunch, loader, INSTANCE_DIR);

            // Dateien herunterladen (werden in BASE_DIR/versions/<version>/... gespeichert)
            ClientLogger.log("Downloading Minecraft files...", "INFO", "MinecraftLauncher");
            downloadMinecraftFiles(versionInfo, INSTANCE_DIR, loader);

            // Starten
            if (launchMc) {
                ClientLogger.log("Launching Minecraft process", "INFO", "MinecraftLauncher");
                LogWindow logWindow = new LogWindow();
                startMinecraft(versionInfo, username, uuid, accessToken, INSTANCE_DIR, logWindow, versionToLaunch, loader, onComplete);
            }
        } catch (Exception e) {
            ClientLogger.log("Error launching Minecraft: " + e.getMessage(), "ERROR", "MinecraftLauncher");
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement ste : e.getStackTrace()) sb.append(ste.toString()).append('\n');
            ClientLogger.log(sb.toString(), "ERROR", "MinecraftLauncher");
        }
    }

    // ----------------------------- Version helpers (unchanged) -----------------------------
    public static String getLatestVersionAtLeast(String minVersion) throws IOException {
        String manifestJson = readUrlToString(MINECRAFT_API_URL);
        JsonObject manifest = JsonParser.parseString(manifestJson).getAsJsonObject();

        String latestRelease = manifest.getAsJsonObject("latest").get("release").getAsString();
        if (isVersionAtLeast(latestRelease, minVersion)) {
            ClientLogger.log("Latest release " + latestRelease + " satisfies min " + minVersion, "INFO", "MinecraftLauncher");
            return latestRelease;
        }

        JsonArray versions = manifest.getAsJsonArray("versions");
        String bestId = null;
        Instant bestTime = null;

        for (JsonElement el : versions) {
            JsonObject vObj = el.getAsJsonObject();
            String type = vObj.has("type") ? vObj.get("type").getAsString() : "release";
            if (!"release".equalsIgnoreCase(type)) continue;
            String id = vObj.get("id").getAsString();
            if (!isNumericDottedVersion(id)) continue;

            if (!isVersionAtLeast(id, minVersion)) continue;

            String t = vObj.has("releaseTime") ? vObj.get("releaseTime").getAsString() : null;
            Instant ti = null;
            if (t != null) {
                try { ti = Instant.parse(t); } catch (Exception ignored) {}
            }
            if (bestId == null) { bestId = id; bestTime = ti; }
            else {
                if (bestTime != null && ti != null) {
                    if (ti.isAfter(bestTime)) { bestId = id; bestTime = ti; }
                } else if (ti != null) { bestId = id; bestTime = ti; }
                else if (compareVersion(id, bestId) > 0) bestId = id;
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
        return getLatestVersionAtLeast("1.17");
    }

    public static JsonObject getVersionInfo(String version, LoaderType loader, File instanceDIR) throws IOException {
//        if(loader.equals(LoaderType.FABRIC)) {
//            File fabricVersionFolder = new File(instanceDIR + File.separator + "versions" + File.separator);
//            File fabricVersionJson;
//            for (File file : fabricVersionFolder.listFiles()) {
//                if (file.isDirectory() && file.getName().contains("fabric")) {
//                     fabricVersionJson = file;
//                }
//            }
//        }
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

    public static JsonObject getFabricVersionJson(
            File instanceDIR
    ) {
        File fabricVersionFolder = new File(instanceDIR + File.separator + "versions" + File.separator);
        File fabricVersionJson = null;
        for (File file : fabricVersionFolder.listFiles()) {
            if (file.isDirectory() && file.getName().contains("fabric")) {
                fabricVersionJson = new File(file + File.separator + file.getName() + ".json");
            }
        }
        try {
            String content = Files.readString(fabricVersionJson.toPath());
            return JsonParser.parseString(content).getAsJsonObject();
        } catch (IOException e) {
            ClientLogger.log("Failed to load Fabric version json: " + e.getMessage(), "ERROR", "MinecraftLauncher");
            return null;
        }
    }
    public static File getFabricVersionJsonFile(
            File instanceDIR
    ) {
        File fabricVersionFolder = new File(instanceDIR + File.separator + "versions" + File.separator);
        File fabricVersionJson = null;
        for (File file : fabricVersionFolder.listFiles()) {
            if (file.isDirectory() && file.getName().contains("fabric")) {
                fabricVersionJson = new File(file + File.separator + file.getName() + ".json");
            }
        }
            return fabricVersionJson;
    }

    // ----------------------------- Download & prepare files -----------------------------
    private static void downloadMinecraftFiles(JsonObject versionInfo, File INSTANCE_DIR, LoaderType loader) {
        try {
            String versionId = versionInfo.get("id").getAsString();
            File VERSION_DIR = new File(VERSIONS_BASE_DIR, versionId);
            File LIB_DIR = new File(VERSION_DIR, "libraries");
            File NATIVES_DIR = new File(VERSION_DIR, "natives");
            // assets are global
            File ASSETS_DIR = GLOBAL_ASSETS_DIR;

            Files.createDirectories(VERSION_DIR.toPath());
            Files.createDirectories(LIB_DIR.toPath());
            Files.createDirectories(NATIVES_DIR.toPath());
            Files.createDirectories(ASSETS_DIR.toPath());

            // 1) launcher jar

            if (versionInfo.has("downloads") && versionInfo.getAsJsonObject("downloads").has("client")) {
                JsonObject client = versionInfo.getAsJsonObject("downloads").getAsJsonObject("client");

                String clientjarname = "client.jar";

                if(loader == LoaderType.FABRIC) {
                    File jsonfile = getFabricVersionJsonFile(INSTANCE_DIR);
                    clientjarname = jsonfile.getName().replace(".json", "").replace("fabric-loader-", "") + ".jar"; // do it like the fabric installer wants it to be, without the struggle              Fabric Installer reference: String.format("%s-%s-%s", Reference.LOADER_NAME, loaderVersion.name, gameVersion);
                }
                File clientFile = new File(VERSION_DIR,clientjarname);

                if (!clientFile.exists() || clientFile.length() < 1024) {
                    ClientLogger.log("Downloading minecraft jar...", "INFO", "MinecraftLauncher");
                    safeDownload(client.get("url").getAsString(), clientFile);
                } else {
                    ClientLogger.log("minecraft jar exists, skipping", "INFO", "MinecraftLauncher");
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
                                if (!libFile.exists() || libFile.length() < 1024) {
                                    ClientLogger.log("Downloading lib: " + path, "INFO", "MinecraftLauncher");
                                    libFile.getParentFile().mkdirs();
                                    safeDownload(artifact.get("url").getAsString(), libFile);
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
                                    File nativeJarFile = new File(LIB_DIR, nativePath);
                                    if (!nativeJarFile.exists() || nativeJarFile.length() < 1024) {
                                        ClientLogger.log("Downloading native jar: " + nativeJarFile.getName(), "INFO", "MinecraftLauncher");
                                        nativeJarFile.getParentFile().mkdirs();
                                        safeDownload(nativeObj.get("url").getAsString(), nativeJarFile);
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

            // 3) Assets (index into global assets dir)
            if (versionInfo.has("assetIndex")) {
                JsonObject assetIndex = versionInfo.getAsJsonObject("assetIndex");
                String assetUrl = assetIndex.get("url").getAsString();
                String assetId = assetIndex.get("id").getAsString();
                File assetIndexFile = new File(ASSETS_DIR + "/indexes", assetId + ".json");
                Files.createDirectories(assetIndexFile.getParentFile().toPath());
                if (!assetIndexFile.exists() || assetIndexFile.length() < 100) {
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

    // minimal safe downloader: retries once, then tries wget fallback
    private static void safeDownload(String url, File target) throws IOException {
        Files.createDirectories(target.getParentFile().toPath());
        int attempts = 0;
        while (attempts < 2) {
            attempts++;
            ClientLogger.log("safeDownload attempt " + attempts + " -> " + target.getName(), "INFO", "Downloader");
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                ClientLogger.log("Download attempt failed: " + e.getMessage(), "WARN", "Downloader");
                ClientLogger.log("Trying wget fallback...", "WARN", "Downloader");
                try {
                    Process wget = new ProcessBuilder("wget", "-O", target.getAbsolutePath(), url).start();
                    boolean finished = wget.waitFor(60, TimeUnit.SECONDS);
                    if (!finished || wget.exitValue() != 0) {
                        ClientLogger.log("wget failed with exit code " + (finished ? wget.exitValue() : "timeout"), "WARN", "Downloader");
                    }
                } catch (Exception ex) {
                    ClientLogger.log("wget fallback failed: " + ex.getMessage() + " maybe it isn't installed", "ERROR", "Downloader");
                }
            }
            if (target.exists() && target.length() > 0) return;
            if (attempts >= 2) throw new IOException("Failed to download " + url);
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
     * Start minecraft process with given parameters.
     */
    private static void startMinecraft(JsonObject versionInfo,
                                       String username,
                                       String uuid,
                                       String accessToken,
                                       File INSTANCE_DIR,
                                       LogWindow logWindow,
                                       String version,
                                       LoaderType loader,
                                       Optional<Runnable> onComplete) throws IOException {

        String versionId = versionInfo.get("id").getAsString();
        File VERSION_DIR = new File(VERSIONS_BASE_DIR, versionId);
        File LIB_DIR = new File(VERSION_DIR, "libraries");
        File NATIVES_DIR = new File(VERSION_DIR, "natives");
        File ASSETS_DIR = GLOBAL_ASSETS_DIR;

        // Secure the client jar in instance dir
        String clientjarname = "client.jar";

        if(loader == LoaderType.FABRIC) {
            File jsonfile = getFabricVersionJsonFile(INSTANCE_DIR);
            clientjarname = jsonfile.getName().replace(".json", "").replace("fabric-loader-", "") + ".jar"; // do it like the fabric installer wants it to be, without the struggle              Fabric Installer reference: String.format("%s-%s-%s", Reference.LOADER_NAME, loaderVersion.name, gameVersion);
        }
        File clientJarInVersion = new File(VERSION_DIR, clientjarname);
        File clientJarInInstance = new File(INSTANCE_DIR, clientjarname);
        if (!clientJarInVersion.exists()) {
            throw new FileNotFoundException("minecraft jar not found at " + clientJarInVersion.getAbsolutePath());
        }
        if (!clientJarInInstance.exists()) {
            Files.createDirectories(INSTANCE_DIR.toPath());
            Files.copy(clientJarInVersion.toPath(), clientJarInInstance.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // Platform natives folder
        String platformFolder = getPlatformFolder(); // linux/windows/macos
        File PLATFORM_NATIVES_DIR = new File(NATIVES_DIR, platformFolder);
        Files.createDirectories(PLATFORM_NATIVES_DIR.toPath());

        // Unpack natives in libs if still present
        for (File libJar : findAllJars(LIB_DIR)) {
            String name = libJar.getName().toLowerCase();
            if (name.contains("natives") && name.endsWith(".jar")) {
                unpackNatives(libJar, PLATFORM_NATIVES_DIR);
            }
        }

        // sanity check: look for liblwjgl.so/dll/dylib
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
            ClientLogger.log("WARNING: No platform natives found in " + PLATFORM_NATIVES_DIR.getAbsolutePath() + ". Will attempt to re-extract from library jars.", "WARN", "MinecraftLauncher");
            for (File libJar : findAllJars(LIB_DIR)) {
                if (libJar.getName().toLowerCase().contains("natives") && libJar.getName().toLowerCase().endsWith(".jar")) {
                    unpackNatives(libJar, PLATFORM_NATIVES_DIR);
                }
            }
        } else {
            ClientLogger.log("Found platform natives in " + PLATFORM_NATIVES_DIR.getAbsolutePath(), "INFO", "MinecraftLauncher");
        }

        // --- LWJGL Version Filter
        List<File> jars = findAllJars(LIB_DIR);

        List<File> filtered = new ArrayList<>();
        for (File jar : jars) {
            boolean isVanillaAsm =
                    jar.getAbsolutePath().contains("versions")
                            && jar.getAbsolutePath().contains("org/ow2/asm");

            if (!isVanillaAsm) {
                filtered.add(jar);
            }
        }

        jars = filtered;

        jars.addAll(findAllJars(new File(INSTANCE_DIR + "/libraries")));



// Map: version -> list of lwjgl-related jars (core + modules)
        Map<String, List<File>> lwjglVersionMap = new HashMap<>();
        Pattern verPattern = Pattern.compile(".*-([0-9]+\\.[0-9]+\\.[0-9]+)\\.jar$");
        for (File jar : jars) {
            String lname = jar.getName().toLowerCase();
            if (!lname.contains("lwjgl")) continue;
            String ver = null;
            Matcher m = verPattern.matcher(lname);
            if (m.find()) ver = m.group(1);
            if (ver == null) {
                Matcher m2 = Pattern.compile("([0-9]+\\.[0-9]+\\.[0-9]+)").matcher(lname);
                if (m2.find()) ver = m2.group(1);
            }
            if (ver == null) {
                ClientLogger.log("Could not detect LWJGL version for " + jar.getName(), "WARN", "MinecraftLauncher");
                continue;
            }
            lwjglVersionMap.computeIfAbsent(ver, k -> new ArrayList<>()).add(jar);
        }

// choose highest lwjgl version available
        String highestVersion = null;
        for (String ver : lwjglVersionMap.keySet()) {
            if (highestVersion == null || compareVersion(ver, highestVersion) > 0) highestVersion = ver;
        }
        List<File> selectedLWJGLJars = highestVersion != null ? lwjglVersionMap.get(highestVersion) : Collections.emptyList();

// debug log which jars selected
        StringBuilder sel = new StringBuilder();
        for (File f : selectedLWJGLJars) sel.append(f.getName()).append(", ");
        ClientLogger.log("Selected LWJGL version: " + highestVersion + " jars: " + sel.toString(), "INFO", "MinecraftLauncher");

// Build classpath: include client + non-lwjgl jars + selected lwjgl jars
        StringJoiner cp = new StringJoiner(File.pathSeparator);
        cp.add(clientJarInInstance.getAbsolutePath());
        for (File jar : jars) {
            String lname = jar.getName().toLowerCase();
            if (lname.contains("lwjgl")) {
                if (!selectedLWJGLJars.contains(jar)) continue; // only chosen version's lwjgl jars
            }
            cp.add(jar.getAbsolutePath());
        }



        // Build command
        List<String> baseCommand = new ArrayList<>();
        String javaCmd;
        try {
            javaCmd = new JavaManager().getRequiredJavaVersionCommand(version);
        } catch (Throwable t1) {
            ClientLogger.log("Failed to get required Java command: " + t1.getMessage(), "WARN", "MinecraftLauncher");
            try {
                javaCmd = new JavaManager().getNewestJavaCommand();
            } catch (Throwable t2) {
                ClientLogger.log("Failed to get fallback Java command: " + t2.getMessage(), "ERROR", "MinecraftLauncher");
                throw new IOException("No usable Java found");
            }
        }
        baseCommand.add(javaCmd);

        // JVM Flags
        baseCommand.add("-Xmx2G");
        baseCommand.add("-Xms1G");
        baseCommand.add("-Dorg.lwjgl.librarypath=" + PLATFORM_NATIVES_DIR.getAbsolutePath());
        baseCommand.add("-Djava.library.path=" + PLATFORM_NATIVES_DIR.getAbsolutePath());
        if(loader.equals(LoaderType.FABRIC)) {
            baseCommand.add("-DFabricMcEmu= net.minecraft.client.main.Main");
        }
        // TODO: -Duser.language=en

        try {
            if (VersionInfo.supportsAddOpens(VersionInfo.getRequiredJavaVersion(version))) {
                baseCommand.add("--add-opens");
                baseCommand.add("java.base/java.lang.invoke=ALL-UNNAMED");
            }
        } catch (Exception e) {
            ClientLogger.log("Failed to determine Java version for --add-opens: " + e.getMessage(), "ERROR", "MinecraftLauncher");
        }

        // complete command
        List<String> command = new ArrayList<>(baseCommand);
        command.add("-cp");
        command.add(cp.toString());
        String mainClass = versionInfo.get("mainClass").getAsString();
        if(loader.equals(LoaderType.FABRIC)) {
            JsonObject fabricVersionJson = getFabricVersionJson(INSTANCE_DIR);
            if (fabricVersionJson != null && fabricVersionJson.has("mainClass")) {
                mainClass = fabricVersionJson.get("mainClass").getAsString();
                ClientLogger.log("Using Fabric main class: " + mainClass, "INFO", "MinecraftLauncher");
            } else {
                ClientLogger.log("Failed to get Fabric main class from version json, using vanilla main class: " + mainClass, "WARN", "MinecraftLauncher");
            }
        }
        command.add(mainClass);

        command.add("--username"); command.add(username);
        command.add("--uuid"); command.add(uuid);
        command.add("--accessToken"); command.add(accessToken);
        command.add("--version"); command.add(versionId);
        command.add("--gameDir"); command.add(INSTANCE_DIR.getAbsolutePath());
        command.add("--assetsDir"); command.add(ASSETS_DIR.getAbsolutePath());
        command.add("--clientId"); command.add(UUID.randomUUID().toString());
        if (versionInfo.has("assetIndex")) {
            JsonObject assetIndex = versionInfo.getAsJsonObject("assetIndex");
            command.add("--assetIndex");
            command.add(assetIndex.get("id").getAsString());
        }

        ClientLogger.log("Start args: " + String.join(" ", command), "INFO", "MinecraftLauncher");

        // Launch with retry for native crash
        boolean triedRemovingJemalloc = false;
        int attempt = 0;
        while (attempt < 2) {
            attempt++;
            Process process = new ProcessBuilder(command).directory(INSTANCE_DIR).start();

            StringBuilder stderrBuf = new StringBuilder();
            new Thread(() -> streamToLogger(process.getInputStream(), line -> {
                String lvl = parseLogLevel(line);
                if (logWindow != null) logWindow.log(lvl, line);
                else ClientLogger.log(line, lvl, "MinecraftProcess");
            }), "mc-stdout-reader").start();

            new Thread(() -> streamToLogger(process.getErrorStream(), line -> {
                if (logWindow != null) logWindow.log("ERROR", line);
                else ClientLogger.log(line, "ERROR", "MinecraftProcess");
                synchronized (stderrBuf) { stderrBuf.append(line).append('\n'); }
            }), "mc-stderr-reader").start();

            boolean exited;
         try { exited = process.waitFor(10, TimeUnit.SECONDS); } catch (InterruptedException e) { exited = false; }

            if (!exited) {
                ClientLogger.log("Minecraft process started and is running.", "INFO", "MinecraftLauncher");
                onComplete.ifPresent(Runnable::run);
                return;
            } else {
                int exit = process.exitValue();
                ClientLogger.log("Minecraft process exited quickly with code " + exit, exit == 0 ? "INFO" : "ERROR", "MinecraftLauncher");
                String stderr;
                synchronized (stderrBuf) { stderr = stderrBuf.toString(); }

                boolean nativeCrash = stderr.toLowerCase().contains("sigsegv") ||
                        stderr.toLowerCase().contains("jemalloc") ||
                        stderr.toLowerCase().contains("could not initialize class com.mojang.blaze3d.systems.rendersystem");

                if (nativeCrash && !triedRemovingJemalloc) {
                    ClientLogger.log("Detected native crash (jemalloc/SIGSEGV). Will remove jemalloc natives and retry once.", "WARN", "MinecraftLauncher");
                    File[] nfiles = PLATFORM_NATIVES_DIR.listFiles();
                    if (nfiles != null) {
                        for (File f : nfiles) {
                            String name = f.getName().toLowerCase();
                            if (name.contains("jemalloc")) {
                                try { Files.deleteIfExists(f.toPath()); } catch (IOException e) { ClientLogger.log("Failed to delete " + f.getName() + ": " + e.getMessage(), "WARN", "MinecraftLauncher"); }
                            }
                        }
                    }
                    triedRemovingJemalloc = true;
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                    continue;
                } else {
                    ClientLogger.log("Minecraft failed to start and no automatic fix left. See logs for details.", "ERROR", "MinecraftLauncher");
                    return;
                }
            }
        }
    }


    // ------------------- Helper: platform folder -------------------
    private static String getPlatformFolder() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac") || os.contains("darwin")) return "macos";
        return "linux";
    }

    // Helper: extract natives from a JAR (kept for completeness)
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
