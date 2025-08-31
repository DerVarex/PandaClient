package com.dervarex.PandaClient.utils.Minecraft.forge.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JarHandling {
    public static List<String> extractLibraryPaths(JsonObject json) {
        List<String> libraries = new ArrayList<>();

        JsonArray libs = json.getAsJsonArray("libraries");
        for (JsonElement element : libs) {
            JsonObject lib = element.getAsJsonObject();
            if (lib.has("downloads")) {
                JsonObject downloads = lib.getAsJsonObject("downloads");
                if (downloads.has("artifact")) {
                    JsonObject artifact = downloads.getAsJsonObject("artifact");
                    String path = artifact.get("path").getAsString(); // z.B. "org/lwjgl/lwjgl/3.3.1/lwjgl-3.3.1.jar"
                    libraries.add(path);
                }
            }
        }

        return libraries;
    }

    public static List<File> resolveJars(List<String> libPaths, File librariesDir) {
        List<File> jars = new ArrayList<>();
        for (String path : libPaths) {
            File jarFile = new File(librariesDir, path);
            if (jarFile.exists()) {
                jars.add(jarFile);
            } else {
                System.err.println("Fehlende JAR: " + jarFile.getAbsolutePath());
                String jarFileName = jarFile.getName();
                if (jarFileName.endsWith(".jar")) {

                }
            }
        }
        return jars;
    }
}
