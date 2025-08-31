package com.dervarex.PandaClient.Auth.old;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UserSession {

    private static UserSession instance;
    private Map<String, User> users;

    private UserSession() {
        users = new HashMap<>();
    }

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void addUser(String uuid, String username, String accessToken, String refreshToken) {
        User user = new User(uuid, username, accessToken, refreshToken);
        users.put(uuid, user); // nur im RAM speichern
        System.out.println("✅ User saved " + username);
    }

    public User getUser() {
        // Gib den ersten User zurück, wenn einer existiert
        if (!users.isEmpty()) {
            User user = users.values().stream().findFirst().orElse(null);
            if (user != null) {
                System.out.println("🔵 User loaded: " + user.getUsername());
                return user;
            }
        }
        System.out.println("🔴 No user found.");
        return null;
    }


    public Collection<User> getUsers() {
        return users.values();
    }

    // Nutzerklasse bleibt gleich
    public static class User {
        private String uuid;
        private String username;
        private String accessToken;
        private String refreshToken;

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
