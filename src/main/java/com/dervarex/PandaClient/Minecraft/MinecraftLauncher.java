package com.dervarex.PandaClient.Minecraft;


import com.dervarex.PandaClient.utils.OS.OSUtil;
import com.dervarex.PandaClient.utils.Minecraft.forge.getLatestForgeVersion;
import com.dervarex.PandaClient.utils.Minecraft.forge.utils.ForgeGameArgs;
import com.dervarex.PandaClient.utils.Minecraft.forge.utils.JarHandling;
import com.dervarex.PandaClient.utils.Minecraft.forge.utils.Json;
import com.dervarex.PandaClient.utils.Minecraft.forge.utils.VanillaGameArgs;
import com.dervarex.PandaClient.utils.file.getPandaClientFolder;
import com.google.gson.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class MinecraftLauncher {


    private static final String MINECRAFT_API_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final File BASE_DIR = getPandaClientFolder.getPandaClientFolder();
    private static final File INSTANCES_BASE_DIR = new File(BASE_DIR, "instances");


    public static void LaunchMinecraft(String version, String username, String uuid, String accessToken, File instanceName, Boolean launchMc, Boolean installForge) {
        try {
            System.out.println("Starte Minecraft-Launcher...");

            final File INSTANCE_DIR = new File(INSTANCES_BASE_DIR + "\\" + instanceName.getName());
            final File LIB_DIR = new File(INSTANCE_DIR + "\\libraries");
            final File NATIVES_DIR = new File(INSTANCE_DIR + "\\natives");
            final File ASSETS_DIR = new File(INSTANCE_DIR + "\\assets");


            // Ordner erstellen, falls nicht vorhanden
            Files.createDirectories(BASE_DIR.toPath());
            Files.createDirectories(LIB_DIR.toPath());
            Files.createDirectories(NATIVES_DIR.toPath());
            Files.createDirectories(INSTANCES_BASE_DIR.toPath());
            Files.createDirectories(ASSETS_DIR.toPath());
            Files.createDirectories(INSTANCE_DIR.toPath());

            // Aktuelle Version holen
            System.out.println("🔍 Suche Version...");
            String versionToLaunch = version.equals("latest") ? getLatestVersion() : version;
            System.out.println("📦 Lade Version: " + versionToLaunch);

            // Version-Info holen
            JsonObject versionInfo = getVersionInfo(versionToLaunch);

            // Client.jar und Libraries laden
            System.out.println("⬇️ Lade Minecraft-Dateien...");
            downloadMinecraftFiles(versionInfo, INSTANCE_DIR, LIB_DIR, ASSETS_DIR);

            // Natives entpacken
            System.out.println("📦 Entpacke Natives...");
            extractNatives(versionInfo);

            // Minecraft starten
            if(launchMc = true) {
                System.out.println("🚀 Starte Minecraft...");
                startMinecraft(installForge, ASSETS_DIR, versionInfo, username, uuid, accessToken, LIB_DIR, INSTANCE_DIR.getPath(), version);
            }
        } catch (Exception e) {
            System.err.println("❌ Fehler beim Starten von Minecraft:");
            e.printStackTrace();
        }
    }

    public static String getLatestVersion() throws IOException {
        String json = readUrlToString(MINECRAFT_API_URL);
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        return jsonObject.getAsJsonObject("latest").get("release").getAsString();
    }

    public static JsonObject getVersionInfo(String version) throws IOException {
        // Version-Manifest neu laden (da Pfad sich ändert)
        String manifestJson = readUrlToString(MINECRAFT_API_URL);
        JsonObject manifest = JsonParser.parseString(manifestJson).getAsJsonObject();
        JsonArray versions = manifest.getAsJsonArray("versions");

        String versionUrl = null;
        for (JsonElement v : versions) {
            JsonObject vObj = v.getAsJsonObject();
            if (vObj.get("id").getAsString().equals(version)) {
                versionUrl = vObj.get("url").getAsString();
                break;
            }
        }

        if (versionUrl == null) throw new IOException("Version not found: " + version);

        String versionJson = readUrlToString(versionUrl);
        return JsonParser.parseString(versionJson).getAsJsonObject();
    }

    private static void downloadMinecraftFiles(JsonObject versionInfo, File INSTANCE_DIR, File LIB_DIR, File ASSETS_DIR) throws IOException {
        // client.jar
        File clientJarPath = new File(INSTANCE_DIR + File.separator + "client.jar");
        if (!clientJarPath.exists()) {
            String clientJarUrl = versionInfo.getAsJsonObject("downloads").getAsJsonObject("client").get("url").getAsString();
            downloadFile(clientJarUrl, clientJarPath.getAbsolutePath());
            System.out.println("✅ client.jar downloaded.");
        } else {
            System.out.println("Client.jar already exists. Skipping download.");
        }


        if (versionInfo.has("libraries")) {
            JsonArray libraries = versionInfo.getAsJsonArray("libraries");

            Map<String, JsonObject> libMap = new HashMap<>();

            for (JsonElement element : libraries) {
                JsonObject library = element.getAsJsonObject();

                if (!library.has("name") || !library.has("downloads")) continue;
                JsonObject downloads = library.getAsJsonObject("downloads");
                if (!downloads.has("artifact")) continue;
                JsonObject artifact = downloads.getAsJsonObject("artifact");
                if (!artifact.has("url") || !artifact.has("path")) continue;

                // → "org.ow2.asm:asm:9.2" → "org.ow2.asm:asm"
                String fullName = library.get("name").getAsString();
                String[] parts = fullName.split(":");
                if (parts.length < 2) continue;
                String key = parts[0] + ":" + parts[1]; // group:artifact


                /*if (libMap.containsKey(key)) {
                    System.out.println("Duplicate lib found: " + key + " – Skipping older one.");
                    continue;
                } */

                libMap.put(key, artifact);
            }


            for (Map.Entry<String, JsonObject> entry : libMap.entrySet()) {
                JsonObject artifact = entry.getValue();

                String url = artifact.get("url").getAsString();
                String path = artifact.get("path").getAsString();
                File libFile = new File("libraries", path);

                if (!libFile.exists()) {
                    libFile.getParentFile().mkdirs();
                    try (InputStream in = new URL(url).openStream();
                         FileOutputStream out = new FileOutputStream(libFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    } catch (IOException e) {
                        System.err.println("Error downloading library: " + url);
                        e.printStackTrace();
                    }
                }
            }
        }
/*
        if (versionInfo.has("libraries")) {
            JsonArray libraries = versionInfo.getAsJsonArray("libraries");
            for (JsonElement element : libraries) {
                JsonObject library = element.getAsJsonObject();

                if (!library.has("downloads")) continue;
                JsonObject downloads = library.getAsJsonObject("downloads");

                if (!downloads.has("artifact")) continue;
                JsonObject artifact = downloads.getAsJsonObject("artifact");

                if (!artifact.has("url") || !artifact.has("path")) continue;

                String url = artifact.get("url").getAsString();
                String path = artifact.get("path").getAsString();

                File libFile = new File("libraries", path);
                if (!libFile.exists()) {
                    libFile.getParentFile().mkdirs();
                    try (InputStream in = new URL(url).openStream();
                         FileOutputStream out = new FileOutputStream(libFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    } catch (IOException e) {
                        System.err.println("Error downloading library: " + url);
                        e.printStackTrace();
                    }
                }
            }
        } */



        if (versionInfo.has("assetIndex")) {
            System.out.println("Loading Asset index from: " + versionInfo.getAsJsonObject("assetIndex").get("url").getAsString() + " ...");
            JsonObject assetIndex = versionInfo.getAsJsonObject("assetIndex");
            String assetUrl = assetIndex.get("url").getAsString();
            String assetId = assetIndex.get("id").getAsString();
            File assetIndexFile = new File(ASSETS_DIR + File.separator + "indexes" + File.separator,  assetId + ".json");
            System.out.println("Downloading now Asset index NOW!" + assetId + " to: " + assetIndexFile.getAbsolutePath() + " ...");
            assetIndexFile.getParentFile().mkdirs();
            try {
                downloadFile(assetUrl, assetIndexFile.getAbsolutePath());
            } catch (Exception e) {
                System.out.println("Error while   downloadFile(assetUrl, assetIndexFile.getAbsolutePath());   from" + assetUrl + " to" + assetIndexFile.getAbsolutePath());
                System.out.println("Error: " + e.getMessage());
            }
            System.out.println("🖼️ Asset index downloaded: " + assetId);

            System.out.println("Downloading assets textures and sound...");
            downloadAssets(assetIndexFile, new File(ASSETS_DIR + File.separator + "objects"));


        } else {
            System.out.println("No Assets index found in version manifest: " + versionInfo.get("id").getAsString() + " (" + versionInfo.toString() + ") - Skipping assets download.");
        }


    }


    private static void extractNatives(JsonObject versionInfo) throws IOException {
        //Platzhalter
        System.out.println("(Natives entpacken - Platzhalter)");
    }

    private static void startMinecraft(
            boolean installForge,
            File ASSETS_DIR,
            JsonObject versionInfo,
            String username,
            String uuid,
            String accessToken,
            File LIB_DIR,
            String INSTANCE_DIR,
            String version
    ) throws IOException {
        OSUtil.OS SystemOS = OSUtil.getOS();
        List<String> command = new ArrayList<>();

        // Java Befehl + RAM
        command.add("java");
        command.add("-Xmx2G");
        command.add("-Xms1G");
        //Required by... the compiler?
        command.add("--add-opens");
        command.add("java.base/java.lang.invoke=ALL-UNNAMED");

        // Classpath zusammensetzen: Alle .jar-Dateien + client.jar
        // rekursiv alle JAR-Dateien finden (inkl. Unterordner) REMOVED
        //String forgeVersion = new getLatestForgeVersion(version);
        String forgeVersion = getLatestForgeVersion.getLatestForgeInstallerVersion(version, true);
        File forgeJsonFile = new File(INSTANCE_DIR + File.separator + "versions"
                + File.separator + version + "-forge-" + forgeVersion
                + File.separator + version + "-forge-" + forgeVersion + ".json");
        String vanillaJsonUrl = getVanillaClientJsonUrl(version);
        File librariesDir = new File(LIB_DIR.toURI());

        List<File> jars = getRequiredClientJars(vanillaJsonUrl, forgeJsonFile, librariesDir);

// optional: Ausgabe oder Weiterverarbeitung
        for (File jar : jars) {
            System.out.println(jar.getAbsolutePath());
        }

        List<File> allLibs = jars; // REMOVED findAllJars(LIB_DIR);

// Classpath String zusammenbauen mit ; als Trenner (Windows)
        StringJoiner classpathJoiner = new StringJoiner(";");

// Alle gefundenen JARs hinzufügen, wenn sie fürs derzeitige os sind
        for (File lib : allLibs) {
            String name = lib.getName().toLowerCase();
            // "windows", "linux", "macos"

            boolean mentionsWindows = name.contains("windows");
            boolean mentionsLinux = name.contains("linux");
            boolean mentionsMac = name.contains("macos");

            boolean mentionsAnyOS = mentionsWindows || mentionsLinux || mentionsMac;

            if (mentionsAnyOS && !name.contains(SystemOS.toString())) {
                System.out.println("Not for the current OS (" + SystemOS.toString() + "): " + lib.getAbsolutePath());
                continue; // Ignorieren
            }

            classpathJoiner.add(lib.getAbsolutePath()); // Nur passende oder generische hinzufügen
            System.out.println("Lib hinzugefügt: " + lib.getAbsolutePath());
        }


        classpathJoiner.add(INSTANCE_DIR + File.separator + "client.jar");


        System.out.println("Classpath to use: " + classpathJoiner.toString());

        command.add("-cp");
        command.add(classpathJoiner.toString());

        // Main-Class abhängig von Forge oder nicht
        if (!installForge) {
            // Vanilla: Main-Class aus Version.json verwenden
            String mainClass = versionInfo.get("mainClass").getAsString();
            command.add(mainClass);
        } else {
            // Forge: Main-Class + zusätzliche Game-Args aus Forge-Version.json lesen
            Gson gson = new Gson();



            System.out.println("Path to Forge JSON: " + forgeJsonFile.getAbsolutePath());

            if (!forgeJsonFile.exists()) {
                System.err.println("Forge JSON file not found: " + forgeJsonFile.getAbsolutePath());
                return;
            }

            try (FileReader reader = new FileReader(forgeJsonFile)) {
                JsonObject forgeJson = gson.fromJson(reader, JsonObject.class);

                // Main-Class aus Forge JSON
                String mainClass = forgeJson.get("mainClass").getAsString();
                command.add(mainClass);

                command.add("--launchTarget");
                command.add("forgeclient");


                // Game-Argumente anhängen
                String vanillaJsonURL = getVanillaClientJsonUrl(version);
                command.add(String.valueOf(ForgeGameArgs.gameArgs(forgeJson, LIB_DIR, version)));
                command.add(String.valueOf(VanillaGameArgs.gameArgs(vanillaJsonURL, LIB_DIR, version)));

                /*if (forgeJson.has("arguments")) {
                    JsonObject arguments = forgeJson.getAsJsonObject("arguments");
                    if (arguments.has("jvm")) {
                        JsonArray jvmArgs = arguments.getAsJsonArray("jvm");


                        String versionName = version;
                        String libraryDirectory =LIB_DIR.getAbsolutePath();
                        String classpathSeparator = System.getProperty("os.name").toLowerCase().contains("win") ? ";" : ":";

                        for (JsonElement jvmElement : jvmArgs) {
                            String arg = jvmElement.getAsString();

                            // Platzhalter ersetzen
                            arg = arg.replace("${version_name}", versionName)
                                    .replace("${library_directory}", libraryDirectory)
                                    .replace("${classpath_separator}", classpathSeparator);

                            command.add(arg);
                        }
                    } else {
                        System.err.println("Keine 'jvm' in Forge JSON gefunden.");
                    }

                    if (arguments.has("game")) {
                        JsonArray gameArgs = arguments.getAsJsonArray("game");
                        for (JsonElement argElement : gameArgs) {
                            command.add(argElement.getAsString());
                        }
                    } else {
                        System.err.println("Keine 'game'-Argumente in Forge JSON gefunden.");
                    }

                } else {
                    System.err.println("Keine 'arguments' in Forge JSON gefunden.");
                } */

            } catch (IOException | JsonParseException e) {
                System.err.println("Fehler beim Einlesen der Forge JSON: " + e.getMessage());
                return;
            }
        }

        // Vanilla-Standard Argumente (werden auch von Forge genutzt)
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


        // Asset-Index anhängen
        JsonObject assetIndex = versionInfo.getAsJsonObject("assetIndex");
        if (assetIndex != null && assetIndex.has("id")) {
            command.add("--assetIndex");
            command.add(assetIndex.get("id").getAsString());
        } else {
            throw new IllegalStateException("Asset Index fehlt in Version JSON: " + versionInfo.toString());
        }
        System.out.println("Start args for mc: " + command.toString());
        // Prozess starten
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(LIB_DIR);
        builder.inheritIO(); // Konsolenausgabe anzeigen
        builder.start();
    }



    private static String readUrlToString(String urlString) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(urlString).openStream()))) {
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);
            return response.toString();
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
            File outFile = new File(objectsDir, subDir + File.separator + hash);

            if (outFile.exists()) continue;

            outFile.getParentFile().mkdirs();
            String url = "https://resources.download.minecraft.net/" + subDir + "/" + hash;

            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("✅ Downloaded: " + entry.getKey());
            } catch (IOException e) {
                System.err.println("❌ Failed to download: " + entry.getKey());
                e.printStackTrace();
            }
        }
    }
    public static List<File> getRequiredClientJars(String vanillaJsonUrl, File forgeJsonFile, File librariesDir) {
        try {
            // JSON-Dateien laden
            JsonObject vanillaJson = Json.readJsonFromUrl(vanillaJsonUrl);
            JsonObject forgeJson = Json.readJsonFile(forgeJsonFile);

            // Libraries extrahieren
            List<String> vanillaLibs = JarHandling.extractLibraryPaths(vanillaJson);
            List<String> forgeLibs = JarHandling.extractLibraryPaths(forgeJson);

            // Zusammenführen
            Set<String> allLibPaths = new LinkedHashSet<>(); // kein Duplikat, aber Reihenfolge behalten
            allLibPaths.addAll(vanillaLibs);
            allLibPaths.addAll(forgeLibs);

            // In echte .jar-Dateien umwandeln
            return JarHandling.resolveJars(new ArrayList<>(allLibPaths), librariesDir);
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList(); // bei Fehler → leere Liste zurückgeben
        }
    }

    public static List<File> findAllJars(File dir) {
        List<File> jarFiles = new ArrayList<>();
        if (!dir.exists() || !dir.isDirectory()) return jarFiles;

        File[] files = dir.listFiles();
        if (files == null) return jarFiles;

        for (File file : files) {
            if (file.isDirectory()) {
                jarFiles.addAll(findAllJars(file)); // Rekursion in Unterordnern
            } else if (file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
                jarFiles.add(file);
            }
        }
        return jarFiles;
    }
    /*public static String getVanillaClientJsonUrl(String versionId) throws IOException {
        String manifestUrl = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

        try (InputStream is = new URL(manifestUrl).openStream();
             InputStreamReader reader = new InputStreamReader(is)) {

            JsonObject manifest = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray versions = manifest.getAsJsonArray("versions");

            for (JsonElement elem : versions) {
                JsonObject versionObj = elem.getAsJsonObject();
                String id = versionObj.get("id").getAsString();

                if (id.equals(versionId)) {
                    return versionObj.get("url").getAsString();
                }
            }
        }

        throw new IOException("Version '" + versionId + "' nicht im Manifest gefunden.");
    } */
    public static String getVanillaClientJsonUrl(String versionId) throws IOException {
        // Lokaler Pfad, unter dem das Manifest gespeichert wird
        Path localManifestPath = Paths.get("version_manifest.json");
        String manifestUrl = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

        // 1. Herunterladen, wenn Datei nicht existiert oder veraltet (optional)
        try (InputStream in = new URL(manifestUrl).openStream()) {
            Files.copy(in, localManifestPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // 2. Datei einlesen
        try (Reader reader = Files.newBufferedReader(localManifestPath)) {
            JsonObject manifest = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray versions = manifest.getAsJsonArray("versions");

            for (JsonElement elem : versions) {
                JsonObject versionObj = elem.getAsJsonObject();
                String id = versionObj.get("id").getAsString();

                if (id.equals(versionId)) {
                    return versionObj.get("url").getAsString();
                }
            }
        }

        throw new IOException("Version '" + versionId + "' nicht im Manifest gefunden.");
    }
}
