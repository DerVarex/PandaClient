package com.dervarex.PandaClient.Auth.old;

import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import fr.litarvan.openauth.microsoft.model.response.MinecraftProfile;

import java.io.File;
import java.io.FileWriter;

public class PandaClientAuth {

    public static boolean login(String email, String password) {
        UserSession session = UserSession.getInstance();

        String appData = System.getenv("APPDATA");
        String userFolder = appData + File.separator + "PandaClient" + File.separator + "user";
        File folder = new File(userFolder);

        // Refresh login wird über SavedUserLoader vorgenommen, damit es etwas cleaner ist

        //  Normales Login mit Passwort (wenn Refresh nicht ging)
        try {
            MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
            MicrosoftAuthResult result = authenticator.loginWithCredentials(email, password);
            MinecraftProfile profile = result.getProfile();
            System.out.println("✅ Logged in as: " + profile.getName());

            session.addUser(
                    profile.getId(),
                    profile.getName(),
                    result.getAccessToken(),
                    result.getRefreshToken()
            );

            //  Speichere den Refresh-Token
            try {
                String fileName = profile.getName() + "_userdata";

                if (!folder.exists()) {
                    boolean created = folder.mkdirs();
                    System.out.println(created ? "Folder created: " + userFolder : "Cannot create folder!");
                }

                File file = new File(userFolder, fileName);
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(result.getRefreshToken());
                    System.out.println("💾 Refreshtoken saved in: " + file.getAbsolutePath());
                }

            } catch (Exception e) {
                System.out.println("Error saving refresh token: " + e.getMessage());
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Login failed: " + e.getMessage());
            return false;
        }
    }

}

/* MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
MicrosoftAuthResult result = authenticator.loginWithCredentials("email", "password");
      // Or using a webview: authenticator.loginWithWebView();
  // Or using refresh token: authenticator.loginWithRefreshToken("refresh token");
   // Or using your own way: authenticator.loginWithTokens("access token", "refresh token");

System.out.printf("Logged in with '%s'%n", result.getProfile().getName()); */