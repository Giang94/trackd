package com.app.trackd.util;

import java.text.Normalizer;

public class StringUtils {


    public static String normalizeArtistName(String name) {
        if (name == null) return "";
        String trimmed = name.trim().toLowerCase();
        // Replace accented characters with base characters
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", ""); // remove diacritics
        // Remove non-letter characters (like punctuation, spaces)
        normalized = normalized.replaceAll("[^a-z]", "");
        return normalized;
    }
}
