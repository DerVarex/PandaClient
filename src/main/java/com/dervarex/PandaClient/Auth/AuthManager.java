package com.dervarex.PandaClient.Auth;

import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import fr.litarvan.openauth.microsoft.model.response.MinecraftProfile;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AuthManager {

    private static final Path BASE_DIR = Path.of(System.getenv("APPDATA"), "PandaClient");
    private static final Path KEY_FILE = BASE_DIR.resolve("master.key");
    private static final Path TOKEN_FILE = BASE_DIR.resolve("token.enc");

    private static SecretKey masterKey;
    private static Map<String, User> session = new HashMap<>();

    static {
        try {
            if (!Files.exists(BASE_DIR)) Files.createDirectories(BASE_DIR);
            masterKey = loadOrCreateMasterKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static SecretKey loadOrCreateMasterKey() throws Exception {
        if (Files.exists(KEY_FILE)) {
            byte[] rawKey = Files.readAllBytes(KEY_FILE);
            return new SecretKeySpec(rawKey, "AES");
        } else {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256, new SecureRandom());
            SecretKey key = keyGen.generateKey();
            Files.write(KEY_FILE, key.getEncoded());
            return key;
        }
    }

    private static void saveEncryptedToken(String refreshToken) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, masterKey);
        byte[] encrypted = cipher.doFinal(refreshToken.getBytes());
        Files.write(TOKEN_FILE, Base64.getEncoder().encode(encrypted));
    }

    private static String loadEncryptedToken() throws Exception {
        if (!Files.exists(TOKEN_FILE)) return null;
        byte[] encrypted = Base64.getDecoder().decode(Files.readAllBytes(TOKEN_FILE));
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, masterKey);
        return new String(cipher.doFinal(encrypted));
    }

    // Login mit Email und Passwort
    public static User loginWithCredentials(String email, String password) throws Exception {
        MicrosoftAuthenticator auth = new MicrosoftAuthenticator();
        MicrosoftAuthResult result = auth.loginWithCredentials(email, password);
        MinecraftProfile profile = result.getProfile();

        User user = new User(profile.getId(), profile.getName(),
                result.getAccessToken(), result.getRefreshToken());
        session.put(user.getUuid(), user);

        saveEncryptedToken(user.getRefreshToken());

        return user;
    }

    // Login mit gespeicherten (verschlüsselten) RefreshToken
    public static User loginWithSavedToken() {
        try {
            String refreshToken = loadEncryptedToken();
            if (refreshToken == null) return null;

            MicrosoftAuthenticator auth = new MicrosoftAuthenticator();
            MicrosoftAuthResult result = auth.loginWithRefreshToken(refreshToken);
            MinecraftProfile profile = result.getProfile();

            User user = new User(profile.getId(), profile.getName(),
                    result.getAccessToken(), result.getRefreshToken());
            session.put(user.getUuid(), user);

            saveEncryptedToken(user.getRefreshToken()); // Token aktualisieren
            return user;
        } catch(Exception e) {
            System.err.println("Login mit Token fehlgeschlagen: " + e.getMessage());
            return null;
        }
    }


    public static Boolean hasTokenSaved() {
        return TOKEN_FILE.toFile().exists();
    }

    // Zugriff auf Session
    public static User getUser() {
        return session.values().stream().findFirst().orElse(null);
    }

    // User
    public static class User {
        private final String uuid;
        private final String username;
        private final String accessToken;
        private final String refreshToken;

        public User(String uuid, String username, String accessToken, String refreshToken) {
            this.uuid = uuid;
            this.username = username;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getUuid() { return uuid; }
        public String getUsername() { return username; }
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
    }
}
