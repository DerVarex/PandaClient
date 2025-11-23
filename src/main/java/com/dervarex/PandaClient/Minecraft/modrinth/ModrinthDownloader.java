package com.dervarex.PandaClient.Minecraft.modrinth;

import com.dervarex.PandaClient.Minecraft.logger.ClientLogger;

import java.io.File;
import java.util.List;
import java.util.Optional;

/** High level wrapper offering the requested API: search(...) and download(...) */
public class ModrinthDownloader {
    private final ModrinthClient client = new ModrinthClient();

    /** Overload ohne Kategorie */
    public List<ModrinthProject> search(String query, int maxResults) {
        return client.search(query, maxResults);
    }

    /** Search with optional category and game version */
    public List<ModrinthProject> search(String query, Category category, int maxResults, String gameVersion) {
        return client.search(query, category, maxResults, gameVersion);
    }

    /**
     * Search mods on Modrinth.
     * @param query text query
     * @param category optional category (can be null)
     * @param maxResults max number of results
     * @return list of ModrinthProject
     */
    public List<ModrinthProject> search(String query, Category category, int maxResults) {
        return client.search(query, category, maxResults);
    }

    /**
     * Download a specific version id directly.
     * @param versionId Modrinth version id
     * @param targetDirectory directory to save the jar
     * @return downloaded File or null
     */
    public File downloadVersion(String versionId, File targetDirectory) {
        return client.downloadVersionFile(versionId, targetDirectory);
    }

    /**
     * Convenience: search and download first hit's latest version.
     * @param query text
     * @param category optional category (nullable)
     * @param targetDirectory directory
     * @return downloaded file or null
     */
    public File searchAndDownloadFirst(String query, Category category, File targetDirectory) {
        return client.searchAndDownloadFirst(query, category, targetDirectory);
    }

    /**
     * Download by project id: fetch project, pick latestVersionId.
     */
    public File downloadLatestByProjectId(String projectId, File targetDirectory) {
        List<ModrinthProject> list = client.search(projectId, null, 25);
        Optional<ModrinthProject> match = list.stream().filter(p -> p.getId().equalsIgnoreCase(projectId) || p.getSlug().equalsIgnoreCase(projectId)).findFirst();
        if (match.isEmpty()) {
            ClientLogger.log("No Modrinth project found for id/slug: " + projectId, "WARN", "ModrinthDownloader");
            return null;
        }
        ModrinthProject p = match.get();
        if (p.getLatestVersionId() == null) {
            ClientLogger.log("Project has no latestVersionId: " + p.getTitle(), "WARN", "ModrinthDownloader");
            return null;
        }
        return client.downloadVersionFile(p.getLatestVersionId(), targetDirectory);
    }

    /** Download by project id/slug selecting a compatible version using optional filters. */
    public File downloadByProject(String projectIdOrSlug, File targetDirectory, String gameVersion, String loader) {
        return client.downloadByProject(projectIdOrSlug, targetDirectory, gameVersion, loader);
    }
}
