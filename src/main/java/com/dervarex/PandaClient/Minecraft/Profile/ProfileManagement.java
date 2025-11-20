package com.dervarex.PandaClient.Minecraft.Profile;

import com.dervarex.PandaClient.Auth.AuthManager;
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
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
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
    public Profile getProfileByName(String profileName) {
        List<Profile> profiles = getProfiles();
        for (Profile profile : profiles) {
            if (profile.getProfileName().equals(profileName)) {
                return profile;
            }
        }
        return null; // Profil nicht gefunden
    }
    public List<String> getMods(Profile profile) {
        List<String> mods = new ArrayList<>();
        File modsDir = new File(
                new File(getPandaClientFolder.getPandaClientFolder(), "instances"),
                profile.getProfileName() + File.separator + "mods"
        );

        if (!modsDir.exists() || !modsDir.isDirectory()) {
            return mods; // doesn't exist, return empty list
        }

        for (File modFile : modsDir.listFiles((dir, name) -> name.endsWith(".jar"))) {
            mods.add(modFile.getName());
        }

        return mods;
    }
    public JSONObject getModsAsJson(Profile profile) {
        JSONObject ModsJson = new JSONObject();
        List<String> mods = getMods(profile);
        for(String mod : mods) {
            ModsJson.append("mods", mod);
        }
        return ModsJson;
    }
    public void editProfileName(Profile profile, String newName) {
        if (profile == null) {
            ClientLogger.log("editProfileName called with null profile", "ERROR", "ProfileManagement");
            return;
        }

        File instancesDir = new File(getPandaClientFolder.getPandaClientFolder(), "instances");
        File oldFolder = new File(instancesDir, profile.getProfileName());
        File newFolder = new File(instancesDir, newName);

        try {
            if (!oldFolder.exists()) {
                ClientLogger.log("Old profile folder does not exist: " + oldFolder.getAbsolutePath(), "ERROR", "ProfileManagement");
                return;
            }

            // Try atomic / simple move first
            try {
                Files.move(oldFolder.toPath(), newFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveEx) {
                ClientLogger.log("Atomic move failed, attempting copy-delete fallback: " + moveEx.getMessage(), "WARN", "ProfileManagement");
                // Fallback: copy all files then delete old folder
                Files.walk(oldFolder.toPath()).forEach(sourcePath -> {
                    try {
                        Path relative = oldFolder.toPath().relativize(sourcePath);
                        Path targetPath = newFolder.toPath().resolve(relative);
                        if (Files.isDirectory(sourcePath)) {
                            if (!Files.exists(targetPath)) Files.createDirectories(targetPath);
                        } else {
                            if (!Files.exists(targetPath.getParent())) Files.createDirectories(targetPath.getParent());
                            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException io) {
                        throw new RuntimeException(io);
                    }
                });
                // delete old folder recursively
                Files.walk(oldFolder.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            File jsonFile = new File(newFolder, "profile.json");
            if (!jsonFile.exists()) {
                ClientLogger.log("Profile JSON file does not exist for profile: " + newFolder.getName(), "ERROR", "ProfileManagement");
                return;
            }

            String content = Files.readString(jsonFile.toPath());
            JSONObject obj = new JSONObject(content);

            obj.put("profileName", newName);
            obj.put("versionId", profile.getVersionId());
            obj.put("loader", profile.getLoader().toString());

            try (FileWriter file = new FileWriter(jsonFile)) {
                file.write(obj.toString(4));
            }

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            ClientLogger.log("Failed to edit profile: " + sw.toString(), "ERROR", "ProfileManagement");
        }
    }
}
