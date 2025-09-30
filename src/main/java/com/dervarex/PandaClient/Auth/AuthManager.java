package com.dervarex.PandaClient.Auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.StepMCProfile;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * AuthManager für PandaClient
 * - Speichert die komplette MinecraftAuth Session verschlüsselt
 * - Lädt und refreshed sie beim Start automatisch
 * - Login via DeviceCode (mit Code/Link für den Nutzer)
 */
public class AuthManager {

    private static final Path BASE_DIR = Path.of(System.getenv("APPDATA"), "PandaClient");
    private static final Path KEY_FILE = BASE_DIR.resolve("master.key");
    private static final Path SESSION_FILE = BASE_DIR.resolve("session.enc"); // <== jetzt gesamte Session

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

    // --- Key Handling ------------------------------------------------------

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

    private static void saveEncryptedSession(JsonObject sessionJson) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, masterKey);
        byte[] encrypted = cipher.doFinal(sessionJson.toString().getBytes());
        Files.write(SESSION_FILE, Base64.getEncoder().encode(encrypted));
    }
    private static final Gson GSON = new Gson();

    private static JsonObject loadEncryptedSession() throws Exception {
        if (!Files.exists(SESSION_FILE)) return null;
        byte[] encrypted = Base64.getDecoder().decode(Files.readAllBytes(SESSION_FILE));
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, masterKey);
        String json = new String(cipher.doFinal(encrypted));
        return GSON.fromJson(json, JsonObject.class);
    }

    // --- Login mit DeviceCode ----------------------------------------------

    /**
     * Führt einen neuen Login durch, speichert die komplette Session
     */
    public static User login() {
        HttpClient httpClient = MinecraftAuth.createHttpClient();
        try {
            StepFullJavaSession.FullJavaSession javaSession =
                    MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(httpClient,
                            new StepMsaDeviceCode.MsaDeviceCodeCallback(msa -> {
                                System.out.println("Go to " + msa.getVerificationUri());
                                System.out.println("Enter code " + msa.getUserCode());
                                System.out.println("Direct URL: " + msa.getDirectVerificationUri());
                            }));

            JsonObject serialized = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(javaSession);
            saveEncryptedSession(serialized); // ganze Session speichern

            StepMCProfile.MCProfile profile = javaSession.getMcProfile();
            User user = new User(profile.getId().toString(),
                    profile.getName(),
                    profile.getMcToken().getAccessToken(),
                    serialized);
            session.put(user.getUuid(), user);
            return user;
        } catch (Exception e) {
            throw new RuntimeException("Login failed", e);
        }
    }

    // --- Login mit gespeicherter Session + Refresh -------------------------

    /**
     * Lädt gespeicherte Session und refresht Tokens falls nötig
     */
    public static User loginWithSavedSession() {
        try {
            JsonObject saved = loadEncryptedSession();
            if (saved == null) return null;

            HttpClient httpClient = MinecraftAuth.createHttpClient();

            // aus gespeicherter Session wieder FullJavaSession erstellen
            StepFullJavaSession.FullJavaSession loaded =
                    MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.fromJson(saved);

            // Refresh: aktualisiert nur abgelaufene Tokens
            StepFullJavaSession.FullJavaSession refreshed =
                    MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.refresh(httpClient, loaded);

            // neu speichern, falls sich Tokens geändert haben
            JsonObject refreshedJson = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(refreshed);
            saveEncryptedSession(refreshedJson);

            StepMCProfile.MCProfile profile = refreshed.getMcProfile();
            User user = new User(profile.getId().toString(),
                    profile.getName(),
                    profile.getMcToken().getAccessToken(),
                    refreshedJson);
            session.put(user.getUuid(), user);
            return user;
        } catch (Exception e) {
            System.err.println("Login/Refresh fehlgeschlagen: " + e.getMessage());
            return null;
        }
    }

    // --- Hilfsmethoden -----------------------------------------------------

    public static boolean hasSessionSaved() {
        return SESSION_FILE.toFile().exists();
    }

    public static User getUser() {
        return session.values().stream().findFirst().orElse(null);
    }

    // --- User Datenklasse --------------------------------------------------

    public static class User {
        private final String uuid;
        private final String username;
        private final String accessToken;
        private final JsonObject serializedSession;

        public User(String uuid, String username, String accessToken, JsonObject serializedSession) {
            this.uuid = uuid;
            this.username = username;
            this.accessToken = accessToken;
            this.serializedSession = serializedSession;
        }

        public String getUuid() { return uuid; }
        public String getUsername() { return username; }
        public String getAccessToken() { return accessToken; }
        public JsonObject getSerializedSession() { return serializedSession; }
    }
}
