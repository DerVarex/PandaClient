package com.dervarex.PandaClient.server;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final String name;
    private final String version;
    private final List<String> consoleOutput = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean running = false;
    private Process serverProcess;

    private final List<ConsoleListener> listeners = new ArrayList<>();
    private File serverDir;
    private File jarFile;
    private String[] args = new String[0];
    private BufferedWriter commandWriter;

    public interface ConsoleListener {
        void onNewLine(String line);
    }

    public Server(String name, String version, File serverDir, File jarFile, String... args) {
        this.name = name;
        this.version = version;
        this.serverDir = serverDir;
        this.jarFile = jarFile;
        this.args = args;

        if (!serverDir.exists()) serverDir.mkdirs();
    }

    public boolean isRunning() {
        return running;
    }

    public void addConsoleListener(ConsoleListener listener) {
        listeners.add(listener);
    }

    private void addConsole(String line) {
        consoleOutput.add(line);
        System.out.println("[" + name + "] " + line);
        for (ConsoleListener l : listeners) {
            l.onNewLine(line);
        }
    }

    public List<String> getConsoleOutput() {
        return new ArrayList<>(consoleOutput);
    }

    public String getName() {
        return name;
    }

    public File getJarFile() {
        return jarFile;
    }

    public String getVersion() {
        return version;
    }

    public File getServerDir() {
        return serverDir;
    }

    public void clearConsole() {
        consoleOutput.clear();
    }

    // ---- SERVER START/STOP ----

    public void start() {
        if (running) {
            addConsole("Server is already running.");
            return;
        }

        if (!jarFile.exists()) {
            addConsole("Server JAR not found: " + jarFile.getAbsolutePath());
            return;
        }

        running = true;
        addConsole("Starting server " + name + "...");

        executor.submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(buildCommand());
                pb.directory(serverDir);
                pb.redirectErrorStream(true);
                serverProcess = pb.start();

                // writer for stdin commands
                commandWriter = new BufferedWriter(new OutputStreamWriter(serverProcess.getOutputStream()));

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null && running) {
                        addConsole(line);
                    }
                }

                serverProcess.waitFor();
            } catch (IOException | InterruptedException e) {
                addConsole("Error: " + e.getMessage());
            } finally {
                running = false;
                addConsole("Server stopped.");
            }
        });
    }

    public synchronized void sendCommand(String command) {
        if (!running || commandWriter == null) {
            addConsole("Cannot send command, server not running.");
            return;
        }
        try {
            commandWriter.write(command);
            commandWriter.newLine();
            commandWriter.flush();
        } catch (IOException e) {
            addConsole("Failed to send command: " + e.getMessage());
        }
    }

    public void stop() {
        if (!running) {
            addConsole("Server is not running.");
            return;
        }
        addConsole("Stopping server...");
        running = false;
        if (serverProcess != null) serverProcess.destroy();
    }

    private List<String> buildCommand() {
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-Xmx2G");
        command.add("-Xms512M");
//        command.add("-Dcom.mojang.eula.agree=true");
        command.add("-jar");
        command.add(jarFile.getAbsolutePath());
        command.add("--nogui");
        command.addAll(List.of(args));
        return command;
    }

    public void shutdown() {
        stop();
        executor.shutdownNow();
    }
}
