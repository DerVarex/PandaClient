package com.dervarex.PandaClient.GUI;

import com.dervarex.PandaClient.Minecraft.MinecraftLauncher;
import com.dervarex.PandaClient.Minecraft.Profile.Profile;
import com.dervarex.PandaClient.Minecraft.Profile.ProfileManagement;
import com.dervarex.PandaClient.Minecraft.loader.LoaderType;
import fi.iki.elonen.NanoHTTPD;
import com.dervarex.PandaClient.Auth.AuthManager;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.dervarex.PandaClient.utils.file.getPandaClientFolder.getPandaClientFolder;

public class ModServer extends NanoHTTPD {
    public ModServer(int port) {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        // --- /openPandaClientFolder ---
        if ("/openPandaClientFolder".equals(uri)) {
            try {
                java.awt.Desktop.getDesktop().open(getPandaClientFolder());
                NotificationServerStart.getNotificationServer().showNotification(NotificationServer.NotificationType.INFO, "PandaClient folder opened");
                return jsonResponse(new JSONObject()
                        .put("success", true)
                        .toString());
            } catch (IOException e) {
                e.printStackTrace();
                NotificationServerStart.getNotificationServer().showNotification(NotificationServer.NotificationType.ERROR, "ERROR: Could not open PandaClient folder: " + e.getMessage());
                return jsonResponse(new JSONObject()
                        .put("success", false)
                        .put("error", e.getMessage())
                        .toString());
            }
        }
        // --- /isLoggedIn ---
        if ("/isLoggedIn".equals(uri)) {
            boolean loggedIn = AuthManager.getUser() != null;
            boolean hasSaved = AuthManager.hasSessionSaved();
            String json = new JSONObject()
                    .put("loggedIn", loggedIn)
                    .put("hasSaved", hasSaved)
                    .toString();
            return jsonResponse(json);
        }
        // --- /launch ---
        if ("/launch".equals(uri)) {
            try {
                Map<String, java.util.List<String>> params = session.getParameters();
                String profileName = null;
                if (params != null) {
                    if (params.containsKey("profileName") && !params.get("profileName").isEmpty()) profileName = params.get("profileName").get(0);
                    else if (params.containsKey("name") && !params.get("name").isEmpty()) profileName = params.get("name").get(0);
                }
                if (profileName == null || profileName.isBlank()) {
                    return jsonResponse(new JSONObject().put("success", false).put("error", "Missing profileName parameter").toString());
                }
                AuthManager.User user = AuthManager.getUser();
                if (user == null) {
                    return jsonResponse(new JSONObject().put("success", false).put("error", "Not logged in").toString());
                }
                ProfileManagement pm = new ProfileManagement();
                java.util.List<Profile> profiles = pm.getProfiles();
                Profile target = null;
                for (Profile p : profiles) {
                    if (profileName.equalsIgnoreCase(p.getProfileName())) { target = p; break; }
                }
                if (target == null) {
                    return jsonResponse(new JSONObject().put("success", false).put("error", "Profile not found: " + profileName).toString());
                }
                // Instanzordner ermitteln (gleiche Logik wie in createProfile)
                File instanceFolder = new File(new File(getPandaClientFolder(), "instances"), target.getProfileName());
                // Spiel starten (launchMc = true)
                MinecraftLauncher.LaunchMinecraft(target.getVersionId(), user.getUsername(), user.getUuid(), user.getAccessToken(), instanceFolder, true);
                NotificationServerStart.getNotificationServer().showNotification(NotificationServer.NotificationType.INFO, "Launching " + target.getProfileName());
                return jsonResponse(new JSONObject().put("success", true).put("launched", target.getProfileName()).toString());
            } catch (Exception e) {
                e.printStackTrace();
                NotificationServerStart.getNotificationServer().showNotification(NotificationServer.NotificationType.ERROR, "ERROR: Launch failed: " + e.getMessage());
                return jsonResponse(new JSONObject().put("success", false).put("error", e.getMessage()).toString());
            }
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
                String body = new String(buffer, StandardCharsets.UTF_8);

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
                NotificationServerStart.getNotificationServer().showNotification(NotificationServer.NotificationType.ERROR, "ERROR: Error while creating Instance" + e.getMessage());
                return jsonResponse(new JSONObject()
                        .put("success", false)
                        .put("error", e.getMessage())
                        .toString());
            }
        }
        // --- /loginWithToken ---
        if ("/loginWithToken".equals(uri)) {
            try {
                AuthManager.User user = AuthManager.loginWithSavedSession();
                boolean ok = user != null;
                if (!ok) {
                    NotificationServerStart.getNotificationServer().showNotification(NotificationServer.NotificationType.ERROR, "ERROR: Error while trying to login with saved Session");
                }
                return jsonResponse(new JSONObject().put("success", ok).toString());
            } catch (Exception e) {
                NotificationServerStart.getNotificationServer().showNotification(NotificationServer.NotificationType.ERROR, "ERROR: Error while trying to login with saved Session: " + e.getMessage());
                return jsonResponse(new JSONObject().put("success", false).put("error", e.getMessage()).toString());
            }
        }
        if("/isOnline".equals(uri)) {
            boolean online = com.dervarex.PandaClient.utils.NetUtils.NetUtils.isOnline();
            String json = new JSONObject()
                    .put("online", online)
                    .toString();
            return jsonResponse(json);
        }
        // --- /login (start or use saved) ---
        if ("/login".equals(uri)) {
            try {
                // if a saved session exists, try to use it first
                if (AuthManager.hasSessionSaved()) {
                    AuthManager.User user = AuthManager.loginWithSavedSession();
                    return jsonResponse(new JSONObject()
                            .put("success", user != null)
                            .put("usedSaved", true)
                            .toString());
                } else {
                    // start device code login asynchronously
                    String stateJson = AuthManager.startDeviceCodeLoginAsync();
                    // ensure there's always a success flag for backward compatibility
                    JSONObject state = new JSONObject(stateJson);
                    boolean success = "SUCCESS".equalsIgnoreCase(state.optString("status"));
                    state.put("success", success);
                    return jsonResponse(state.toString());
                }
            } catch (Exception e) {
                NotificationServerStart.getNotificationServer().showNotification(NotificationServer.NotificationType.ERROR, "ERROR: Error while trying to login: " + e.getMessage());
                // don't throw; return JSON error
                JSONObject err = new JSONObject()
                        .put("success", false)
                        .put("error", e.getMessage());
                return jsonResponse(err.toString());
            }
        }

        // --- /login/status ---
        if ("/login/status".equals(uri)) {
            JSONObject state = new JSONObject(AuthManager.getLoginStateJson());
            state.put("success", "SUCCESS".equalsIgnoreCase(state.optString("status")));
            return jsonResponse(state.toString());
        }

        // --- /shutdown ---
        if ("/shutdown".equals(uri)) {
            System.out.println("Shutting down PandaClient");
            Response res = jsonResponse(new JSONObject()
                    .put("success", true)
                    .put("message", "Shutting down")
                    .toString());
            try {
                var ns = NotificationServerStart.getNotificationServer();
                if (ns != null) {
                    ns.showNotification(NotificationServer.NotificationType.INFO, "Shutting down PandaClient backend…");
                }
            } catch (Throwable ignored) {}
            new Thread(() -> {
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                try { stop(); } catch (Throwable ignored) {}
                System.exit(0);
            }, "ShutdownThread").start();
            return res;
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
