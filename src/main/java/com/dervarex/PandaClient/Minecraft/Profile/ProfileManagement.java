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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ProfileManagement {
    // profileImagePath can be null - if it's null, the standard image will be used
    public void createProfile(String profileName, String versionId, LoaderType loader, File profileImage){
        if(profileName == null || profileName.isEmpty()) {
            throw new IllegalArgumentException("Profile name darf nicht null oder leer sein!");
        }

        String imagePath = (profileImage != null) ? profileImage.getAbsolutePath() : "images/default.png";

        JSONObject profile = new JSONObject();
        profile.put("profileName", profileName);
        profile.put("versionId", versionId);
        profile.put("loader", loader.toString());
        profile.put("profileImagePath", imagePath);

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
            e.printStackTrace();
        }

        AuthManager.User User = AuthManager.getUser();
        Boolean installForge = false;
        /*if(loader == LoaderType.FORGE) {
            installForge = true;
        } */

        MinecraftLauncher.LaunchMinecraft(versionId, User.getUsername(), User.getUuid(), User.getAccessToken(), profileFolder, false, installForge);
    }
    public Profile loadProfile(File profileFile){
        try {
            String content = Files.readString(profileFile.toPath());
            JSONObject obj = new JSONObject(content);

            String name = obj.getString("profileName");
            String versionId = obj.getString("versionId");
            LoaderType loader = LoaderType.fromString(obj.optString("loader")); // safer
            String imagePath = obj.optString("profileImagePath", "images/default.png");

            // Profile Object bauen
            Profile.ProfileFactory factory = new Profile.ProfileFactory(name, versionId, loader);
            factory.setProfileImagePath(imagePath);

            return factory.build(); // fertiges Profil zurückgeben

        } catch (IOException e) {
            e.printStackTrace();
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
                System.out.println("Profile Folder existiert, aber keine profile.json: " + profileFolder.getName());
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
