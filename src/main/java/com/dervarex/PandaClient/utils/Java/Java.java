package com.dervarex.PandaClient.utils.Java;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import com.dervarex.PandaClient.Minecraft.logger.ClientLogger;
import com.dervarex.PandaClient.utils.OS.OSUtil;

public class Java {

    public static class JavaFactory {

        public static Java fromJson(Object json) {
            if (json == null)
                return null;

            String path;
            String name = null;

            if (json instanceof JSONObject obj) {
                if (!obj.has("path"))
                    return null;

                path = obj.getString("path");

                if (obj.has("name"))
                    name = obj.optString("name", null);
            } else if (json instanceof String s) {
                path = s;
            } else {
                return null;
            }

            return new Java(name, Path.of(path));
        }

        public static JSONObject toJson(Java java) {
            JSONObject obj = new JSONObject();

            if (java.getName() != null)
                obj.put("name", java.getName());

            if (java.getPath() != null)
                obj.put("path", java.getPath().toString());
            else
                obj.put("path", String.valueOf(java.majorVersion));

            return obj;
        }
    }

    private String codeName;
    public int majorVersion;
    public int arch;
    public String version;
    private String name;
    private Path path;
    private boolean loaded;

    public Java() {
        path = null;
        loaded = false;
    }

    public Java(int majorVersion) {
        this.majorVersion = majorVersion;
        path = null;
        loaded = false;
    }

    public Java(String name) {
        this.name = name;
        path = null;
        loaded = false;
    }

    public Java(String name, Path path) {
        this.name = name;
        this.path = path;
        identify();
    }

    public Java(Path path) {
        this.path = path;
        identify();
    }

    /**
     * Identifies the Java version and architecture from the path.
     * @return true if successful, false otherwise.
     */
    public boolean identify() {
        if (path == null) {
            ClientLogger.log("Java path is null", "WARN", "Java");
            return loaded = false;
        }

        var nameFile = path.resolve("name.txt");

        if (nameFile.toFile().exists()) {
            try {
                name = java.nio.file.Files.readString(nameFile);
            } catch (IOException e) {
                ClientLogger.log("Could not read name.txt: " + e.getMessage(), "DEBUG", "Java");
                // ignore read error
            }
        }

        try {
            Path exe = getExecutable();
            ClientLogger.log("Java.identify diagnostics: exe=" + exe + ", exists=" + (exe != null && java.nio.file.Files.exists(exe)) + ", isFile=" + (exe != null && java.nio.file.Files.isRegularFile(exe)) + ", isExecutable=" + (exe != null && java.nio.file.Files.isExecutable(exe)), "DEBUG", "Java");

            if (exe == null || !java.nio.file.Files.exists(exe)) {
                ClientLogger.log("Java executable not found for path=" + path + " (exe=" + exe + ")", "ERROR", "Java");
                return loaded = false;
            }

            // If on Unix and not executable, try to set executable bit (fixes permission issues after extraction)
            if (OSUtil.getOS() != com.dervarex.PandaClient.utils.OS.OS.WINDOWS && !java.nio.file.Files.isExecutable(exe)) {
                try {
                    // try POSIX first
                    try {
                        var perms = java.nio.file.Files.getPosixFilePermissions(exe);
                        perms.add(java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE);
                        perms.add(java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE);
                        perms.add(java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE);
                        java.nio.file.Files.setPosixFilePermissions(exe, perms);
                        ClientLogger.log("Set POSIX exec perms on " + exe, "DEBUG", "Java");
                    } catch (UnsupportedOperationException ux) {
                        boolean ok = exe.toFile().setExecutable(true, false);
                        ClientLogger.log("Tried File.setExecutable on " + exe + " -> " + ok, "DEBUG", "Java");
                    }
                } catch (Exception pe) {
                    ClientLogger.log("Could not set executable bit on " + exe + ": " + pe.getMessage(), "WARN", "Java");
                }
            }

            ProcessBuilder pb = new ProcessBuilder(exe.toString(), "-version");
            // java -version typically writes to stderr; we'll read both to be safe
            Process process = pb.start();

            java.util.List<String> lines = new java.util.ArrayList<>();
            try (java.io.BufferedReader err = new java.io.BufferedReader(new java.io.InputStreamReader(process.getErrorStream()));
                 java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {

                String l;
                while ((l = err.readLine()) != null) {
                    lines.add(l);
                }
                // if nothing on stderr, try stdout
                if (lines.isEmpty()) {
                    while ((l = in.readLine()) != null) lines.add(l);
                }
            } catch (IOException ioe) {
                ClientLogger.log("Error reading java -version output: " + ioe.getMessage(), "ERROR", "Java");
                return loaded = false;
            }

            if (lines.isEmpty()) {
                ClientLogger.log("No output from java -version at " + exe, "ERROR", "Java");
                return loaded = false;
            }

            String versionLine = lines.get(0);
            String archLine = lines.size() > 2 ? lines.get(2) : (lines.size() > 1 ? lines.get(1) : null);

            // extract version quoted string if present
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"([^\"]+)\"").matcher(versionLine);
            if (m.find()) this.version = m.group(1);
            else {
                String[] a = versionLine.split(" ");
                if (a.length >= 3) this.version = a[2].replace("\"", "");
                else this.version = versionLine;
            }

            String[] parts = this.version.split("\\.");
            try {
                majorVersion = Integer.parseInt(parts[0].equals("1") ? parts[1] : parts[0]);
            } catch (Exception ex) {
                ClientLogger.log("Failed to parse java version from '" + this.version + "' (line='" + versionLine + "')", "ERROR", "Java");
                return loaded = false;
            }

            this.arch = archLine != null && (archLine.contains("64") || archLine.toLowerCase().contains("64-bit")) ? 64 : 32;

            return loaded = true;
        } catch (IOException e) {
            ClientLogger.log("IO error while executing java -version for path=" + path + ": " + e.getMessage(), "ERROR", "Java");
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            ClientLogger.log(sw.toString(), "ERROR", "Java");
            return loaded = false;
        }
    }

    /**
     * Returns the Minecraft code name of this Java version.
     */
    public String getCodeName() {
        if (codeName != null)
            return codeName;

        if (majorVersion == 17)
            codeName = "java-runtime-alpha";
        else if (majorVersion == 16)
            codeName = "java-runtime-gamma";
        else
            codeName = "jre-legacy";

        return codeName;
    }

    public static Java fromVersion(int majorVersion) {
        return new Java(majorVersion);
    }

    public static Java fromCodeName(String codeName) {
        if (codeName.contains("alpha") || codeName.contains("beta"))
            return new Java(17);
        else if (codeName.equals("gamma"))
            return new Java(16);
        else
            return new Java(8);
    }

    public boolean isLoaded() {
        return loaded;
    }

    public Java setPath(Path path) {
        this.path = path;
        return this;
    }

    public Path getPath() {
        return path;
    }

    public boolean isInitial() {
        return name == null;
    }

    public String getName() {
        return name == null ? "Java " + majorVersion : name;
    }

    public Java setName(String name) {
        this.name = name;
        if (path != null) {
            try {
                java.nio.file.Files.writeString(path.resolve("name.txt"), name);
            } catch (IOException e) {
                ClientLogger.log( "Could not write name.txt for Java: " + name, "WARN", "Java");
            }
        }
        return this;
    }

    public boolean isEmpty() {
        return path == null;
    }

    public String toIdentifier() {
        return getName() + " - " + majorVersion;
    }

    public Path getWindowExecutable() {
        return OSUtil.getJavaFile(path.toString(), true);
    }

    public Path getExecutable() {
        return OSUtil.getJavaFile(path.toString(), false);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Java j && path != null && path.equals(j.path);
    }
}
