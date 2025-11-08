package com.dervarex.PandaClient.utils.file;

import java.io.IOException;

public class Path {
    private java.nio.file.Path path;

    public Path(java.nio.file.Path path) {
        this.path = path;
    }

    public Path to(String sub) {
        return new Path(path.resolve(sub));
    }

    public Path to(String... subs) {
        java.nio.file.Path temp = path;
        for (String s : subs) {
            temp = temp.resolve(s);
        }
        return new Path(temp);
    }

    public Path parent() {
        return new Path(path.getParent());
    }

    public void move(Path target) {
        try {
            java.nio.file.Files.move(this.path, target.path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete() {
        try {
            if (java.nio.file.Files.isDirectory(path)) {
                java.nio.file.Files.walk(path)
                        .sorted((a, b) -> b.compareTo(a)) // erst Unterdateien
                        .forEach(p -> {
                            try {
                                java.nio.file.Files.delete(p);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            } else {
                java.nio.file.Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public java.nio.file.Path getPath() {
        return path;
    }
    public void extract(String dest, Object options) {

    }
}
