package com.dervarex.PandaClient.utils.Minecraft.forge.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.dervarex.PandaClient.utils.WebUtils.downloadFromURL;
import com.google.gson.JsonParser;

public class VanillaGameArgs {
    public static List<String> gameArgs(String VanillaJsonURL, File LIB_DIR, String version) {
        String VanillaJsonPath = System.getenv("APPDATA") + File.separator + "PandaClient" + File.separator + "installer" + File.separator + "vanilla" + File.separator + "json" + File.separator + VanillaJsonURL.substring(VanillaJsonURL.lastIndexOf('/') + 1);
        File VanillaJsonPathFile = new File(VanillaJsonPath);

        // Stelle sicher, dass der Ordner existiert
        File parentDir = VanillaJsonPathFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Lade die Datei herunter, wenn sie noch nicht existiert
        if (!VanillaJsonPathFile.exists()) {
            System.out.println("📥 JSON nicht vorhanden, wird geladen von: " + VanillaJsonURL);
            downloadFromURL.download(VanillaJsonURL, VanillaJsonPath);

            // Prüfen, ob sie wirklich da ist (zur Sicherheit)
            if (!VanillaJsonPathFile.exists()) {
                throw new RuntimeException("❌ Download fehlgeschlagen oder Datei existiert nicht: " + VanillaJsonPath);
            }
        }

        // Jetzt die JSON-Datei lesen und parsen
        JsonObject VanillaJson;
        try (FileReader reader = new FileReader(VanillaJsonPath)) {
            VanillaJson = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new RuntimeException("❌ Fehler beim Lesen der JSON-Datei: " + VanillaJsonPath, e);
        }

        // Argumente extrahieren
        List<String> command = new ArrayList<>();
        if (VanillaJson.has("arguments")) {
            JsonObject arguments = VanillaJson.getAsJsonObject("arguments");

            // JVM-Argumente
            if (arguments.has("jvm")) {
                JsonArray jvmArgs = arguments.getAsJsonArray("jvm");

                String libraryDirectory = LIB_DIR.getAbsolutePath();
                String classpathSeparator = System.getProperty("os.name").toLowerCase().contains("win") ? ";" : ":";

                for (JsonElement jvmElement : jvmArgs) {
                    if (jvmElement.isJsonPrimitive()) {
                        // Einfacher String
                        String arg = jvmElement.getAsString()
                                .replace("${version_name}", version)
                                .replace("${library_directory}", libraryDirectory)
                                .replace("${classpath_separator}", classpathSeparator);

                        command.add(arg);
                    } else if (jvmElement.isJsonObject()) {
                        JsonObject jvmObj = jvmElement.getAsJsonObject();

                        boolean allow = false;  // Default: nicht erlauben

                        if (jvmObj.has("rules")) {
                            for (JsonElement ruleElement : jvmObj.getAsJsonArray("rules")) {
                                JsonObject rule = ruleElement.getAsJsonObject();

                                String action = rule.has("action") ? rule.get("action").getAsString() : "allow";

                                if ("allow".equals(action)) {
                                    allow = true;
                                } else if ("disallow".equals(action)) {
                                    allow = false;
                                    break;
                                }
                            }
                        } else {
                            allow = true;  // Wenn keine Regeln, dann erlauben
                        }

                        if (allow && jvmObj.has("value")) {
                            String arg = jvmObj.get("value").getAsString()
                                    .replace("${version_name}", version)
                                    .replace("${library_directory}", libraryDirectory)
                                    .replace("${classpath_separator}", classpathSeparator);

                            command.add(arg);
                        }
                    }
                }

            } else {
                System.err.println("⚠️ Keine 'jvm'-Argumente in Vanilla JSON gefunden.");
            }

            // Game-Argumente
            if (arguments.has("game")) {
                JsonArray gameArgs = arguments.getAsJsonArray("game");
                for (JsonElement argElement : gameArgs) {
                    if (argElement.isJsonPrimitive()) {
                        // Einfacher String-Argument
                        command.add(argElement.getAsString());
                    } else if (argElement.isJsonObject()) {
                        JsonObject argObj = argElement.getAsJsonObject();

                        // Check, ob rules vorhanden sind
                        if (argObj.has("rules")) {
                            boolean allow = false; // Default: nicht erlauben

                            for (JsonElement ruleElement : argObj.getAsJsonArray("rules")) {
                                JsonObject rule = ruleElement.getAsJsonObject();

                                String action = rule.has("action") ? rule.get("action").getAsString() : "allow";
                                // Die "os" kann optional sein, wenn du OS-spezifisch filtern willst
                                // Hier einfach ignorieren oder implementieren, wenn du willst

                                // Beispiel: Erlaube, wenn action == "allow"
                                if ("allow".equals(action)) {
                                    allow = true;
                                } else if ("disallow".equals(action)) {
                                    allow = false;
                                    break;
                                }
                            }

                            if (allow && argObj.has("value")) {
                                JsonElement valueElement = argObj.get("value");

                                if (valueElement.isJsonPrimitive()) {
                                    command.add(valueElement.getAsString());
                                } else if (valueElement.isJsonArray()) {
                                    JsonArray arr = valueElement.getAsJsonArray();
                                    for (JsonElement el : arr) {
                                        if (el.isJsonPrimitive()) {
                                            command.add(el.getAsString());
                                        } else {
                                            System.err.println("Unerwarteter Wert im Array: " + el);
                                        }
                                    }
                                } else {
                                    System.err.println("Unerwarteter Typ bei 'value': " + valueElement);
                                }
                            }

                        } else if (argObj.has("value")) {
                            JsonElement valueElement = argObj.get("value");

                            if (valueElement.isJsonPrimitive()) {
                                command.add(valueElement.getAsString());
                            } else if (valueElement.isJsonArray()) {
                                JsonArray arr = valueElement.getAsJsonArray();
                                for (JsonElement el : arr) {
                                    if (el.isJsonPrimitive()) {
                                        command.add(el.getAsString());
                                    } else {
                                        System.err.println("Unerwarteter Wert im Array: " + el);
                                    }
                                }
                            } else {
                                System.err.println("Unerwarteter Typ bei 'value': " + valueElement);
                            }
                        }
                    }
                }
            } else {
                System.err.println("⚠️ Keine 'game'-Argumente in Vanilla JSON gefunden.");
            }
        } else {
            System.err.println("⚠️ Keine 'arguments'-Sektion in Vanilla JSON gefunden.");
        }

        return command;
    }
}