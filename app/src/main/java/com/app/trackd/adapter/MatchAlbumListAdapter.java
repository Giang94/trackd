package com.app.trackd.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
                .inflate(R.layout.item_recent_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecentItemViewHolder holder, int position) {
        Album album = albums.get(position);
        holder.tvTitle.setText(album.getTitle());

        String subtitleStr = album.getFormat().getDisplayName() + " • " + album.getYear();
        holder.tvSubtitle.setText(subtitleStr);
        holder.ivAlbumCover.setImageBitmap(ImageUtils.toBitmap(album.getCover()));
        holder.ivAlbumCover.setVisibility(View.VISIBLE);
        holder.glFourContainer.setVisibility(View.GONE);

        db = AppDatabase.get(holder.itemView.getContext());
        List<Artist> artists = db.albumArtistDao().getArtistsForAlbum(album.getId());
        List<String> artistNames = artists.stream().map(a -> a.displayName).toList();
        holder.itemView.post(() -> holder.tvArtist.setText(StringUtils.formatArtists(artistNames)));
    }

    @Override
    public int getItemCount() {
        return albums.size();
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

    public void updateData(List<Album> newList) {
        this.albums = newList;
        notifyDataSetChanged();
    }
}
