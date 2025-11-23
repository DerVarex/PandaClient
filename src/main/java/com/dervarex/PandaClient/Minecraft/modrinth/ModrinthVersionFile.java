package com.dervarex.PandaClient.Minecraft.modrinth;

/** Represents a downloadable file (primary file) from a Modrinth version */
public class ModrinthVersionFile {
    private final String url;
    private final String filename;
    private final long size;
    private final String hashesSha1;

    public ModrinthVersionFile(String url, String filename, long size, String hashesSha1) {
        this.url = url;
        this.filename = filename;
        this.size = size;
        this.hashesSha1 = hashesSha1;
    }

    public String getUrl() { return url; }
    public String getFilename() { return filename; }
    public long getSize() { return size; }
    public String getHashesSha1() { return hashesSha1; }
}
