package com.app.trackd.fragment;

import static com.app.trackd.util.SizeUtils.dpToPx;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.app.trackd.R;
import com.app.trackd.activity.TaggingActivity;
import com.app.trackd.database.AppDatabase;
import com.app.trackd.model.Album;
import com.app.trackd.model.AlbumWithArtists;
import com.app.trackd.model.Artist;
import com.app.trackd.model.Tag;
import com.app.trackd.util.ImageUtils;
import com.app.trackd.util.SpotifyUrlHelper;
import com.app.trackd.util.StringUtils;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

import java.util.List;
import java.util.Random;

public class AlbumDetailBottomSheet extends BottomSheetDialogFragment {

    private static final float MAX_BOTTOM_SHEET_HEIGHT = 0.9f;

    private final AlbumWithArtists albumWithArtists;
    private int lastColor = -1;

    // Views
    private ImageView albumCover;
    private NestedScrollView scrollView;
    private FrameLayout overlay;
    private LinearLayout buttonGroup;
    private TextView albumTitle, albumYear, albumFormat;
    private LinearLayout artistListContainer, openInContainer;
    private ImageButton btnOpenSpotify, btnDelete, btnEdit, btnTag;
    private FlexboxLayout chipGroupTags;

    // Listeners
    private OnAlbumDeletedListener deleteListener;
    private OnAlbumEditListener editListener;

    public AlbumDetailBottomSheet(AlbumWithArtists albumWithArtists) {
        this.albumWithArtists = albumWithArtists;
    }

    /* ---------------- Lifecycle ---------------- */

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);

        View view = inflater.inflate(R.layout.fragment_album_detail, container, false);

        bindViews(view);
        setupOverlayGesture();
        setupButtons();

        populateAlbumData();
        loadTags();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        configureBottomSheet();
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }

    /* ---------------- View binding & setup ---------------- */

    private void bindViews(View view) {
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

        scrollView = view.findViewById(R.id.scrollView);
        overlay = view.findViewById(R.id.actionOverlay);
        buttonGroup = overlay.findViewById(R.id.buttonGroup);
    }

    private void setupOverlayGesture() {
        GestureDetector detector = new GestureDetector(
                requireContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public void onLongPress(MotionEvent e) {
                        showOverlay();
                    }
                }
        );

        scrollView.setOnTouchListener((v, event) -> {
            detector.onTouchEvent(event);
            return false;
        });

        overlay.setOnClickListener(v -> hideOverlay());
    }

    /* ---------------- Bottom sheet config ---------------- */

    private void configureBottomSheet() {
        View sheet = getDialog().findViewById(
                com.google.android.material.R.id.design_bottom_sheet
        );

        if (sheet == null) return;

        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);
        behavior.setFitToContents(false);
        behavior.setExpandedOffset(dpToPx(requireContext(), 24));

        ViewGroup.LayoutParams params = sheet.getLayoutParams();
        params.height = (int) (getResources().getDisplayMetrics().heightPixels * MAX_BOTTOM_SHEET_HEIGHT);
        sheet.setLayoutParams(params);
    }

    /* ---------------- Overlay animations ---------------- */

    private void showOverlay() {
        overlay.setVisibility(View.VISIBLE);
        overlay.setAlpha(0f);

        overlay.animate()
                .alpha(1f)
                .setDuration(200)
                .start();

        animateOverlayButtonsIn();
    }

    private void hideOverlay() {
        animateOverlayButtonsOut();

        overlay.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction(() -> overlay.setVisibility(View.GONE))
                .start();
    }

    private void animateOverlayButtonsIn() {
        for (int i = 0; i < buttonGroup.getChildCount(); i++) {
            View child = buttonGroup.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(20f);

            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(i * 40L)
                    .setDuration(500)
                    .start();
        }
    }

    private void animateOverlayButtonsOut() {
        for (int i = 0; i < buttonGroup.getChildCount(); i++) {
            View child = buttonGroup.getChildAt(i);
            child.animate()
                    .alpha(0f)
                    .translationY(20f)
                    .setDuration(120)
                    .start();
        }
    }

    /* ---------------- Button actions ---------------- */

    private void setupButtons() {
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnEdit.setOnClickListener(v -> editAlbum());
        btnTag.setOnClickListener(v -> openTagging());
    }

    private void confirmDelete() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete album")
                .setMessage("This action cannot be undone. Are you sure?")
                .setPositiveButton("Delete", (dialog, which) -> deleteAlbum())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAlbum() {
        new Thread(() -> {
            long id = albumWithArtists.getAlbum().getId();
            AppDatabase db = AppDatabase.get(requireContext());

            db.albumDao().deleteArtistLinks(id);
            db.albumDao().delete(albumWithArtists.getAlbum());

            if (deleteListener != null) {
                requireActivity().runOnUiThread(() ->
                        deleteListener.onAlbumDeleted(id)
                );
            }

            requireActivity().runOnUiThread(this::dismiss);
        }).start();
    }

    private void editAlbum() {
        if (editListener != null) {
            editListener.onEditRequested(albumWithArtists.getAlbum().getId());
            dismiss();
        }
    }

    private void openTagging() {
        Intent i = new Intent(getContext(), TaggingActivity.class);
        i.putExtra(TaggingActivity.EXTRA_ALBUM_ID, albumWithArtists.getAlbum().getId());
        startActivity(i);
        dismiss();
    }

    /* ---------------- Data binding ---------------- */

    private void populateAlbumData() {
        Album album = albumWithArtists.getAlbum();

        setCover(album);
        setMeta(album);
        setArtists();
        setupSpotify(album);
    }

    private void setCover(Album album) {
        if (album.getCover() == null) return;
        Bitmap bitmap = ImageUtils.toBitmap(getContext(), album.getCover());
        albumCover.setImageBitmap(bitmap);
    }

    private void setMeta(Album album) {
        albumTitle.setText(album.getTitle());
        StringUtils.balanceText(albumTitle);

        albumYear.setText(
                album.getYear() == 0
                        ? "Unknown release date"
                        : "Released: " + album.getYear()
        );

        albumFormat.setText(
                "Format: " + album.getFormat().getDisplayName()
        );
    }

    private void setArtists() {
        artistListContainer.removeAllViews();

        albumWithArtists.getArtists().stream()
                .map(Artist::getDisplayName)
                .forEach(name -> {
                    TextView tv = new TextView(getContext());
                    tv.setText(name);
                    tv.setTextSize(16);
                    tv.setPadding(0, 6, 0, 6);
                    tv.setGravity(Gravity.CENTER);
                    artistListContainer.addView(tv);
                });
    }

    private void setupSpotify(Album album) {
        if (album.getSpotifyUrl() == null || album.getSpotifyUrl().isEmpty()) {
            openInContainer.setVisibility(View.GONE);
            btnOpenSpotify.setVisibility(View.GONE);
            return;
        }

        openInContainer.setVisibility(View.VISIBLE);
        btnOpenSpotify.setVisibility(View.VISIBLE);

        String url = SpotifyUrlHelper.toFullUrl(album.getSpotifyUrl());

        btnOpenSpotify.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage("com.spotify.music");

            if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });
    }

    private void loadTags() {
        long albumId = albumWithArtists.getAlbum().getId();
        List<Tag> tags = AppDatabase.get(getContext())
                .tagDao()
                .getTagsForAlbum(albumId);

        if (tags != null && !tags.isEmpty()) {
            tags.forEach(this::addChip);
        }
    }

    private void addChip(Tag tag) {
        Chip chip = new Chip(getContext());
        chip.setText(tag.getName());
        chip.setCheckable(false);
        chip.setClickable(false);
        chip.setTag(tag.getId());
        chip.setCloseIconVisible(false);
        chip.setTag(R.id.delete_mode, false);
        chip.setChipBackgroundColor(
                ColorStateList.valueOf(getRandomChipColor())
        );
        chip.setTextColor(Color.WHITE);

        FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
        );

        int margin = dpToPx(getContext(), 4);
        lp.setMargins(margin, 0, margin, 0);
        chip.setLayoutParams(lp);

        chipGroupTags.addView(chip);
    }

    private int getRandomChipColor() {
        int[] colors = {
                R.color.chip_red,
                R.color.chip_blue,
                R.color.chip_green,
                R.color.chip_yellow
        };

        int index;
        do {
            index = new Random().nextInt(colors.length);
        } while (index == lastColor);

        lastColor = index;
        return ContextCompat.getColor(requireContext(), colors[index]);
    }

    /* ---------------- Callbacks ---------------- */

    public void setOnAlbumDeletedListener(OnAlbumDeletedListener listener) {
        this.deleteListener = listener;
    }

    public void setOnAlbumEditListener(OnAlbumEditListener listener) {
        this.editListener = listener;
    }

    public interface OnAlbumDeletedListener {
        void onAlbumDeleted(long albumId);
    }

    public interface OnAlbumEditListener {
        void onEditRequested(long albumId);
    }
}
