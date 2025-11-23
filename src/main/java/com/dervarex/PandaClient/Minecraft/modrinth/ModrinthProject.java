package com.dervarex.PandaClient.Minecraft.modrinth;

import java.util.List;

/** Simple POJO for a Modrinth project search result */
public class ModrinthProject {
    private final String id;
    private final String slug;
    private final String title;
    private final String description;
    private final List<String> categories;
    private final String latestVersionId; // can be null

    public ModrinthProject(String id, String slug, String title, String description, List<String> categories, String latestVersionId) {
        this.id = id;
        this.slug = slug;
        this.title = title;
        this.description = description;
        this.categories = categories;
        this.latestVersionId = latestVersionId;
    }

    public String getId() { return id; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<String> getCategories() { return categories; }
    public String getLatestVersionId() { return latestVersionId; }
}

