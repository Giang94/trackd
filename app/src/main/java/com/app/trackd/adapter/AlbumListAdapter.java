package com.app.trackd.adapter;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.trackd.R;
import com.app.trackd.model.Album;
import com.app.trackd.model.AlbumWithArtists;
import com.app.trackd.model.Artist;
import com.app.trackd.util.ImageUtils;
import com.app.trackd.util.StringUtils;

import java.util.List;

public class AlbumListAdapter extends RecyclerView.Adapter<AlbumListAdapter.AlbumViewHolder> {

    private List<AlbumWithArtists> albums;
    private Runnable loadMoreCallback;

    private OnAlbumClickListener listener;

    public AlbumListAdapter(List<AlbumWithArtists> albums, Runnable loadMoreCallback, OnAlbumClickListener listener) {
        this.albums = albums;
        this.loadMoreCallback = loadMoreCallback;
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return albums.get(position).getAlbum().getId();
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album_layout, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        AlbumWithArtists albumWithArtists = albums.get(position);
        Album album = albumWithArtists.getAlbum();
        List<Artist> artists = albumWithArtists.getArtists();

        // Title
        holder.tvTitle.setText(album.getTitle());

        List<String> artistNames = artists.stream().map(a -> a.displayName).toList();
        holder.tvArtists.setText(StringUtils.formatArtists(artistNames));

        // Year
        String yearString = album.getYear() == 0 ? "Unknown release date" : "Released: " + album.getYear();
        holder.tvYear.setText(yearString);

        // Format
        String formatString = "Format: " + album.getFormat().getDisplayName();
        holder.tvFormat.setText(formatString);

        if (album.getCover() != null) {
            Bitmap bm = ImageUtils.toBitmap(album.getCover());
            holder.ivCover.setImageBitmap(bm);
        }

        // Pagination trigger
        if (position == albums.size() - 1) {
            loadMoreCallback.run();
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAlbumClick(albumWithArtists);
            }
        });
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    public void updateList(List<AlbumWithArtists> newList) {
        albums.clear();
        albums.addAll(newList);
        notifyDataSetChanged();
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {

        ImageView ivCover;
        TextView tvTitle, tvArtists, tvYear, tvFormat;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);

            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtists = itemView.findViewById(R.id.tvArtists);
            tvYear = itemView.findViewById(R.id.tvYear);
            tvFormat = itemView.findViewById(R.id.tvFormat);
        }
    }

    public interface OnAlbumClickListener {
        void onAlbumClick(AlbumWithArtists album);
    }

}
