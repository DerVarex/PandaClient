package com.dervarex.PandaClient.server;

import com.dervarex.PandaClient.Main;
import com.dervarex.PandaClient.server.ui.MainDashboard;
import com.dervarex.PandaClient.utils.NetUtils.NetUtils;
import com.dervarex.PandaClient.utils.OS.OSUtil;
import com.dervarex.PandaClient.utils.file.getAppdata;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ServerConfig {

    private final Gson gson = new Gson();

    private final List<MainDashboard> dashboards = new ArrayList<>();

    public void saveConfig(Server server, File file) {
        JsonObject json = new JsonObject();
        json.addProperty("name", server.getName());
        json.addProperty("version", server.getVersion());
        json.addProperty("serverDir", server.getServerDir().getAbsolutePath());
        json.addProperty("jarFile", server.getJarFile().getAbsolutePath());

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Server loadConfig(File file) {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            String name = json.get("name").getAsString();
            String version = json.get("version").getAsString();
            File serverDir = new File(json.get("serverDir").getAsString());
            File jarFile = new File(json.get("jarFile").getAsString());
            return new Server(name, version, serverDir, jarFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Server createServer(String name, String version, ServerType type, File serverdir) throws IOException {
        serverdir.mkdirs();

        // lade runter und erhalte die echte Jar-Datei (mit originalem Namen)
        File jarFile = NetUtils.downloadToDirectoryPreserveName(type.getDownloadUrl(version), serverdir);

        Server server = new Server(name, version, serverdir, jarFile);

        Main.getServerConfig().saveConfig(server, ServerManager.getConfigFile(name));
        ServerManager.SERVERS.put(name, server);

        MainDashboard ui = new MainDashboard(name, version);
        ui.attachServer(server);
        server.addConsoleListener(ui::addConsoleLog);
        ui.setVisible(true);

        startServer(server);
        return server;
    }




    public void startServer(Server server) throws IOException {
        File jarFile = server.getJarFile();

        if(!jarFile.exists()) {
            throw new IOException("Server jar does not exist: " + jarFile.getAbsolutePath());
        }

        File eula = new File(server.getServerDir(), "eula.txt");
        try (FileWriter w = new FileWriter(eula)) {
            w.write("eula=true\n");       // accept eula automatically, because the user already agreed
        }


        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-Xmx2G");
        command.add("-Xms512M");
//        command.add("-Dcom.mojang.eula.agree=true");
        command.add("-jar");
        command.add(jarFile.getAbsolutePath());
        command.add("--nogui");

        System.out.println("Starting server: " + String.join(" ", command));

        Process process = new ProcessBuilder(command)
                .directory(server.getServerDir())
                .start();

        // stdout
        new Thread(() -> {
            try (var r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    System.out.println("[SERVER] " + line);
                }
            } catch (Exception ignored) {}
        }).start();

        // stderr
        new Thread(() -> {
            try (var r = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    System.err.println("[SERVER-ERR] " + line);
                }
            } catch (Exception ignored) {}
        }).start();
    }



    public void sendConsoleLog(MainDashboard dashboard) {

    }

    public static void main(String[] args) throws Exception {
        File dir = new File("/home/dervarex/.config/Development/PandaClientServers/hollyserver");
        File jar = NetUtils.downloadToDirectoryPreserveName("https://api.papermc.io/v2/projects/paper/versions/1.21.8/builds/60/downloads/paper-1.21.8-60.jar", dir);
        System.out.println("Result file: " + jar.getAbsolutePath());
    }

}
