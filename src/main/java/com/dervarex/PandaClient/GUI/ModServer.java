package com.dervarex.PandaClient.GUI;

import com.dervarex.PandaClient.GUI.WebSocket.NotificationServer.NotificationServer;
import com.dervarex.PandaClient.GUI.WebSocket.NotificationServer.NotificationServerStart;
import com.dervarex.PandaClient.Minecraft.MinecraftLauncher;
import com.dervarex.PandaClient.Minecraft.Profile.Profile;
import com.dervarex.PandaClient.Minecraft.Profile.ProfileManagement;
import com.dervarex.PandaClient.Minecraft.loader.Fabric;
import com.dervarex.PandaClient.Minecraft.loader.LoaderType;
import fi.iki.elonen.NanoHTTPD;
import com.dervarex.PandaClient.Auth.AuthManager;
import org.json.JSONObject;
import com.dervarex.PandaClient.Minecraft.logger.ClientLogger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static com.dervarex.PandaClient.utils.file.getPandaClientFolder.getPandaClientFolder;

public class ModServer extends NanoHTTPD {
    public ModServer(int port) {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        //--- /edit-instance ---
        if ("/edit-instance".equals(uri)) {
            try {
                // Body auslesen
                int contentLength = Integer.parseInt(session.getHeaders().getOrDefault("content-length", "0"));
                byte[] buffer = new byte[contentLength];
                session.getInputStream().read(buffer, 0, contentLength);
                String body = new String(buffer, StandardCharsets.UTF_8);

                // JSON parsen
                JSONObject params = new JSONObject(body);

                String profileName = params.optString("id", "");
                String newName = params.optString("name", "");
                ProfileManagement pm = new ProfileManagement();
                Profile profile = pm.getProfileByName(profileName);
                if (profile == null) {
                    throw new Exception("Profile not found: " + profileName);
                }

                pm.editProfileName(profile, newName);




                ClientLogger.log("Instance edited: " + profileName, "INFO", "ModServer");

                return jsonResponse("{\"success\":true}");
            } catch (Exception e) {
                ClientLogger.log("Edit instance failed: " + e.getMessage(), "ERROR", "ModServer");
                e.printStackTrace();
                NotificationServerStart.getNotificationServer().showNotification(NotificationServer.NotificationType.ERROR, "ERROR: Error while editing Instance: " + e.getMessage());
                return jsonResponse(new JSONObject().put("success", false).put("error", e.getMessage()).toString());
            }
        }
        // --- /openPandaClientFolder ---
        if ("/openPandaClientFolder".equals(uri)) {
            try {
                java.awt.Desktop.getDesktop().open(getPandaClientFolder());
                NotificationServerStart.getNotificationServer().showNotification(NotificationServer.NotificationType.INFO, "PandaClient folder opened");
                ClientLogger.log("Opened PandaClient folder", "INFO", "ModServer");
                return jsonResponse(new JSONObject()
                        .put("success", true)
                        .toString());
            } catch (IOException e) {
                ClientLogger.log("Open folder failed: " + e.getMessage(), "ERROR", "ModServer");
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
                    ClientLogger.log("Missing profileName parameter", "WARN", "ModServer");
                    return jsonResponse(new JSONObject().put("success", false).put("error", "Missing profileName parameter").toString());
                }
                AuthManager.User user = AuthManager.getUser();
                if (user == null) {
                    ClientLogger.log("Launch denied: not logged in", "WARN", "ModServer");
                    return jsonResponse(new JSONObject().put("success", false).put("error", "Not logged in").toString());
                }
                ProfileManagement pm = new ProfileManagement();
                java.util.List<Profile> profiles = pm.getProfiles();
                Profile target = null;
                for (Profile p : profiles) {
                    if (profileName.equalsIgnoreCase(p.getProfileName())) { target = p; break; }
                }
                if (target == null) {
                    ClientLogger.log("Profile not found: " + profileName, "WARN", "ModServer");
                    return jsonResponse(new JSONObject().put("success", false).put("error", "Profile not found: " + profileName).toString());
                }
                // Instanzordner ermitteln (gleiche Logik wie in createProfile)
                File instanceFolder = new File(new File(getPandaClientFolder(), "instances"), target.getProfileName());
                // Spiel starten (launchMc = true)
                ClientLogger.log("Launching instance: " + target.getProfileName(), "INFO", "ModServer");
                if (target.getLoader() == LoaderType.FABRIC) {
                    Profile finalTarget = target;

                    fabricInstall(finalTarget);
                    MinecraftLauncher.LaunchMinecraft(
                            target.getVersionId(),
                            LoaderType.FABRIC,
                            AuthManager.getUser().getUsername(),
                            AuthManager.getUser().getUuid(),
                            AuthManager.getUser().getAccessToken(),
                            instanceFolder,
                            true,
                            Optional.empty()
                    );

                } else {
                    MinecraftLauncher.LaunchMinecraft(
                            target.getVersionId(),
                            LoaderType.VANILLA,
                            user.getUsername(),
                            user.getUuid(),
                            user.getAccessToken(),
                            instanceFolder,
                            true,
                            Optional.empty()
                    );
                }

                NotificationServerStart.getNotificationServer().showNotification(NotificationServer.NotificationType.INFO, "Launching " + target.getProfileName());
                return jsonResponse(new JSONObject().put("success", true).put("launched", target.getProfileName()).toString());
            } catch (Exception e) {
                ClientLogger.log("Launch failed: " + e.getMessage(), "ERROR", "ModServer");
                e.printStackTrace();
                NotificationServerStart.getNotificationServer().showNotification(NotificationServer.NotificationType.ERROR, "ERROR: Launch failed: " + e.getMessage());
                return jsonResponse(new JSONObject().put("success", false).put("error", e.getMessage()).toString());
            }
        }
        if("/mods".equals(uri)) {
            try {
                // Body auslesen
                int contentLength = Integer.parseInt(session.getHeaders().getOrDefault("content-length", "0"));
                byte[] buffer = new byte[contentLength];
                session.getInputStream().read(buffer, 0, contentLength);
                String body = new String(buffer, StandardCharsets.UTF_8);

                // JSON parsen
                JSONObject params = new JSONObject(body);
                String profileName = params.optString("profileName", "");
                ProfileManagement pm = new ProfileManagement();
                JSONObject json = pm.getModsAsJson(pm.getProfileByName(profileName));
                return jsonResponse(json.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
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

                ProfileManagement pm = new ProfileManagement();
                pm.createProfile(name, version, loader);

                ClientLogger.log("Instance created: " + name, "INFO", "ModServer");

                return jsonResponse("{\"success\":true}");
            } catch (Exception e) {
                ClientLogger.log("Create instance failed: " + e.getMessage(), "ERROR", "ModServer");
                e.printStackTrace();
                NotificationServerStart.getNotificationServer().showNotification(NotificationServer.NotificationType.ERROR, "ERROR: Error while creating Instance" + e.getMessage());
                return jsonResponse(new JSONObject().put("success", false).put("error", e.getMessage()).toString());
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
                ClientLogger.log("LoginWithToken error: " + e.getMessage(), "ERROR", "ModServer");
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
                ClientLogger.log("Login start failed: " + e.getMessage(), "ERROR", "ModServer");
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
            ClientLogger.log("Shutting down PandaClient", "INFO", "ModServer");
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
        ClientLogger.log("Requested URI not found: " + uri, "WARN", "ModServer");
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

    public void fabricInstall(Profile profile) {
        Fabric fabric = new Fabric();
        fabric.install(profile.getVersionId(), new File(new File(getPandaClientFolder(), "instances"), profile.getProfileName()), profile.getProfileName());
    }
}
