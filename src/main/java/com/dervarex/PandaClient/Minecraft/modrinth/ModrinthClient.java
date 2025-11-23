package com.dervarex.PandaClient.Minecraft.modrinth;

import com.dervarex.PandaClient.Minecraft.logger.ClientLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lightweight Modrinth API client for searching and downloading mods.
 * Docs: https://docs.modrinth.com/api/
 */
public class ModrinthClient {
    private static final String BASE = "https://api.modrinth.com/v2";

    /** Overload ohne Kategorie (Filter optional). */
    public List<ModrinthProject> search(String query, int maxResults) {
        return search(query, null, maxResults, null);
    }

    /**
     * Search projects by query and optional category. Limit results.
     * Proper facets format: facets=[["categories:worldgen"]]
     */
    public List<ModrinthProject> search(String query, Category category, int maxResults, String gameVersion) {
        try {
            StringBuilder urlBuilder = new StringBuilder(BASE).append("/search?query=")
                    .append(encode(query));
            // Build facets array-of-arrays JSON if category or version provided
            java.util.List<String> facetParts = new java.util.ArrayList<>();
            if (category != null) {
                facetParts.add("[\"categories:" + category.apiValue() + "\"]");
            }
            if (gameVersion != null && !gameVersion.isBlank()) {
                facetParts.add("[\"versions:" + gameVersion + "\"]");
            }
            if (!facetParts.isEmpty()) {
                String facetsJson = "[" + String.join(",", facetParts) + "]";
                urlBuilder.append("&facets=").append(encode(facetsJson));
            }
            String finalUrl = urlBuilder.toString();
            ClientLogger.log("Modrinth search URL: " + finalUrl, "INFO", "ModrinthClient");
            JSONObject json = getJson(finalUrl);
            JSONArray hits = json.optJSONArray("hits");
            if (hits == null) {
                ClientLogger.log("No 'hits' array in response", "WARN", "ModrinthClient");
                return Collections.emptyList();
            }

            List<ModrinthProject> out = new ArrayList<>();
            for (int i = 0; i < hits.length() && out.size() < maxResults; i++) {
                JSONObject h = hits.getJSONObject(i);
                String id = h.optString("project_id", h.optString("id"));
                String slug = h.optString("slug");
                String title = h.optString("title");
                String desc = h.optString("description");
                JSONArray catsArr = h.optJSONArray("categories");
                List<String> cats = new ArrayList<>();
                if (catsArr != null) for (int c = 0; c < catsArr.length(); c++) cats.add(catsArr.getString(c));
                String latestVersion = h.optString("latest_version", null);
                out.add(new ModrinthProject(id, slug, title, desc, cats, latestVersion));
            }
            ClientLogger.log("Modrinth search query='" + query + "' results=" + out.size(), "INFO", "ModrinthClient");
            return out;
        } catch (Exception e) {
            ClientLogger.log("Modrinth search failed: " + e.getMessage(), "ERROR", "ModrinthClient");
            return Collections.emptyList();
        }
    }

    /** Backwards-compatible overload without gameVersion */
    public List<ModrinthProject> search(String query, Category category, int maxResults) {
        return search(query, category, maxResults, null);
    }

    /** Download primary file of a version to target directory. Returns downloaded file or null. */
    public File downloadVersionFile(String versionId, File targetDir) {
        try {
            JSONObject version = getJson(BASE + "/version/" + encode(versionId));
            JSONArray files = version.optJSONArray("files");
            if (files == null || files.length() == 0) {
                ClientLogger.log("No files in version " + versionId, "WARN", "ModrinthClient");
                return null;
            }
            JSONObject primary = null;
            for (int i = 0; i < files.length(); i++) {
                JSONObject f = files.getJSONObject(i);
                if (f.optBoolean("primary", false)) { primary = f; break; }
            }
            if (primary == null) primary = files.getJSONObject(0);
            String url = primary.getString("url");
            String filename = primary.getString("filename");
            long size = primary.optLong("size", -1);
            String sha1 = primary.optJSONObject("hashes") != null ? primary.getJSONObject("hashes").optString("sha1") : null;
            ModrinthVersionFile vf = new ModrinthVersionFile(url, filename, size, sha1);
            if (!targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                if (!created) {
                    ClientLogger.log("Could not create target dir for download: " + targetDir.getAbsolutePath(), "ERROR", "ModrinthClient");
                    return null;
                }
            }
            File out = new File(targetDir, vf.getFilename());
            safeDownload(vf.getUrl(), out);
            ClientLogger.log("Downloaded Modrinth file: " + out.getAbsolutePath(), "INFO", "ModrinthClient");
            return out;
        } catch (Exception e) {
            ClientLogger.log("Download version file failed: " + e.getMessage(), "ERROR", "ModrinthClient");
            return null;
        }
    }

    /** Convenience: search then download first match's latestVersionId (if any). */
    public File searchAndDownloadFirst(String query, Category category, File targetDir) {
        List<ModrinthProject> list = search(query, category, 1);
        if (list.isEmpty()) return null;
        ModrinthProject p = list.get(0);
        if (p.getLatestVersionId() == null) {
            ClientLogger.log("Project has no latestVersionId: " + p.getTitle(), "WARN", "ModrinthClient");
            return null;
        }
        return downloadVersionFile(p.getLatestVersionId(), targetDir);
    }

    /** Resolve a compatible version id for a project (id or slug) using optional gameVersion and loader filters. */
    public String resolveVersionIdForProject(String projectIdOrSlug, String gameVersion, String loader) {
        try {
            StringBuilder url = new StringBuilder(BASE)
                    .append("/project/")
                    .append(encode(projectIdOrSlug))
                    .append("/version");
            java.util.List<String> queryParts = new java.util.ArrayList<>();
            if (gameVersion != null && !gameVersion.isBlank()) {
                // API expects game_versions=["1.21.1"]
                queryParts.add("game_versions=" + encode("[\"" + gameVersion + "\"]"));
            }
            if (loader != null && !loader.isBlank()) {
                // Lowercase loader (e.g., fabric, forge, quilt)
                queryParts.add("loaders=" + encode("[\"" + loader.toLowerCase() + "\"]"));
            }
            if (!queryParts.isEmpty()) {
                url.append("?").append(String.join("&", queryParts));
            }
            String finalUrl = url.toString();
            ClientLogger.log("Modrinth version resolution URL: " + finalUrl, "INFO", "ModrinthClient");
            // This endpoint returns an array of versions
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(finalUrl).openConnection();
            conn.setRequestProperty("User-Agent", "PandaClient-Modrinth/1.0");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            int code = conn.getResponseCode();
            java.io.InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            String body;
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
                body = br.lines().collect(java.util.stream.Collectors.joining());
            }
            if (code < 200 || code >= 300) {
                ClientLogger.log("Version list HTTP " + code + " body=" + body, "WARN", "ModrinthClient");
                return null;
            }
            org.json.JSONArray arr = new org.json.JSONArray(body);
            if (arr.isEmpty()) {
                ClientLogger.log("No versions returned for project " + projectIdOrSlug, "WARN", "ModrinthClient");
                return null;
            }
            // Prefer release versions; fallback to first.
            String fallbackId = null;
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject v = arr.getJSONObject(i);
                String id = v.optString("id");
                if (fallbackId == null && id != null && !id.isBlank()) fallbackId = id;
                String type = v.optString("version_type", "").toLowerCase();
                if ("release".equals(type) && id != null && !id.isBlank()) {
                    ClientLogger.log("Chose release version id " + id + " for project " + projectIdOrSlug, "INFO", "ModrinthClient");
                    return id;
                }
            }
            ClientLogger.log("Using fallback version id " + fallbackId + " for project " + projectIdOrSlug, "INFO", "ModrinthClient");
            return fallbackId;
        } catch (Exception e) {
            ClientLogger.log("Resolve version failed: " + e.getMessage(), "ERROR", "ModrinthClient");
            return null;
        }
    }

    /** Download by project id/slug selecting a compatible version using optional filters. */
    public File downloadByProject(String projectIdOrSlug, File targetDir, String gameVersion, String loader) {
        String versionId = resolveVersionIdForProject(projectIdOrSlug, gameVersion, loader);
        if (versionId == null) return null;
        return downloadVersionFile(versionId, targetDir);
    }

    // Basic GET returning JSON object with status code check
    private JSONObject getJson(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "PandaClient-Modrinth/1.0");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        int code = conn.getResponseCode();
        InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String json = br.lines().collect(Collectors.joining());
            if (code < 200 || code >= 300) {
                throw new RuntimeException("HTTP " + code + " body=" + json);
            }
            return new JSONObject(json);
        }
    }

    private static String encode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private void safeDownload(String urlStr, File target) throws Exception {
        Files.createDirectories(target.getParentFile().toPath());
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "PandaClient-Modrinth/1.0");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(30000);
        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream out = new FileOutputStream(target)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        }
    }
}
