package com.dervarex.PandaClient.Auth;

import com.dervarex.PandaClient.utils.OS.OSUtil;
import com.dervarex.PandaClient.utils.exceptions.NoConnectionException; // added
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
import java.net.*; // diagnostics
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.io.IOException; // diagnostics


public class AuthManager {

    private static final Path BASE_DIR = Path.of(
        System.getenv("APPDATA") != null
            ? System.getenv("APPDATA")
            : System.getProperty("user.home") + "/.config",
        "PandaClient"
    );
    private static final Path KEY_FILE = BASE_DIR.resolve("master.key");
    private static final Path SESSION_FILE = BASE_DIR.resolve("session.enc");

    private static SecretKey masterKey;
    private static Map<String, User> session = new HashMap<>();

    private static final Gson GSON = new Gson();

    // Async login state management
    public enum LoginStatus { IDLE, STARTING, PENDING, SUCCESS, ERROR }
    public static class LoginState {
        public LoginStatus status = LoginStatus.IDLE;
        public String message = "";
        public String userCode = null;
        public String verificationUri = null;
        public String directVerificationUri = null;
        public String username = null;
    }
    private static volatile LoginState loginState = new LoginState();
    private static volatile CountDownLatch codeReadyLatch = null;

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

    private static JsonObject loadEncryptedSession() throws Exception {
        if (!Files.exists(SESSION_FILE)) return null;
        byte[] encrypted = Base64.getDecoder().decode(Files.readAllBytes(SESSION_FILE));
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, masterKey);
        String json = new String(cipher.doFinal(encrypted));
        return GSON.fromJson(json, JsonObject.class);
    }

    public static User login() {
        HttpClient httpClient = MinecraftAuth.createHttpClient();
        try {
            ensureOnline("DeviceCodeLogin");
            StepFullJavaSession.FullJavaSession javaSession =
                    MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(httpClient,
                            new StepMsaDeviceCode.MsaDeviceCodeCallback(msa -> {
                                System.out.println("Go to " + msa.getVerificationUri());
                                System.out.println("Enter code " + msa.getUserCode());
                                System.out.println("Direct URL: " + msa.getDirectVerificationUri());
                                OSUtil.openBrowser(msa.getDirectVerificationUri());
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
        } catch (NoConnectionException nce) {
            throw new RuntimeException(nce.toUserFriendlyMessage(), nce);
        } catch (Exception e) {
            throw new RuntimeException("Login failed", e);
        }
    }

    public static synchronized String startDeviceCodeLoginAsync() {
        // Already in progress?
        if (loginState.status == LoginStatus.PENDING || loginState.status == LoginStatus.STARTING) {
            return GSON.toJson(loginState);
        }
        loginState = new LoginState();
        loginState.status = LoginStatus.STARTING;
        loginState.message = "Starting device code login";
        codeReadyLatch = new CountDownLatch(1);

        new Thread(() -> {
            HttpClient httpClient = MinecraftAuth.createHttpClient();
            try {
                ensureOnline("DeviceCodeLoginAsync");
                StepFullJavaSession.FullJavaSession javaSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(
                        httpClient,
                        new StepMsaDeviceCode.MsaDeviceCodeCallback(msa -> {
                            // expose code & urls immediately
                            loginState.userCode = msa.getUserCode();
                            loginState.verificationUri = msa.getVerificationUri();
                            loginState.directVerificationUri = msa.getDirectVerificationUri();
                            loginState.status = LoginStatus.PENDING;
                            loginState.message = "Waiting for user to authorize in browser";
                            try { OSUtil.openBrowser(msa.getDirectVerificationUri()); } catch (Exception ignored) {}
                            if (codeReadyLatch != null) codeReadyLatch.countDown();
                        })
                );

                JsonObject serialized = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(javaSession);
                saveEncryptedSession(serialized);

                StepMCProfile.MCProfile profile = javaSession.getMcProfile();
                User user = new User(profile.getId().toString(), profile.getName(), profile.getMcToken().getAccessToken(), serialized);
                session.put(user.getUuid(), user);

                loginState.status = LoginStatus.SUCCESS;
                loginState.username = user.getUsername();
                loginState.message = "Login successful";
                if (codeReadyLatch != null) codeReadyLatch.countDown();
            } catch (NoConnectionException nce) {
                loginState.status = LoginStatus.ERROR;
                loginState.message = nce.getMessage();
                if (codeReadyLatch != null) codeReadyLatch.countDown();
            } catch (Exception e) {
                loginState.status = LoginStatus.ERROR;
                loginState.message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                if (codeReadyLatch != null) codeReadyLatch.countDown();
            }
        }, "DeviceCodeLoginThread").start();

        try { if (codeReadyLatch != null) codeReadyLatch.await(2, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        return GSON.toJson(loginState);
    }

    public static synchronized String getLoginStateJson() {
        return GSON.toJson(loginState);
    }

    public static synchronized LoginState getLoginState() {
        return loginState;
    }

    public static synchronized void resetLoginState() {
        loginState = new LoginState();
        codeReadyLatch = null;
    }

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

    public static boolean hasSessionSaved() {
        return SESSION_FILE.toFile().exists();
    }

    public static User getUser() {
        return session.values().stream().findFirst().orElse(null);
    }

    // ---------------- Connectivity Diagnostics ----------------
    private static void ensureOnline(String action) throws NoConnectionException {
        long start; long dnsLatency=-1, tcpLatency=-1, httpLatency=-1;
        boolean dnsOk=false, tcpOk=false, httpOk=false;
        NoConnectionException.Builder builder = new NoConnectionException.Builder().action(action).os(System.getProperty("os.name"));
        // DNS Probe
        try {
            start = System.nanoTime();
            InetAddress addr = InetAddress.getByName("example.com");
            dnsLatency = (System.nanoTime()-start)/1_000_000L;
            dnsOk = addr != null;
            builder.addProbe("dns:example.com", new NoConnectionException.ProbeResult(NoConnectionException.ProbeResult.Type.DNS, dnsOk, dnsLatency, "example.com", addr.getHostAddress()));
        } catch (Exception e) {
            builder.addProbe("dns:example.com", new NoConnectionException.ProbeResult(NoConnectionException.ProbeResult.Type.DNS, false, -1, "example.com", e.getClass().getSimpleName()));
        }
        // TCP Probe (1.1.1.1:53)
        try (Socket s = new Socket()) {
            start = System.nanoTime();
            s.connect(new InetSocketAddress("1.1.1.1",53), 1200);
            tcpLatency = (System.nanoTime()-start)/1_000_000L;
            tcpOk = true;
            builder.addProbe("tcp:1.1.1.1:53", new NoConnectionException.ProbeResult(NoConnectionException.ProbeResult.Type.TCP, true, tcpLatency, "1.1.1.1:53", "connected"));
        } catch (Exception e) {
            builder.addProbe("tcp:1.1.1.1:53", new NoConnectionException.ProbeResult(NoConnectionException.ProbeResult.Type.TCP, false, -1, "1.1.1.1:53", e.getClass().getSimpleName()));
        }
        // HTTP Probe (fast HEAD)
        try {
            start = System.nanoTime();
            HttpURLConnection con = (HttpURLConnection)new URL("https://api.mojang.com").openConnection();
            con.setRequestMethod("HEAD");
            con.setConnectTimeout(1500); con.setReadTimeout(1500);
            int code = con.getResponseCode();
            httpLatency = (System.nanoTime()-start)/1_000_000L;
            httpOk = code >=200 && code < 500; // any response proves connectivity
            builder.addProbe("http:api.mojang.com", new NoConnectionException.ProbeResult(NoConnectionException.ProbeResult.Type.HTTP, httpOk, httpLatency, "https://api.mojang.com", "code="+code));
        } catch (IOException e) {
            builder.addProbe("http:api.mojang.com", new NoConnectionException.ProbeResult(NoConnectionException.ProbeResult.Type.HTTP, false, -1, "https://api.mojang.com", e.getClass().getSimpleName()));
        }
        builder.dnsResolved(dnsOk).tcpAny(tcpOk).httpAny(httpOk);
        if (!(dnsOk || tcpOk || httpOk)) {
            throw builder.build();
        }
    }
    // ----------------------------------------------------------

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
