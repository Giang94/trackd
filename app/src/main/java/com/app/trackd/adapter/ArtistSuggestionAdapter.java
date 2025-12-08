package com.app.trackd.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.app.trackd.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ArtistSuggestionAdapter extends ArrayAdapter<String> {

    private List<String> allArtists;
    private List<String> filteredArtists;

    public ArtistSuggestionAdapter(@NonNull Context context, @NonNull List<String> artists) {
        super(context, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(artists));
        this.allArtists = new ArrayList<>(artists);
        this.filteredArtists = new ArrayList<>(artists);
    }

    @Override
    public int getCount() {
        return filteredArtists.size();
    }

    @Nullable
    @Override
    public String getItem(int position) {
        return filteredArtists.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();

                if (constraint == null || constraint.length() == 0) {
                    filteredArtists = new ArrayList<>(allArtists);
                } else {
                    String raw = constraint.toString();
                    String query = StringUtils.normalize(raw);

                    List<String> matches = new ArrayList<>();

                    for (String name : allArtists) {
                        String norm = StringUtils.normalize(name);

                        if (norm.contains(query)) {
                            matches.add(name);
                            continue;
                        }

                        int score = fuzzyScore(query, norm);
                        if (score <= 2) {
                            matches.add(name);
                        }
                    }

                    filteredArtists = matches;
                }

                results.values = filteredArtists;
                results.count = filteredArtists.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                notifyDataSetChanged();
            }
        };
    }

    private int fuzzyScore(String query, String target) {
        int qLen = query.length();
        int tLen = target.length();

        if (qLen == 0 || tLen == 0) return Integer.MAX_VALUE;

        if (target.startsWith(query)) return 0;

        int[][] dp = new int[qLen + 1][tLen + 1];

        for (int i = 0; i <= qLen; i++) dp[i][0] = i;
        for (int j = 0; j <= tLen; j++) dp[0][j] = j;

        for (int i = 1; i <= qLen; i++) {
            for (int j = 1; j <= tLen; j++) {
                int cost = query.charAt(i - 1) == target.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[qLen][tLen];
    }
}
