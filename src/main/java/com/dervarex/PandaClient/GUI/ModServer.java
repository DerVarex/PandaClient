package com.dervarex.PandaClient.GUI;

import com.dervarex.PandaClient.Minecraft.MinecraftLauncher;
import com.dervarex.PandaClient.Minecraft.Profile.Profile;
import com.dervarex.PandaClient.Minecraft.Profile.ProfileManagement;
import com.dervarex.PandaClient.Minecraft.loader.LoaderType;
import fi.iki.elonen.NanoHTTPD;
import com.dervarex.PandaClient.Auth.AuthManager;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ModServer extends NanoHTTPD {
    public ModServer(int port) {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        // --- /isLoggedIn ---
        if ("/isLoggedIn".equals(uri)) {
            String json = "{\"loggedIn\":" + AuthManager.hasTokenSaved() + "}";
            return jsonResponse(json);
        }
        // --- /launch ---
        if ("/launch".equals(uri)) {
            
        }
        // --- /instances ---
        if ("/instances".equals(uri)) {
            ProfileManagement pm = new ProfileManagement();
            String json = pm.getProfilesAsJson();
            return jsonResponse(json);
        }
        // --- /create-instance ---
        if ("/create-instance".equals(uri)) {
            try {
                // Body auslesen
                int contentLength = Integer.parseInt(session.getHeaders().getOrDefault("content-length", "0"));
                byte[] buffer = new byte[contentLength];
                session.getInputStream().read(buffer, 0, contentLength);
                String body = new String(buffer, "UTF-8");

                // JSON parsen
                JSONObject params = new JSONObject(body);

                String name = params.optString("name", "");
                String version = params.optString("version", "");
                String modloader = params.optString("modloader", "");
                LoaderType loader = LoaderType.fromString(modloader);

                String profileimageParam = params.optString("image", "");
                File profileImageFile = null;
                if (!profileimageParam.isEmpty() && !profileimageParam.equals("null")) {
                    profileImageFile = new File(profileimageParam);
                }

                ProfileManagement pm = new ProfileManagement();
                pm.createProfile(name, version, loader, profileImageFile);

                return jsonResponse("{\"success\":true}");
            } catch (Exception e) {
                e.printStackTrace();
                NotificationServerStart.getNotificationServer().showNotification(NotificationServer.NotificationType.ERROR, "ERROR: Error while creating Instance" + e.getMessage()/* + " (ModServer /create-instance)"*/);
                return jsonResponse("{\"success\":false, \"error\":\"" + e.getMessage() + "\"}");
            }
        }
        // --- /loginWithToken ---
        if ("/loginWithToken".equals(uri)) {
            try {
                AuthManager.loginWithSavedToken();
                return jsonResponse("{\"success\":true}");
            } catch (Exception e) {
                NotificationServerStart.getNotificationServer().showNotification(NotificationServer.NotificationType.ERROR, "ERROR: Error while trying to login with token " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
//        // --- /select-instance ---
//        if (uri.startsWith("/select-instance")) {
//            Map<String, String> params = session.getParms();
//            String id = params.get("id");
//
//            // ID speichern (z. B. in ProfileManagement oder statische Variable)
//            ProfileManagement pm = new ProfileManagement();
//            pm.loadProfile(id);
//
//            String json = "{\"success\":true, \"selected\":\"" + id + "\"}";
//            return jsonResponse(json);
//        }

        // --- /login ---
        if ("/login".equals(uri)) {
            Map<String, String> params = session.getParms();
            String email = params.get("Email");
String password = params.get("password");

if (email == null || email.isBlank()) {
    NotificationServerStart.getNotificationServer().showNotification(
        NotificationServer.NotificationType.ERROR, "Email is blank"
    );
    return jsonResponse("{\"success\":false}");
}

if (password == null || password.isBlank()) {
    NotificationServerStart.getNotificationServer().showNotification(
        NotificationServer.NotificationType.ERROR, "Password is blank"
    );
    return jsonResponse("{\"success\":false}");
}

            boolean worked = false;
            try {
                if (AuthManager.hasTokenSaved()) {
                    AuthManager.loginWithSavedToken();
                } else {
                    AuthManager.loginWithCredentials(email, password);
                    NotificationServerStart.getNotificationServer().showNotification(NotificationServer.NotificationType.INFO, "Please login with your Minecraft account");
                }
                worked = true;
            } catch (Exception e) {
                worked = false;
                throw new RuntimeException(e);
            }

            String json = "{\"success\":" + worked + "}";
            return jsonResponse(json);
        }

        // --- Not Found ---
        NotificationServerStart.getNotificationServer().showNotification(NotificationServer.NotificationType.ERROR, "ERROR: Requested URI not found: " + uri + " Please report this to the dev team, thank you");
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"Not Found\"}");

    }

    // Hilfsmethode für JSON + CORS
    private Response jsonResponse(String json) {
        Response res = newFixedLengthResponse(Response.Status.OK, "application/json", json);
        res.addHeader("Access-Control-Allow-Origin", "*");
        res.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        res.addHeader("Access-Control-Allow-Headers", "Content-Type");
        return res;
    }
}
