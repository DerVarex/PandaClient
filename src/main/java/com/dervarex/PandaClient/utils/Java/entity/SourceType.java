package com.dervarex.PandaClient.utils.Java.entity;

public enum SourceType {
    AZUL("Azul Zulu"),
    ADOPTIUM("Eclipse Adoptium");

    final String displayName;

    SourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}