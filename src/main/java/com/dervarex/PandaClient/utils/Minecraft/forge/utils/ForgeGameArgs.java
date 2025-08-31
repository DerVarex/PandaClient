package com.dervarex.PandaClient.utils.Minecraft.forge.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ForgeGameArgs {
    public static List<String> gameArgs(JsonObject forgeJson, File LIB_DIR, String version) {
        List<String> command = new ArrayList<>();
        if (forgeJson.has("arguments")) {
            JsonObject arguments = forgeJson.getAsJsonObject("arguments");
            if (arguments.has("jvm")) {
                JsonArray jvmArgs = arguments.getAsJsonArray("jvm");

                String libraryDirectory = LIB_DIR.getAbsolutePath();
                String classpathSeparator = System.getProperty("os.name").toLowerCase().contains("win") ? ";" : ":";

                for (JsonElement jvmElement : jvmArgs) {
                    String arg = jvmElement.getAsString();

                    // Platzhalter ersetzen
                    arg = arg.replace("${version_name}",  version)
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
        }
        return command;
    }
}
