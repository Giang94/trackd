package com.app.trackd.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.trackd.R;
import com.app.trackd.model.Album;
import com.app.trackd.util.ImageUtils;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecentAdapter extends RecyclerView.Adapter<RecentAdapter.VH> {

    public interface OnItemClick {
        void onClick(Album album);
    }

    private List<Album> items;
    private OnItemClick listener;

    public RecentAdapter(List<Album> items, OnItemClick listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Album a = items.get(position);
        holder.tvTitle.setText(a.getArtist() + " – " + a.getTitle());
        holder.tvSubtitle.setText(a.getFormat().name()+ " | " + a.getYear());
        holder.ivAlbumCover.setImageBitmap(ImageUtils.toBitmap(a.getCover()));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(a);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle;
        ImageView ivAlbumCover;
        VH(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitleArtist);
            tvSubtitle = v.findViewById(R.id.tvSubtitle);
            ivAlbumCover = v.findViewById(R.id.ivAlbumCover);
        }
    }

    public void updateData(List<Album> newList) {
        this.items = newList;
        notifyDataSetChanged();
    }
}
