package com.app.trackd.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    public static List<String> getNames() {
        return Arrays.stream(AlbumFormat.values())
                .map(AlbumFormat::name)
                .collect(Collectors.toList());
    }

    public static List<String> getVinylNames() {
        return Arrays.asList(VINYL.name(), VINYL_7.name(), VINYL_10.name());
    }

    public static List<String> getDisplayNames() {
        return Arrays.stream(AlbumFormat.values())
                .map(AlbumFormat::getDisplayName)
                .collect(Collectors.toList());
    }
}