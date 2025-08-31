package com.dervarex.PandaClient.Auth.old;

import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import fr.litarvan.openauth.microsoft.model.response.MinecraftProfile;

import java.io.File;
import java.nio.file.Files;

public class SavedUserLoader {

    public static void loadSavedUsers() {
        UserSession session = UserSession.getInstance();

        String appData = System.getenv("APPDATA");
        String userFolder = appData + File.separator + "PandaClient" + File.separator + "user";
        File folder = new File(userFolder);

        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("📂 Kein Benutzerordner gefunden: " + userFolder);
            return;
        }

        File[] files = folder.listFiles(File::isFile);
        if (files == null || files.length == 0) {
            System.out.println("⚠️ Keine gespeicherten User gefunden.");
            return;
        }

        for (File file : files) {
            try {
                String refreshToken = Files.readString(file.toPath());

                MicrosoftAuthenticator auth = new MicrosoftAuthenticator();
                MicrosoftAuthResult result = auth.loginWithRefreshToken(refreshToken);
                MinecraftProfile profile = result.getProfile();

                session.addUser(profile.getId(), profile.getName(), result.getAccessToken(), result.getRefreshToken());

                System.out.println("✅ Benutzer geladen: " + profile.getName());

            } catch (Exception e) {
                System.out.println("❌ Fehler beim Laden von " + file.getName() + ": " + e.getMessage());
            }
        }
    }
}
