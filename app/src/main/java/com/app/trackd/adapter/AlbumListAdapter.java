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
import com.app.trackd.model.enums.AlbumFormat;
import com.app.trackd.util.ImageUtils;
import com.app.trackd.util.StringUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class AlbumListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public enum ViewMode {
        LIST,
        GRID
    }

    private static final int VIEW_TYPE_LIST = 0;
    private static final int VIEW_TYPE_GRID = 1;
    private static final int VIEW_TYPE_GRID_SECTION_HEADER = 2;
    private static final List<AlbumFormat> GRID_SECTION_ORDER = List.of(
            AlbumFormat.values()
    );

    private final List<AlbumWithArtists> albums;
    private final OnAlbumClickListener listener;
    private final List<GridItem> gridItems = new ArrayList<>();
    private final Map<AlbumFormat, Integer> sectionCounts = new EnumMap<>(AlbumFormat.class);
    private boolean sectionHeadersEnabled = true;
    private ViewMode viewMode = ViewMode.LIST;

    public AlbumListAdapter(List<AlbumWithArtists> albums, OnAlbumClickListener listener) {
        this.albums = albums;
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        if (viewMode == ViewMode.GRID) {
            return gridItems.get(position).stableId;
        }
        return albums.get(position).getAlbum().getId();
    }

    @Override
    public int getItemViewType(int position) {
        if (viewMode == ViewMode.LIST) return VIEW_TYPE_LIST;
        return gridItems.get(position).isHeader
                ? VIEW_TYPE_GRID_SECTION_HEADER
                : VIEW_TYPE_GRID;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes;
        if (viewType == VIEW_TYPE_GRID) {
            layoutRes = R.layout.item_album_grid;
        } else if (viewType == VIEW_TYPE_GRID_SECTION_HEADER) {
            layoutRes = R.layout.item_album_section_header;
        } else {
            layoutRes = R.layout.item_album_layout;
        }

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutRes, parent, false);
        if (viewType == VIEW_TYPE_GRID) {
            return new AlbumGridViewHolder(view);
        }
        if (viewType == VIEW_TYPE_GRID_SECTION_HEADER) {
            return new AlbumGridSectionHeaderViewHolder(view);
        }
        return new AlbumListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (viewMode == ViewMode.GRID && holder instanceof AlbumGridSectionHeaderViewHolder) {
            AlbumGridSectionHeaderViewHolder headerHolder = (AlbumGridSectionHeaderViewHolder) holder;
            headerHolder.textSectionHeader.setText(gridItems.get(position).headerText);
            holder.itemView.setOnClickListener(null);
            return;
        }

        AlbumWithArtists albumWithArtists = viewMode == ViewMode.GRID
                ? gridItems.get(position).album
                : albums.get(position);
        Album album = albumWithArtists.getAlbum();
        List<Artist> artists = albumWithArtists.getArtists();
        List<String> artistNames = artists.stream().map(a -> a.displayName).toList();
        String artistsText = StringUtils.formatArtists(artistNames);
        Bitmap coverBitmap = ImageUtils.toBitmap(holder.itemView.getContext(), album.getCover());

        if (holder instanceof AlbumListViewHolder) {
            AlbumListViewHolder listHolder = (AlbumListViewHolder) holder;
            bindListItem(listHolder, album, artistsText, coverBitmap);
        } else if (holder instanceof AlbumGridViewHolder) {
            AlbumGridViewHolder gridHolder = (AlbumGridViewHolder) holder;
            bindGridItem(gridHolder, album, coverBitmap);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && albumWithArtists != null) {
                listener.onAlbumClick(albumWithArtists);
            }
        });
    }

    @Override
    public int getItemCount() {
        return viewMode == ViewMode.GRID ? gridItems.size() : albums.size();
    }

    public void updateList(List<AlbumWithArtists> newList) {
        albums.clear();
        albums.addAll(newList);
        notifyAlbumsChanged();
    }

    public void notifyAlbumsChanged() {
        rebuildGridItems();
        notifyDataSetChanged();
    }

    public void setSectionCounts(@NonNull Map<AlbumFormat, Integer> countsByFormat) {
        sectionCounts.clear();
        sectionCounts.putAll(countsByFormat);
        if (viewMode == ViewMode.GRID) {
            notifyAlbumsChanged();
        }
    }

    public void setSectionHeadersEnabled(boolean enabled) {
        if (sectionHeadersEnabled == enabled) return;
        sectionHeadersEnabled = enabled;
        if (viewMode == ViewMode.GRID) {
            notifyAlbumsChanged();
        }
    }

    public boolean isSectionHeaderPosition(int position) {
        if (viewMode != ViewMode.GRID || position < 0 || position >= gridItems.size()) return false;
        return gridItems.get(position).isHeader;
    }

    public AlbumFormat getGridItemFormatAt(int position) {
        if (viewMode != ViewMode.GRID || position < 0 || position >= gridItems.size()) return null;
        GridItem item = gridItems.get(position);
        if (item.isHeader || item.album == null || item.album.getAlbum() == null) return null;
        return item.album.getAlbum().getFormat();
    }

    public ViewMode getViewMode() {
        return viewMode;
    }

    public void setViewMode(ViewMode viewMode) {
        if (this.viewMode == viewMode) return;
        this.viewMode = viewMode;
        notifyAlbumsChanged();
    }

    private void rebuildGridItems() {
        gridItems.clear();
        if (viewMode != ViewMode.GRID) return;

        if (!sectionHeadersEnabled) {
            for (AlbumWithArtists album : albums) {
                gridItems.add(GridItem.album(album));
            }
            return;
        }

        Map<AlbumFormat, List<AlbumWithArtists>> grouped = new EnumMap<>(AlbumFormat.class);
        List<AlbumWithArtists> unknownFormats = new ArrayList<>();
        for (AlbumWithArtists item : albums) {
            AlbumFormat format = item.getAlbum().getFormat();
            if (format == null) {
                unknownFormats.add(item);
                continue;
            }
            grouped.computeIfAbsent(format, ignored -> new ArrayList<>()).add(item);
        }

        for (AlbumFormat format : GRID_SECTION_ORDER) {
            List<AlbumWithArtists> sectionAlbums = grouped.remove(format);
            appendSection(format, sectionAlbums);
        }

        if (!grouped.isEmpty()) {
            for (Map.Entry<AlbumFormat, List<AlbumWithArtists>> entry : grouped.entrySet()) {
                appendSection(entry.getKey(), entry.getValue());
            }
        }

        if (!unknownFormats.isEmpty()) {
            appendUnknownSection(unknownFormats);
        }
    }

    private void appendSection(@NonNull AlbumFormat format, List<AlbumWithArtists> sectionAlbums) {
        if (sectionAlbums == null || sectionAlbums.isEmpty()) return;
        int sectionCount = sectionCounts.getOrDefault(format, sectionAlbums.size());
        if (sectionCount <= 0) return;

        gridItems.add(GridItem.header(
                sectionStableId(format.name()),
                toSectionHeaderLabel(format, sectionCount)
        ));
        for (AlbumWithArtists albumWithArtists : sectionAlbums) {
            gridItems.add(GridItem.album(albumWithArtists));
        }
    }

    private void appendUnknownSection(@NonNull List<AlbumWithArtists> sectionAlbums) {
        int sectionCount = sectionAlbums.size();
        if (sectionCount <= 0) return;

        gridItems.add(GridItem.header(
                sectionStableId("UNKNOWN"),
                "Other \u00B7 " + sectionCount
        ));
        for (AlbumWithArtists albumWithArtists : sectionAlbums) {
            gridItems.add(GridItem.album(albumWithArtists));
        }
    }

    private long sectionStableId(@NonNull String sectionKey) {
        return -1_000_000L - Math.abs(sectionKey.hashCode());
    }

    private String toSectionHeaderLabel(@NonNull AlbumFormat format, int count) {
        String prefix;
        switch (format) {
            case VINYL:
                prefix = "LP 12\"";
                break;
            case VINYL_10:
                prefix = "LP 10\"";
                break;
            case VINYL_7:
                prefix = "LP 7\"";
                break;
            case CD:
                prefix = "CD";
                break;
            case CASSETTE:
                prefix = "DVD";
                break;
            default:
                prefix = toShortReadableLabel(format.getDisplayName());
                break;
        }
        return prefix + " \u00B7 " + count;
    }

    private void bindListItem(
            @NonNull AlbumListViewHolder holder,
            @NonNull Album album,
            @NonNull String artistsText,
            @NonNull Bitmap coverBitmap
    ) {
        holder.tvTitle.setText(album.getTitle());
        holder.tvArtists.setText(artistsText);

        String yearString = album.getYear() == 0
                ? "Unknown release date"
                : "Released: " + album.getYear();
        holder.tvYear.setText(yearString);

        String formatString = album.getFormat() == null
                ? "Format: Unknown"
                : "Format: " + album.getFormat().getDisplayName();
        holder.tvFormat.setText(formatString);
        holder.ivCover.setImageBitmap(coverBitmap);
    }

    private void bindGridItem(
            @NonNull AlbumGridViewHolder holder,
            @NonNull Album album,
            @NonNull Bitmap coverBitmap
    ) {
        holder.imageCover.setImageBitmap(coverBitmap);
        holder.textTitle.setText(album.getTitle());
        holder.textFormat.setText(toFormatBadge(album.getFormat()));
    }

    private String toFormatBadge(AlbumFormat format) {
        if (format == null) return "--";

        switch (format) {
            case VINYL:
                return "12\"";
            case VINYL_10:
                return "10\"";
            case VINYL_7:
                return "7\"";
            case CD:
                return "CD";
            case CASSETTE:
                return "DVD";
            default:
                return toShortReadableLabel(format.getDisplayName());
        }
    }

    private String toShortReadableLabel(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) return "--";

        String label = displayName.trim();
        if ("SACD".equalsIgnoreCase(label)) return "SACD";
        if (label.length() <= 10) return label;

        int firstSpace = label.indexOf(' ');
        return firstSpace > 0 ? label.substring(0, firstSpace) : label.substring(0, 10);
    }

    public interface OnAlbumClickListener {
        void onAlbumClick(AlbumWithArtists album);
    }

    private static class GridItem {
        final boolean isHeader;
        final String headerText;
        final AlbumWithArtists album;
        final long stableId;

        private GridItem(boolean isHeader, String headerText, AlbumWithArtists album, long stableId) {
            this.isHeader = isHeader;
            this.headerText = headerText;
            this.album = album;
            this.stableId = stableId;
        }

        static GridItem header(long stableId, @NonNull String title) {
            return new GridItem(true, title, null, stableId);
        }

        static GridItem album(@NonNull AlbumWithArtists album) {
            return new GridItem(false, null, album, album.getAlbum().getId());
        }
    }

    static class AlbumListViewHolder extends RecyclerView.ViewHolder {

        ImageView ivCover;
        TextView tvTitle, tvArtists, tvYear, tvFormat;

        public AlbumListViewHolder(@NonNull View itemView) {
            super(itemView);

            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtists = itemView.findViewById(R.id.tvArtists);
            tvYear = itemView.findViewById(R.id.tvYear);
            tvFormat = itemView.findViewById(R.id.tvFormat);
        }
    }

    static class AlbumGridViewHolder extends RecyclerView.ViewHolder {

        ImageView imageCover;
        TextView textTitle;
        TextView textFormat;

        public AlbumGridViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCover = itemView.findViewById(R.id.imageCover);
            textTitle = itemView.findViewById(R.id.textTitle);
            textFormat = itemView.findViewById(R.id.textFormat);
        }
    }

    static class AlbumGridSectionHeaderViewHolder extends RecyclerView.ViewHolder {

        TextView textSectionHeader;

        public AlbumGridSectionHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textSectionHeader = itemView.findViewById(R.id.textSectionHeader);
        }
    }

}
