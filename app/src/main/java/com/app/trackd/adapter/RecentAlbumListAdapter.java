package com.app.trackd.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.trackd.R;
import com.app.trackd.database.AppDatabase;
import com.app.trackd.model.Album;
import com.app.trackd.model.Artist;
import com.app.trackd.util.ImageUtils;
import com.app.trackd.util.StringUtils;
import com.bumptech.glide.Glide;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecentAlbumListAdapter extends RecyclerView.Adapter<RecentAlbumListAdapter.RecentItemViewHolder> {

    private static final int VIEW_TYPE_NORMAL = 0;
    private static final int VIEW_TYPE_MORE = 1;

    private int fullCount;
    private ShowAllCallback showAllCallback;

    public interface OnItemClick {
        void onClick(Album album);
    }

    private List<Album> albums;
    private OnItemClick listener;

    public RecentAlbumListAdapter(List<Album> albums, int fullCount, OnItemClick listener) {
        this.albums = albums;
        this.fullCount = fullCount;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecentItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecentItemViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecentItemViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_MORE) {
            bindMoreView(holder);
            return;
        }
        bindNormalView(holder, albums.get(position));
    }

    private void bindMoreView(RecentItemViewHolder holder) {
        // Items after index 3 → covers 4,5,6,7
        List<Album> nextFour = albums.subList(3, Math.min(7, albums.size()));

        ImageView[] ivs = {holder.iv1, holder.iv2, holder.iv3, holder.iv4};

        // Fill squares
        for (int i = 0; i < 4; i++) {
            if (i < nextFour.size()) {
                // Load cover
                Log.d("ADAPTER", "Loading cover for album: " + nextFour.get(i).getTitle() + " base64: " + nextFour.get(i).getCover().substring(0, 20));
                Glide.with(holder.itemView)
                        .load(ImageUtils.toBitmap(nextFour.get(i).getCover()))
                        .placeholder(R.drawable.ic_gallery)
                        .centerCrop()
                        .into(ivs[i]);
            } else {
                ivs[i].setImageResource(R.drawable.ic_gallery);
                ivs[i].setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }
        }
        holder.ivAlbumCover.setVisibility(View.GONE);
        holder.glFourContainer.setVisibility(View.VISIBLE);

        // Hide text areas
        holder.tvTitle.setVisibility(View.GONE);
        holder.tvArtist.setVisibility(View.GONE);
        holder.tvSubtitle.setVisibility(View.GONE);

        // Show "Show all" text
        holder.tvShowAll.setVisibility(View.VISIBLE);

        holder.tvShowAll.setOnClickListener(v -> {
            if (showAllCallback != null) showAllCallback.onShowAll();
        });

        holder.itemView.setOnClickListener(v -> {
            if (showAllCallback != null) showAllCallback.onShowAll();
        });
    }

    private void bindNormalView(RecentItemViewHolder holder, Album album) {
        // Set placeholder title until artists are loaded
        holder.tvTitle.setText(album.getTitle());
        holder.tvSubtitle.setText(album.getFormat().name() + " | " + album.getYear());
        holder.ivAlbumCover.setImageBitmap(ImageUtils.toBitmap(album.getCover()));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(album);
        });
        holder.ivAlbumCover.setVisibility(View.VISIBLE);
        holder.glFourContainer.setVisibility(View.GONE);

        // Fetch artists asynchronously
        new Thread(() -> {
            AppDatabase db = AppDatabase.get(holder.itemView.getContext());
            List<Artist> artists = db.albumArtistDao().getArtistsForAlbum(album.getId(), 2);
            List<String> artistNames = artists.stream().map(a -> a.displayName).toList();
            holder.itemView.post(() -> holder.tvArtist.setText(StringUtils.formatArtists(artistNames)));
        }).start();
    }

    @Override
    public int getItemCount() {
        return Math.min(albums.size(), 4);
    }

    static class RecentItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvArtist, tvSubtitle, tvShowAll;
        ImageView ivAlbumCover;
        View glFourContainer;
        ImageView iv1, iv2, iv3, iv4;

        RecentItemViewHolder(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvArtist = v.findViewById(R.id.tvArtist);
            tvSubtitle = v.findViewById(R.id.tvSubtitle);
            ivAlbumCover = v.findViewById(R.id.ivAlbumCover);
            tvShowAll = v.findViewById(R.id.tvShowAll);

            glFourContainer = itemView.findViewById(R.id.includeFourCovers);

            iv1 = itemView.findViewById(R.id.ivCover1);
            iv2 = itemView.findViewById(R.id.ivCover2);
            iv3 = itemView.findViewById(R.id.ivCover3);
            iv4 = itemView.findViewById(R.id.ivCover4);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 3 && fullCount > 4) {
            return VIEW_TYPE_MORE;
        }
        return VIEW_TYPE_NORMAL;
    }

    public void updateData(List<Album> newList) {
        this.albums = newList;
        notifyDataSetChanged();
    }

    public interface ShowAllCallback {
        void onShowAll();
    }

    public void setShowAllCallback(ShowAllCallback callback) {
        this.showAllCallback = callback;
    }
}
