package com.dervarex.PandaClient.GUI;

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
                return jsonResponse("{\"success\":false, \"error\":\"" + e.getMessage() + "\"}");
            }
        }
        // --- /loginWithToken ---
        if ("/loginWithToken".equals(uri)) {
            try {
                AuthManager.loginWithSavedToken();
                return jsonResponse("{\"success\":true}");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        // --- /login ---
        if ("/login".equals(uri)) {
            Map<String, String> params = session.getParms();
            String email = params.get("email");
            String password = params.get("password");

            boolean worked = false;
            try {
                if (AuthManager.hasTokenSaved()) {
                    AuthManager.loginWithSavedToken();
                } else {
                    AuthManager.loginWithCredentials(email, password);
                }
                worked = true;
            } catch (Exception e) {
                worked = false;
            }

            String json = "{\"success\":" + worked + "}";
            return jsonResponse(json);
        }

        // --- Not Found ---
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
