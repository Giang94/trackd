package com.app.trackd.database;

import com.app.trackd.model.enums.AlbumFormat;

public class AlbumFormatConverter {
    public static String mapFormatForDb(String display) {
        switch (display) {
            case "CASSETTE":
                return AlbumFormat.CASSETTE.name();
            case "VINYL 12\"":
                return AlbumFormat.VINYL.name();
            case "VINYL 10\"":
                return AlbumFormat.VINYL_10.name();
            case "VINYL 7\"":
                return AlbumFormat.VINYL_7.name();
            default:
                return AlbumFormat.CD.name();
        }
    }

}
