package com.dervarex.PandaClient.utils.Minecraft.forge.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.URL;

public class Json {
    public static JsonObject readJsonFile(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    public static JsonObject readJsonFromUrl(String urlStr) throws IOException {
        try (InputStream is = new URL(urlStr).openStream();
             InputStreamReader reader = new InputStreamReader(is)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

}
