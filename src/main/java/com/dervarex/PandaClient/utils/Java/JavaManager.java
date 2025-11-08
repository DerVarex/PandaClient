package com.dervarex.PandaClient.utils.Java;

import com.dervarex.PandaClient.Minecraft.Version.VersionInfo;
import com.dervarex.PandaClient.Minecraft.logger.ClientLogger;
import com.dervarex.PandaClient.utils.OS.OS;
import com.dervarex.PandaClient.utils.OS.OSUtil;
import com.dervarex.PandaClient.utils.file.getPandaClientFolder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/**
 * Manages multiple Java runtimes located under <PandaClientFolder>/java.
 * - Discovers installed JREs (folders with bin/java[.exe])
 * - Can download and install a specific major version from Adoptium
 * - Returns the path to the executable for a selected Java
 */
public class JavaManager {

    private final Path baseDir; // <PandaClient>/java

    public JavaManager() {
        this.baseDir = getPandaClientFolder.getPandaClientFolder().toPath().resolve("java");
        try { Files.createDirectories(baseDir); } catch (IOException ignored) {}
    }

    public Path getBaseDir() { return baseDir; }

    // Scan baseDir for installed Javas
    public List<Java> listInstalled() {
        List<Java> list = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(baseDir)) {
            for (Path p : ds) {
                if (!Files.isDirectory(p)) continue;
                Path home = findJavaHome(p);
                if (home == null) continue;
                try {
                    Java j = new Java(p.getFileName().toString(), home);
                    if (j.isLoaded()) list.add(j);
                } catch (Throwable t) {
                    ClientLogger.log("Ignoring invalid Java at " + p + ": " + t.getMessage(), "WARN", "JavaManager");
                }
            }
        } catch (IOException e) {
            ClientLogger.log("Failed to scan java dir: " + e.getMessage(), "ERROR", "JavaManager");
        }
        return list;
    }

    // Return the java executable path for a given Java
    public Path getExecutable(Java java) {
        if (java == null || java.getPath() == null) return null;
        return OSUtil.getJavaFile(java.getPath().toString(), false);
    }

    // Ensure a Java of given major exists; download if necessary; returns the installed Java
    public Java ensureInstalled(int major, String arch) throws IOException {
        // try find existing
        for (Java j : listInstalled()) {
            if (j.majorVersion == major) return j;
        }
        // otherwise download
        return downloadAndInstall(major, arch);
    }

    /** Returns the current system architecture string (e.g., amd64, aarch64). */
    public static String currentArch() {
        return System.getProperty("os.arch", "amd64");
    }

    /** Ensure a Java of given major for current architecture exists; download if necessary. */
    public Java ensureInstalledForCurrentArch(int major) throws IOException {
        return ensureInstalled(major, currentArch());
    }

    public Java downloadAndInstall(int major, String arch) throws IOException {
        // delete old java of same major i don't know why we need this because theres a check before but maybe this will work
        for (Java existing : listInstalled()) {
            if (existing.majorVersion == major) {
                ClientLogger.log("Removing old Java " + existing.getName(), "INFO", "JavaManager");
                deleteRecursive(existing.getPath());
            }
        }

        Objects.requireNonNull(arch, "arch");
        String osStr = mapOsForAdoptium(OSUtil.getOS());
        String mappedArch = mapArch(arch);

        // Try JRE first, then JDK as fallback
        JSONArray arr = new JSONArray();
        String link = null;
        String releaseName = null;
        String imageTypeUsed = null;
        IOException lastErr = null;
        for (String imageType : new String[]{"jre", "jdk"}) {
            String url = String.format(Locale.ROOT,
                    "https://api.adoptium.net/v3/assets/latest/%d/hotspot?os=%s&image_type=%s&architecture=%s",
                    major, osStr, imageType, mappedArch);
            ClientLogger.log("Query Adoptium: " + url, "INFO", "JavaManager");
            try {
                arr = new JSONArray(httpGet(url));
                if (!arr.isEmpty()) {
                    JSONObject obj = arr.getJSONObject(0);
                    for (int i = 1; i < arr.length(); i++) {
                        JSONObject next = arr.getJSONObject(i);
                        if (next.optInt("version_data", 0) > obj.optInt("version_data", 0)) obj = next;
                    }
                    JSONObject binary = obj.getJSONObject("binary");
                    JSONObject pkg = binary.getJSONObject("package");
                    link = pkg.getString("link");
                    releaseName = obj.getString("release_name") + "-" + imageType;
                    imageTypeUsed = imageType;
                    break;
                }
            } catch (IOException e) {
                lastErr = e;
                ClientLogger.log("Adoptium request failed for image_type=" + imageType + ": " + e.getMessage(), "WARN", "JavaManager");
            }
        }
        if (link == null) {
            if (lastErr != null) throw lastErr;
            throw new IOException("No assets found for Java " + major + " on " + osStr + "/" + mappedArch);
        }

        // Download
        Path tmp = Files.createTempFile("java-" + major + "-", getFileSuffix(link));
        try (InputStream in = new URL(link).openStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        ClientLogger.log("Downloaded " + imageTypeUsed.toUpperCase(Locale.ROOT) + " to " + tmp, "INFO", "JavaManager");

        // Extract into <base>/<releaseName>
        Path target = baseDir.resolve(safeName(releaseName));
        if (Files.exists(target)) deleteRecursive(target);
        Files.createDirectories(target);

        if (link.endsWith(".zip")) extractZip(tmp, target);
        else if (link.endsWith(".tar.gz") || link.endsWith(".tgz")) extractTarGz(tmp, target);
        else throw new IOException("Unsupported archive: " + link);

        // On macOS, adjust to Contents/Home
        Path home = findJavaHome(target);
        if (home == null) throw new IOException("Could not locate bin/java in extracted " + imageTypeUsed.toUpperCase(Locale.ROOT));

        Java j = new Java(releaseName, home);
        Boolean identifyResult = j.identify();
        System.out.println(identifyResult);
        if (!j.isLoaded()) throw new IOException("Installed runtime did not identify correctly at " + home);

        // Persist a name file
        try { Files.writeString(home.resolve("name.txt"), releaseName); } catch (IOException ignored) {}
        ClientLogger.log("Installed Java " + j.getName() + " at " + home, "INFO", "JavaManager");
        return j;
    }

    // Helper: find top-level java home (folder containing bin/java)
    private Path findJavaHome(Path root) {
        // Direct check
        Path exe = root.resolve(OSUtil.getOS() == OS.WINDOWS ? "bin/java.exe" : "bin/java");
        if (Files.exists(exe)) return root;
        // macOS typical layout: <something>.jdk/Contents/Home
        Path mac = root.resolve("Contents").resolve("Home");
        Path macExe = mac.resolve(OSUtil.getOS() == OS.WINDOWS ? "bin/java.exe" : "bin/java");
        if (Files.exists(macExe)) return mac;
        // Scan shallow children to be robust after extraction
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
            for (Path p : ds) {
                if (Files.isDirectory(p)) {
                    Path e = p.resolve(OSUtil.getOS() == OS.WINDOWS ? "bin/java.exe" : "bin/java");
                    if (Files.exists(e)) return p;
                    Path m = p.resolve("Contents").resolve("Home");
                    Path me = m.resolve(OSUtil.getOS() == OS.WINDOWS ? "bin/java.exe" : "bin/java");
                    if (Files.exists(me)) return m;
                }
            }
        } catch (IOException ignored) {}
        return null;
    }

    private static String mapOsForAdoptium(OS os) {
        switch (os) {
            case WINDOWS: return "windows";
            case MAC: return "mac";
            case LINUX: return "linux";
            default: return "linux";
        }
    }

    private static String mapArch(String arch) {
        String a = arch.toLowerCase(Locale.ROOT);
        if (a.equals("x86_64") || a.equals("amd64")) return "x64";
        if (a.equals("x86") || a.equals("i386") || a.equals("i686")) return "x86";
        if (a.equals("aarch64") || a.equals("arm64")) return "aarch64";
        return a; // hope it's already an adoptium value
    }

    private static String httpGet(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(15000);
        con.setReadTimeout(30000);
        try (InputStream in = con.getInputStream()) {
            return new String(in.readAllBytes());
        } finally {
            con.disconnect();
        }
    }

    private static void extractZip(Path zipFile, Path target) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path out = target.resolve(entry.getName()).normalize();
                if (!out.startsWith(target)) throw new IOException("Zip traversal attempt: " + entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    try (OutputStream os = Files.newOutputStream(out)) {
                        zis.transferTo(os);
                    }
                    // if this looks like a binary under bin/ or a shell script, try to make it executable
                    if (isUnix() && looksLikeExecutable(entry.getName())) {
                        trySetExecutable(out);
                    }
                }
            }
        }
    }

    private static void extractTarGz(Path tarGz, Path target) throws IOException {
        try (InputStream fin = Files.newInputStream(tarGz);
             BufferedInputStream bin = new BufferedInputStream(fin);
             GzipCompressorInputStream gzIn = new GzipCompressorInputStream(bin);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn)) {
            TarArchiveEntry entry;
            while ((entry = tarIn.getNextTarEntry()) != null) {
                Path out = target.resolve(entry.getName()).normalize();
                if (!out.startsWith(target)) throw new IOException("Tar traversal attempt: " + entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    try (OutputStream os = Files.newOutputStream(out)) {
                        tarIn.transferTo(os);
                    }
                    if (isUnix() && looksLikeExecutable(entry.getName())) {
                        trySetExecutable(out);
                    }
                }
            }
        }
    }

    private static boolean isUnix() {
        return OSUtil.getOS() != OS.WINDOWS;
    }

    private static boolean looksLikeExecutable(String name) {
        String n = name.replace('\\', '/');
        n = n.startsWith("/") ? n.substring(1) : n;
        // common Java binary locations: bin/java, bin/java.exe, bin/* (shell scripts)
        if (n.startsWith("bin/")) return true;
        if (n.endsWith(".sh")) return true;
        // sometimes nested like */bin/java
        if (n.contains("/bin/")) return true;
        return false;
    }

    private static void trySetExecutable(Path p) {
        try {
            // Prefer POSIX permissions
            try {
                java.util.Set<java.nio.file.attribute.PosixFilePermission> perms = java.nio.file.Files.getPosixFilePermissions(p);
                perms.add(java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE);
                perms.add(java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE);
                perms.add(java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE);
                java.nio.file.Files.setPosixFilePermissions(p, perms);
            } catch (UnsupportedOperationException ex) {
                // fallback
                p.toFile().setExecutable(true, false);
            }
        } catch (Exception e) {
            ClientLogger.log("Failed to set executable on " + p + ": " + e.getMessage(), "WARN", "JavaManager");
        }
    }

    private static void deleteRecursive(Path p) throws IOException {
        if (!Files.exists(p)) return;
        Files.walkFileTree(p, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file); return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir); return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String getFileSuffix(String url) {
        int i = url.lastIndexOf('.');
        return i >= 0 ? url.substring(i) : ".bin";
    }

    private static String safeName(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    /**
     * Choose a reasonable Java command:
     * - prefer the highest installed Java under baseDir
     * - else use the currently running java.home
     * - else fallback to "java" on PATH
     * @return latest Java executable path
     */
    public String getNewestJavaCommand() {
        try {
            List<Java> installed = listInstalled();
            if (!installed.isEmpty()) {
                Java best = installed.stream()
                        .filter(Java::isLoaded)
                        .max((a, b) -> Integer.compare(a.majorVersion, b.majorVersion))
                        .orElse(installed.get(0));
                Path exe = getExecutable(best);
                if (exe != null && Files.exists(exe)) return exe.toAbsolutePath().toString();
            }
        } catch (Throwable ignored) {}
        try {
            Path home = Path.of(System.getProperty("java.home"));
            Path exe = OSUtil.getJavaFile(home.toString(), false);
            if (Files.exists(exe)) return exe.toAbsolutePath().toString();
        } catch (Throwable ignored) {}
        return "java";
    }

    /**
     * Ensure the required Java version for the given Minecraft version is installed,
     * and return its executable path.
     * This method now contains robust fallbacks: if the Mojang API can't be reached,
     * we infer the requirement from the version string (<=1.16 => 8, 1.17 => 16, >=1.18 => 17)
     * and try to use or install that.
     * @return Java executable path
     * @throws Exception if installation or lookup fails
     */
    public String getRequiredJavaVersionCommand(String version) throws Exception {
        int requiredJava;
        try {
            requiredJava = VersionInfo.getRequiredJavaVersion(version); // API
        } catch (Exception e) {
            ClientLogger.log("Failed to get required Java version from API: " + e.getMessage(), "WARN", "JavaManager");
            requiredJava = inferJavaFromMcVersion(version);
            ClientLogger.log("Inferred required Java " + requiredJava + " from version '" + version + "'", "INFO", "JavaManager");
        }

        JavaManager jm = new JavaManager();
        try {
            Java j = jm.ensureInstalledForCurrentArch(requiredJava);
            Path exe = jm.getExecutable(j);
            ClientLogger.log("Using Java " + j.getName() + " for Minecraft " + version + " | executable: " + exe, "INFO", "JavaManager");
            if (exe != null && Files.exists(exe)) return exe.toAbsolutePath().toString();
        } catch (IOException dl) {
            ClientLogger.log("Failed to ensure/install Java " + requiredJava + ": " + dl.getMessage(), "WARN", "JavaManager");
            // try to find an already installed Java of the required major
            for (Java j : jm.listInstalled()) {
                if (j.majorVersion == requiredJava) {
                    Path exe = jm.getExecutable(j);
                    if (exe != null && Files.exists(exe)) {
                        ClientLogger.log("Falling back to already installed Java " + j.getName() + " (" + exe + ")", "INFO", "JavaManager");
                        return exe.toAbsolutePath().toString();
                    }
                }
            }
            // as last resort use newest available (may still be wrong, but better than plain 'java')
            String fallback = jm.getNewestJavaCommand();
            ClientLogger.log("Falling back to newest available Java command: " + fallback, "WARN", "JavaManager");
            return fallback;
        }

        throw new IOException("Could not locate java executable for Java " + requiredJava);
    }

    // Heuristic fallback mapping if API fails
    private static int inferJavaFromMcVersion(String version) {
        if (version == null || version.isBlank()) return 17; // safe default
        String v = version.trim().toLowerCase(Locale.ROOT);
        // strip any labels like "release-" etc.
        v = v.replace("release-", "").replace("minecraft-", "");
        // Extract numeric prefix like 1.16.5
        String num = v.split("-", 2)[0]; // split on '-' if present
        // basic compare on major.minor
        try {
            String[] parts = num.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            if (major > 1) {
                // newer scheme (e.g., 21w) won't land here normally
                return 17;
            }
            if (minor <= 16) return 8;        // 1.16 and below
            if (minor == 17) return 16;       // 1.17 requires Java 16
            return 17;                        // 1.18+
        } catch (Exception ignored) {
            return 17;
        }
    }

    public static void main(String[] args) {
        JavaManager jm = new JavaManager();
        try {
            Java j = jm.ensureInstalledForCurrentArch(VersionInfo.getRequiredJavaVersion("1.16.1"));
            Path exe = jm.getExecutable(j);
            System.out.println("Installed Java " + j.getName() + " at " + exe);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
