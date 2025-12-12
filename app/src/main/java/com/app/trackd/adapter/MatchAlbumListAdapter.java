package com.app.trackd.adapter;

import static com.app.trackd.util.SizeUtils.dpToPx;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.app.trackd.R;
import com.app.trackd.database.AppDatabase;
import com.app.trackd.model.Album;
import com.app.trackd.model.Artist;
import com.app.trackd.util.ImageUtils;
import com.app.trackd.util.StringUtils;

import java.util.List;

public class MatchAlbumListAdapter extends RecyclerView.Adapter<MatchAlbumListAdapter.RecentItemViewHolder> {
    private AppDatabase db;
    private List<Album> albums;

    public MatchAlbumListAdapter(List<Album> albums) {
        this.albums = albums;
    }

    @NonNull
    @Override
    public RecentItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecentItemViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match_result_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecentItemViewHolder holder, int position) {
        Context context = holder.itemView.getContext();

        Album album = albums.get(position);
        holder.tvTitle.setText(album.getTitle());

        String subtitleStr = album.getFormat().getDisplayName() + " • " + album.getYear();
        holder.tvSubtitle.setText(subtitleStr);
        holder.ivAlbumCover.setImageBitmap(ImageUtils.toBitmap(album.getCover()));
        holder.ivAlbumCover.setVisibility(View.VISIBLE);

        db = AppDatabase.get(context);
        List<Artist> artists = db.albumArtistDao().getArtistsForAlbum(album.getId());
        List<String> artistNames = artists.stream().map(a -> a.displayName).toList();
        holder.itemView.post(() -> holder.tvArtist.setText(StringUtils.formatArtists(artistNames)));

        // Badge logic
        if (position < 3) {
            holder.tvBadge.setText(String.valueOf(position + 1));
            holder.tvBadge.setVisibility(View.VISIBLE);
            int badgeColor, textColor;
            switch (position) {
                case 0:
                    badgeColor = ContextCompat.getColor(context, R.color.first_badge_bg);
                    textColor = ContextCompat.getColor(context, R.color.first_badge_text);
                    break; // gold
                case 1:
                    badgeColor = ContextCompat.getColor(context, R.color.second_badge_bg);
                    textColor = ContextCompat.getColor(context, R.color.second_badge_text);
                    break; // silver
                case 2:
                    badgeColor = ContextCompat.getColor(context, R.color.third_badge_bg);
                    textColor = ContextCompat.getColor(context, R.color.third_badge_text);
                    break; // bronze
                default:
                    badgeColor = Color.GRAY;
                    textColor = Color.WHITE;
            }
            GradientDrawable bg = (GradientDrawable) holder.tvBadge.getBackground().mutate();
            bg.setColor(badgeColor);
            holder.tvBadge.setBackground(bg);
            holder.tvBadge.setTextColor(textColor);
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecentItemViewHolder holder) {
        super.onViewAttachedToWindow(holder);

        RecyclerView parent = (RecyclerView) holder.itemView.getParent();
        if (parent == null) return;

        int parentWidth = parent.getWidth();
        int spacing = dpToPx(holder.itemView.getContext(), 16);

        int cardWidth;
        int itemCount = getItemCount();

        if (itemCount <= 2) {
            // exactly 2 cards fill the parent
            cardWidth = (parentWidth - spacing) / 2;
        } else {
            // peek for next card: reduce width a bit
            // 2.5 cards visible at a time
            cardWidth = (parentWidth - spacing) * 2 / 5; // 2/5 = 2.5 cards
        }

        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        params.width = cardWidth;
        holder.itemView.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    public void updateData(List<Album> newList) {
        this.albums = newList;
        notifyDataSetChanged();
    }

    static class RecentItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvArtist, tvSubtitle, tvShowAll, tvBadge;
        ImageView ivAlbumCover;

        RecentItemViewHolder(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvArtist = v.findViewById(R.id.tvArtist);
            tvSubtitle = v.findViewById(R.id.tvSubtitle);
            ivAlbumCover = v.findViewById(R.id.ivAlbumCover);
            tvShowAll = v.findViewById(R.id.tvShowAll);
            tvBadge = v.findViewById(R.id.tvBadge);
        }
    }
}
