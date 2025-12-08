package com.app.trackd.util;

import android.text.TextUtils;

import java.text.Normalizer;
import java.util.List;

public class StringUtils {

    public static String normalize(String name) {
        if (name == null) return "";
        String trimmed = name.trim().toLowerCase();
        // Replace accented characters with base characters
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", ""); // remove diacritics
        // Remove non-letter characters (like punctuation, spaces)
        normalized = normalized.replaceAll("[^a-z]", "");
        return normalized;
    }

    public static String formatArtistsLimited(List<String> artists, int limit) {
        if (artists == null || artists.isEmpty()) return "";

        if (artists.size() <= limit) {
            return TextUtils.join(" • ", artists);
        }

        int remaining = artists.size() - 2;
        return artists.get(0) + " • " + artists.get(1) + " • +" + remaining + " more";
    }

    public static String formatArtists(List<String> artists) {
        if (artists == null || artists.isEmpty()) return "";

        return TextUtils.join(" • ", artists);
    }
}
