package com.dervarex.PandaClient;

import com.dervarex.PandaClient.GUI.ModServer;
import com.dervarex.PandaClient.GUI.WebSocket.NotificationServer.NotificationServerStart;
import com.dervarex.PandaClient.Minecraft.logger.ClientLogger;
import com.dervarex.PandaClient.utils.file.getPandaClientFolder;
import fi.iki.elonen.NanoHTTPD;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class MainGUI {
    private static final int DEFAULT_PORT = 8800;
    private static final int MAX_PORT_ATTEMPTS = 10;

    // Starts the GUI and Initialises processes :)
    public MainGUI() {
        File electronDir = resolveElectronDir();
        int actualPort = startServer();
        writePortFile(electronDir, actualPort);
        startElectron(electronDir, actualPort);
        NotificationServerStart.startNotificationServer();
        ClientLogger.log("Notification server started", "INFO", "MainGUI");
    }

    private File resolveElectronDir() {
        // Prefer alongside the jar, then parent of jar (for target/* builds), then CWD/electron
        try {
            File codeSource = new File(MainGUI.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File baseDir = codeSource.isFile() ? codeSource.getParentFile() : codeSource;
            File sibling = new File(baseDir, "electron");
            if (isValidElectronDir(sibling)) return sibling.getAbsoluteFile();
            File parent = baseDir.getParentFile();
            if (parent != null) {
                File parentSibling = new File(parent, "electron");
                if (isValidElectronDir(parentSibling)) return parentSibling.getAbsoluteFile();
            }
        } catch (Exception ignored) {}
        File cwd = new File("electron");
        if (isValidElectronDir(cwd)) return cwd.getAbsoluteFile();
        ClientLogger.log("Electron directory not found; expected at " + cwd.getAbsolutePath(), "ERROR", "MainGUI");
        return cwd.getAbsoluteFile();
    }

    private boolean isValidElectronDir(File dir) {
        if (dir == null || !dir.exists()) return false;
        File pkg = new File(dir, "package.json");
        return pkg.exists();
    }

    private int startServer() {
        int port = DEFAULT_PORT;
        for (int attempt = 0; attempt < MAX_PORT_ATTEMPTS; attempt++) {
            try {
                ModServer server = new ModServer(port);
                server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
                ClientLogger.log("Server running on http://localhost:" + port, "INFO", "MainGUI");
                return port;
            } catch (IOException e) {
                ClientLogger.log("Port " + port + " in use, trying next port", "WARN", "MainGUI");
                port++;
            }
        }
        ClientLogger.log("Could not find available port after " + MAX_PORT_ATTEMPTS + " attempts", "ERROR", "MainGUI");
        throw new RuntimeException("No available port found");
    }

    private void writePortFile(File electronDir, int port) {
        try {
            File portFile = new File(electronDir, ".backend-port");
            File parent = portFile.getAbsoluteFile().getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (FileWriter writer = new FileWriter(portFile)) {
                writer.write(String.valueOf(port));
            }
            ClientLogger.log("Written backend port " + port + " to " + portFile.getAbsolutePath(), "INFO", "MainGUI");
        } catch (IOException e) {
            ClientLogger.log("Failed to write port file: " + e.getMessage(), "WARN", "MainGUI");
        }
    }

    private void startElectron(File electronDir, int port) {
        try {
            if (!electronDir.exists()) {
                ClientLogger.log("Electron directory not found at " + electronDir.getAbsolutePath(), "ERROR", "MainGUI");
                return;
            }

            String npmPath = ensureNpmAvailable(electronDir);
            if (npmPath == null) {
                ClientLogger.log("Could not find or install npm automatically. Please install Node.js manually.", "ERROR", "MainGUI");
                return;
            }

            File nodeModules = new File(electronDir, "node_modules");
            if (!nodeModules.exists()) {
                ClientLogger.log("Installing npm dependencies...", "INFO", "MainGUI");
                installNpmDependencies(npmPath, electronDir);
            }

            ensureStartScript(electronDir);

            ProcessBuilder pb = new ProcessBuilder(npmPath, "start");
            pb.directory(electronDir);
            pb.environment().put("BACKEND_PORT", String.valueOf(port));
            // ensure local node bin is on PATH when using embedded runtime
            File binDir = new File(npmPath).getParentFile();
            if (binDir != null && binDir.exists()) {
                String path = pb.environment().getOrDefault("PATH", "");
                pb.environment().put("PATH", binDir.getAbsolutePath() + File.pathSeparator + path);
            }
            pb.inheritIO();
            pb.start();
            ClientLogger.log("Electron started with BACKEND_PORT=" + port, "INFO", "MainGUI");
        } catch (Exception e) {
            ClientLogger.log("Electron start failed: " + e.getMessage(), "ERROR", "MainGUI");
        }
    }

    private void ensureStartScript(File electronDir) {
        try {
            File packageJson = new File(electronDir, "package.json");
            if (!packageJson.exists()) {
                ClientLogger.log("package.json not found, cannot ensure start script", "WARN", "MainGUI");
                return;
            }

            String content = new String(Files.readAllBytes(packageJson.toPath()), StandardCharsets.UTF_8);
            if (content.contains("\"start\":")) {
                ClientLogger.log("Start script already present in package.json", "INFO", "MainGUI");
                return;
            }

            ClientLogger.log("Adding start script to package.json", "INFO", "MainGUI");
            content = content.replaceFirst("\\{", "{\n  \"scripts\": {\n    \"start\": \".embedded-bin/launch-editor.sh\"\n  },");
            Files.write(packageJson.toPath(), content.getBytes(StandardCharsets.UTF_8));
            ClientLogger.log("Start script added to package.json", "INFO", "MainGUI");
        } catch (Exception e) {
            ClientLogger.log("Failed to ensure start script in package.json: " + e.getMessage(), "ERROR", "MainGUI");
        }
    }

    private String ensureNpmAvailable(File electronDir) {
        String npmCommand = System.getProperty("os.name").toLowerCase().contains("win") ? "npm.cmd" : "npm";
        String systemNpm = findNpm(npmCommand);
        if (systemNpm != null) {
            ClientLogger.log("NPM found: " + systemNpm, "INFO", "MainGUI");
            return systemNpm;
        }

        // Download a portable Node.js runtime (linux-x64) into the PandaClient folder
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch");
        if (!(os.contains("linux") && ("amd64".equalsIgnoreCase(arch) || "x86_64".equalsIgnoreCase(arch)))) {
            ClientLogger.log("Automatic Node.js download only implemented for Linux x64. Please install Node.js manually.", "WARN", "MainGUI");
            return null;
        }

        File installDir = new File(getPandaClientFolder.getPandaClientFolder(), "embedded-node");
        File npmBin = new File(installDir, "bin/npm");
        if (npmBin.exists()) return npmBin.getAbsolutePath();

        try {
            String version = "v20.11.1";
            String fileName = "node-" + version + "-linux-x64.tar.xz";
            String url = "https://nodejs.org/dist/" + version + "/" + fileName;
            File downloadTarget = new File(installDir, fileName);
            if (!installDir.exists()) installDir.mkdirs();

            ClientLogger.log("Downloading Node.js " + version + "...", "INFO", "MainGUI");
            java.nio.file.Files.copy(new java.net.URL(url).openStream(), downloadTarget.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            ClientLogger.log("Download complete. Extracting...", "INFO", "MainGUI");

            ProcessBuilder pb = new ProcessBuilder("tar", "-xf", downloadTarget.getAbsolutePath(), "--strip-components=1", "-C", installDir.getAbsolutePath());
            pb.inheritIO();
            Process p = pb.start();
            int exit = p.waitFor();
            if (exit != 0) {
                ClientLogger.log("Extraction failed with exit code " + exit, "ERROR", "MainGUI");
                return null;
            }
            ClientLogger.log("Node.js unpacked to " + installDir.getAbsolutePath(), "INFO", "MainGUI");
            return npmBin.getAbsolutePath();
        } catch (Exception e) {
            ClientLogger.log("Failed to download/extract Node.js: " + e.getMessage(), "ERROR", "MainGUI");
            return null;
        }
    }

    private String findNpm(String npmCommand) {
        try {
            ProcessBuilder pb = new ProcessBuilder(npmCommand, "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String version = reader.readLine();
                int exitCode = process.waitFor();
                if (exitCode == 0 && version != null && !version.isEmpty()) {
                    return npmCommand;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void installNpmDependencies(String npmPath, File electronDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(npmPath, "install");
            pb.directory(electronDir);
            File binDir = new File(npmPath).getParentFile();
            if (binDir != null && binDir.exists()) {
                String path = pb.environment().getOrDefault("PATH", "");
                pb.environment().put("PATH", binDir.getAbsolutePath() + File.pathSeparator + path);
            }
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                ClientLogger.log("NPM dependencies installed successfully", "INFO", "MainGUI");
            } else {
                ClientLogger.log("NPM install failed with exit code: " + exitCode, "WARN", "MainGUI");
            }
        } catch (Exception e) {
            ClientLogger.log("Failed to install NPM dependencies: " + e.getMessage(), "WARN", "MainGUI");
        }
    }
}
