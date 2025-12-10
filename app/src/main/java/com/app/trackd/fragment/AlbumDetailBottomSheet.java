package com.app.trackd.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.app.trackd.R;
import com.app.trackd.activity.MainActivity;
import com.app.trackd.activity.TaggingActivity;
import com.app.trackd.database.AppDatabase;
import com.app.trackd.model.Album;
import com.app.trackd.model.AlbumWithArtists;
import com.app.trackd.model.Artist;
import com.app.trackd.model.Tag;
import com.app.trackd.util.ImageUtils;
import com.app.trackd.util.StringUtils;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

import java.util.List;
import java.util.Random;

public class AlbumDetailBottomSheet extends BottomSheetDialogFragment {

    private ImageView albumCover;
    private TextView albumTitle, albumYear, albumFormat;
    private LinearLayout artistListContainer, openInContainer;
    private ImageButton btnOpenSpotify, btnDelete, btnEdit, btnTag;
    private FlexboxLayout chipGroupTags;

    private final AlbumWithArtists albumWithArtists;

    public AlbumDetailBottomSheet(AlbumWithArtists albumWithArtists) {
        this.albumWithArtists = albumWithArtists;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album_detail, container, false);

        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);

        albumCover = view.findViewById(R.id.albumCover);
        albumTitle = view.findViewById(R.id.albumTitle);
        albumYear = view.findViewById(R.id.albumYear);
        albumFormat = view.findViewById(R.id.albumFormat);
        artistListContainer = view.findViewById(R.id.artistListContainer);
        openInContainer = view.findViewById(R.id.openInContainer);
        btnOpenSpotify = view.findViewById(R.id.btnOpenSpotify);
        btnEdit = view.findViewById(R.id.btnEdit);
        btnDelete = view.findViewById(R.id.btnDelete);
        btnTag = view.findViewById(R.id.btnTag);
        chipGroupTags = view.findViewById(R.id.chipGroupTags);

        btnDelete.setOnClickListener(v -> {
            new Thread(() -> {
                long id = albumWithArtists.getAlbum().getId();

                // delete album + cross-table links
                AppDatabase db = AppDatabase.get(requireContext());
                db.albumDao().deleteArtistLinks(id);
                db.albumDao().delete(albumWithArtists.getAlbum());

                // notify parent
                if (deleteListener != null) {
                    requireActivity().runOnUiThread(() -> deleteListener.onAlbumDeleted(id));
                }

                // close bottom sheet
                dismiss();

            }).start();
        });

        btnEdit.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onEditRequested(albumWithArtists.getAlbum().getId());
            }
            dismiss();
        });

        btnTag.setOnClickListener(v -> {
            Intent i = new Intent(this.getContext(), TaggingActivity.class);
            i.putExtra(TaggingActivity.EXTRA_ALBUM_ID, albumWithArtists.getAlbum().getId());
            startActivity(i);
        });

        populateAlbumData();
        loadTags();
        return view;
    }

    private void loadTags() {
        long albumId = albumWithArtists.getAlbum().getId();

        AppDatabase db = AppDatabase.get(this.getContext());
        List<Tag> tags = db.tagDao().getTagsForAlbum(albumId);
        if (tags != null && tags.size() > 0) {
            tags.forEach(this::addChip);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

            bottomSheet.post(() -> {
                int maxHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.75f);

                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                bottomSheet.requestLayout();

                behavior.setPeekHeight(maxHeight, true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            });
        }
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }

    private void populateAlbumData() {
        Album album = albumWithArtists.getAlbum();

        if (album.getCover() != null) {
            Bitmap coverBitmap = ImageUtils.toBitmap(album.getCover());
            albumCover.setImageBitmap(coverBitmap);
        }

        String yearString = album.getYear() == 0 ? "Unknown release date" : "Released: " + album.getYear();
        albumYear.setText(yearString);

        String formatString = "Format: " + album.getFormat().getDisplayName();
        albumFormat.setText(formatString);

        albumTitle.setText(album.getTitle());
        StringUtils.balanceText(albumTitle);

        List<String> artists = albumWithArtists.getArtists().stream()
                .map(Artist::getDisplayName)
                .toList();

        artistListContainer.removeAllViews();
        for (String name : artists) {
            TextView tv = new TextView(getContext());
            tv.setText(name);
            tv.setTextSize(16);
            tv.setPadding(0, 6, 0, 6);
            tv.setGravity(Gravity.CENTER);
            tv.setTextColor(Color.BLACK);
            artistListContainer.addView(tv);
        }

        if (album.getSpotifyUrl() != null && !album.getSpotifyUrl().isEmpty()) {
            openInContainer.setVisibility(View.VISIBLE);
            btnOpenSpotify.setVisibility(View.VISIBLE);
            String spotifyUrl = album.getSpotifyUrl();

            btnOpenSpotify.setOnClickListener(view -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUrl));
                intent.setPackage("com.spotify.music");

                // Fallback to browser if Spotify app not installed
                if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    // open in browser
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUrl)));
                }
            });
        } else {
            openInContainer.setVisibility(View.GONE);
            btnOpenSpotify.setVisibility(View.GONE);
        }
    }

    public interface OnAlbumDeletedListener {
        void onAlbumDeleted(long albumId);
    }

    private OnAlbumDeletedListener deleteListener;

    public void setOnAlbumDeletedListener(OnAlbumDeletedListener listener) {
        this.deleteListener = listener;
    }

    public interface OnAlbumEditListener {
        void onEditRequested(long albumId);
    }

    private OnAlbumEditListener editListener;

    public void setOnAlbumEditListener(OnAlbumEditListener listener) {
        this.editListener = listener;
    }

    private Chip addChip(Tag tag) {
        Chip chip = new Chip(this.getContext());
        chip.setText(tag.getName());
        chip.setCheckable(true);
        chip.setTag(tag.getId());
        chip.setCloseIconVisible(false);
        chip.setTag(R.id.delete_mode, false);
        chip.setChipBackgroundColor(ColorStateList.valueOf(getRandomChipColor()));
        chip.setTextColor(Color.WHITE);

        FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT);
        int margin = dpToPx(4);
        lp.setMargins(margin, 0, margin, 0);
        chip.setLayoutParams(lp);
        chipGroupTags.addView(chip);
        return chip;
    }

    private int dpToPx(int dp) {
        float scale = getResources().getDisplayMetrics().density;
        return Math.round(dp * scale);
    }

    private int getRandomChipColor() {
        int[] chipColors = new int[]{
                R.color.chip_red,
                R.color.chip_blue,
                R.color.chip_green,
                R.color.chip_yellow
        };
        int index = new Random().nextInt(chipColors.length);
        return ContextCompat.getColor(this.getContext(), chipColors[index]);
    }
}
