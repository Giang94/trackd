package com.app.trackd.util;

import android.text.Layout;
import android.text.TextUtils;
import android.view.ViewTreeObserver;
import android.widget.TextView;

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

    public static void balanceText(final TextView textView) {
        textView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        Layout layout = textView.getLayout();
                        if (layout == null) return;

                        String textStr = textView.getText().toString();

                        int lineCount = layout.getLineCount();
                        if (lineCount < 2) return; // nothing to fix

                        int lastLineStart = layout.getLineStart(lineCount - 1);
                        int lastLineEnd = layout.getLineEnd(lineCount - 1);
                        String lastLineText = textStr.substring(lastLineStart, lastLineEnd).trim();

                        String[] lastLineWords = lastLineText.split(" ");

                        // If last line has only 1 word → orphan detected
                        if (lastLineWords.length == 1) {
                            // Previous line
                            int prevLineStart = layout.getLineStart(lineCount - 2);
                            int prevLineEnd = layout.getLineEnd(lineCount - 2);
                            String prevLineText = textStr.substring(prevLineStart, prevLineEnd).trim();

                            String[] prevWords = prevLineText.split(" ");
                            if (prevWords.length > 1) {
                                // Move last word of previous line to lonely last line
                                int breakPoint = textStr.indexOf(prevWords[prevWords.length - 1], prevLineStart);

                                String newText = textStr.substring(0, breakPoint) + "\n" +
                                        textStr.substring(breakPoint);

                                textView.setText(newText);
                            }
                        }
                        textView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
    }
}
