package com.app.trackd.model.enums;

public enum AlbumFormat {

    CD("CD"),
    VINYL("Vinyl 12\""),
    CASSETTE("Cassette"),
    VINYL_7("Vinyl 7\""),
    VINYL_10("Vinyl 10\"");

    private final String displayName;

    AlbumFormat(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}