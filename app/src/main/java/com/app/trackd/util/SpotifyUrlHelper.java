package com.app.trackd.util;

import android.net.Uri;

import java.util.List;

public class SpotifyUrlHelper {

    /**
     * Normalize any Spotify URL or partial input into a clean form:
     * album/<id>
     * track/<id>
     * playlist/<id>
     * artist/<id>
     * <p>
     * Examples:
     * Input: https://open.spotify.com/album/3JUrJP460nFIqwjxM19slT?si=abc
     * Output: album/3JUrJP460nFIqwjxM19slT
     * <p>
     * Input: album/3JUrJP460nFIqwjxM19slT    (already normalized)
     * Output: album/3JUrJP460nFIqwjxM19slT
     */
    public static String normalize(String input) {

        // Already normalized
        if (isNormalized(input)) {
            return input;
        }

        // Try extracting from URL
        String extracted = extractFromUrl(input);
        return extracted;

        // Not a valid Spotify input
    }


    /**
     * Checks if input is already in normalized form: type/id
     */
    private static boolean isNormalized(String input) {
        return input.startsWith("album/") ||
                input.startsWith("track/") ||
                input.startsWith("playlist/") ||
                input.startsWith("artist/");
    }


    /**
     * Extract clean "type/id" from a full Spotify URL.
     */
    private static String extractFromUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            List<String> segments = uri.getPathSegments();

            // Need at least: type + id
            if (segments.size() >= 2) {
                String type = segments.get(0);
                String id = segments.get(1);
                return type + "/" + id;
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }


    /**
     * Build a full Spotify URL from stored "type/id"
     * Example:
     * Input:  album/3JUrJP...
     * Output: https://open.spotify.com/album/3JUrJP...
     */
    public static String toFullUrl(String storedPath) {
        if (storedPath == null || storedPath.trim().isEmpty()) {
            return null;
        }
        return "https://open.spotify.com/" + storedPath;
    }


    /**
     * Extract only the ID (e.g. 3JUrJP460nFIqwjxM19slT)
     */
    public static String extractId(String normalized) {
        if (normalized == null) return null;

        int slash = normalized.indexOf('/');
        if (slash == -1) return null;

        return normalized.substring(slash + 1);
    }


    /**
     * Extract only the type (album / track / playlist / artist)
     */
    public static String extractType(String normalized) {
        if (normalized == null) return null;

        int slash = normalized.indexOf('/');
        if (slash == -1) return null;

        return normalized.substring(0, slash);
    }
}
