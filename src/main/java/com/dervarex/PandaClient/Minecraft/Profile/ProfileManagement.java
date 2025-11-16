package com.dervarex.PandaClient.Minecraft.Profile;

import com.dervarex.PandaClient.Auth.AuthManager;
import com.dervarex.PandaClient.Minecraft.MinecraftLauncher;
import com.dervarex.PandaClient.Minecraft.loader.LoaderType;
import com.dervarex.PandaClient.utils.file.getPandaClientFolder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.dervarex.PandaClient.Minecraft.logger.ClientLogger;

public class ProfileManagement {
    // profileImagePath can be null - if it's null, the standard image will be used
    public void createProfile(String profileName, String versionId, LoaderType loader){
        if(profileName == null || profileName.isEmpty()) {
            throw new IllegalArgumentException("Profile name darf nicht null oder leer sein!");
        }

        JSONObject profile = new JSONObject();
        profile.put("profileName", profileName);
        profile.put("versionId", versionId);
        profile.put("loader", loader.toString());

        // Profilordner erstellen

        File profileFolder = new File(
                new File(getPandaClientFolder.getPandaClientFolder(), "instances"),
                profileName
        );
        if (!profileFolder.exists()) profileFolder.mkdirs();

        // JSON in dem Ordner speichern
        File jsonFile = new File(profileFolder, "profile.json");
        jsonFile.getParentFile().mkdirs();
        try (FileWriter file = new FileWriter(jsonFile)) {
            file.write(profile.toString(4));
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            ClientLogger.log("Failed to write profile.json: " + sw.toString(), "ERROR", "ProfileManagement");
        }

        AuthManager.User User = AuthManager.getUser();
        /*if(loader == LoaderType.FORGE) {
            installForge = true;
        } */
    }
    public Profile loadProfile(File profileFile){
        try {
            String content = Files.readString(profileFile.toPath());
            JSONObject obj = new JSONObject(content);

            String name = obj.getString("profileName");
            String versionId = obj.getString("versionId");
            LoaderType loader = LoaderType.fromString(obj.optString("loader")); // safer

            // Profile Object bauen
            Profile.ProfileFactory factory = new Profile.ProfileFactory(name, versionId, loader);

            return factory.build(); // fertiges Profil zurückgeben

        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            ClientLogger.log("Failed to load profile: " + sw.toString(), "ERROR", "ProfileManagement");
            return null; // falls File nicht gelesen werden kann
        }
    }
    public List<Profile> getProfiles() {
        List<Profile> profiles = new ArrayList<>();
        File profilesDir = new File(getPandaClientFolder.getPandaClientFolder(), "instances");

        // Sicherheit: existiert der Ordner überhaupt?
        if (!profilesDir.exists() || !profilesDir.isDirectory()) {
            return profiles; // leere Liste zurückgeben
        }

        for (File profileFolder : profilesDir.listFiles(File::isDirectory)) {
            File profileFile = new File(profileFolder, "profile.json");

            if (profileFile.exists()) {
                Profile p = loadProfile(profileFile);
                if (p != null) {
                    profiles.add(p);
                }
            } else {
                ClientLogger.log("Profile folder exists but no profile.json: " + profileFolder.getName(), "WARN", "ProfileManagement");
            }
        }

        return profiles;
    }
    public String getProfilesAsJson() {
        List<Profile> profiles = getProfiles();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(profiles);
    }
}
