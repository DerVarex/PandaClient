package com.dervarex.PandaClient.Minecraft.modrinth;

import java.util.List;

public class test {
    public static void main(String[] args) {
        ModrinthDownloader downloader = new ModrinthDownloader();
        List<ModrinthProject> modsWithCat = downloader.search("TerraBlender", Category.WORLDGEN, 10);
        List<ModrinthProject> modsNoCat = downloader.search("TerraBlender", 10);
        System.out.println("With category (WORLDGEN) found: " + modsWithCat.size());
        for (ModrinthProject mod : modsWithCat) {
            System.out.println("- " + mod.getTitle() + " id=" + mod.getId() + " slug=" + mod.getSlug());
        }
        System.out.println("Without category filter found: " + modsNoCat.size());
        for (ModrinthProject mod : modsNoCat) {
            System.out.println("- " + mod.getTitle() + " id=" + mod.getId() + " slug=" + mod.getSlug());
        }
    }
}
