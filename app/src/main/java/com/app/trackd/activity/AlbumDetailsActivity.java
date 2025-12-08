package com.app.trackd.activity;

import android.graphics.Bitmap;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.app.trackd.R;
import com.app.trackd.database.AppDatabase;
import com.app.trackd.model.Album;
import com.app.trackd.model.AlbumWithArtists;
import com.app.trackd.util.ImageUtils;
import com.bumptech.glide.Glide;
import com.nambimobile.widgets.efab.ExpandableFab;
import com.nambimobile.widgets.efab.FabOption;

import java.util.List;

public class AlbumDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_ALBUM_ID_TO_ALBUM_DETAILS = "albumId";

    ImageView albumBanner, blurBackground;
    ImageView imgCover;

    TextView txtTitle;
    TextView txtYear;
    TextView txtFormat;

    LinearLayout artistListContainer;

    FabOption fabEdit;
    FabOption fabDelete;
    ExpandableFab mainFab;

    AppDatabase db;
    long albumId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_details);

        db = AppDatabase.get(this);
        albumId = getIntent().getLongExtra(EXTRA_ALBUM_ID_TO_ALBUM_DETAILS, -1);

        if (albumId == -1) {
            finish();
            return;
        }

        initViews();
        loadAlbum();
        setupEditButton();
    }

    private void initViews() {

        // Banner at the top
        albumBanner = findViewById(R.id.albumBanner);
        blurBackground = findViewById(R.id.blurBackground);

        // Main cover card image
        imgCover = findViewById(R.id.albumCover);

        // Title + year + format
        txtTitle = findViewById(R.id.albumTitle);
        txtYear = findViewById(R.id.albumYear);
        txtFormat = findViewById(R.id.albumFormat);

        // Artist list container (dynamic)
        artistListContainer = findViewById(R.id.artistListContainer);

        // FAB options
        fabEdit = findViewById(R.id.fabEditAlbum);
        fabDelete = findViewById(R.id.fabDeleteAlbum);
        mainFab = findViewById(R.id.mainFab);
    }

    private void loadAlbum() {
        new Thread(() -> {
            AlbumWithArtists data = db.albumDao().getAlbumWithArtistsById(albumId);

            runOnUiThread(() -> {
                if (data == null) {
                    finish();
                    return;
                }

                Album album = data.getAlbum();

                // Cover: change to your own local image loading
                if (album.getCover() != null) {
                    Bitmap coverBitmap = ImageUtils.toBitmap(album.getCover());
                    Glide.with(this)
                            .load(coverBitmap)
                            .placeholder(R.drawable.ic_gallery)
                            .centerCrop()
                            .into(imgCover);
                    applyBlurredBackground(coverBitmap);
                }

                txtYear.setText(album.getYear() + " • " + album.getFormat());
                txtTitle.setText(album.getTitle());

                List<String> artists = data.getArtists().stream()
                        .map(a -> a.getDisplayName())
                        .toList();
                populateArtistList(artists);
            });
        }).start();
    }

    private void setupEditButton() {
//        btnEdit.setOnClickListener(v -> {
//            Intent i = new Intent(this, AlbumEditActivity.class);
//            i.putExtra(AlbumEditActivity.EXTRA_ALBUM_ID, albumId);
//            startActivity(i);
//        });
    }

    private void populateArtistList(List<String> artistNames) {
        artistListContainer.removeAllViews();

        for (String name : artistNames) {
            TextView tv = new TextView(this);
            tv.setText(name);
            tv.setTextSize(14);
            tv.setPadding(0, 6, 0, 6);
            tv.setTextColor(getColor(android.R.color.black));
            tv.setMaxLines(Integer.MAX_VALUE); // make sure long names don't get ellipsized

            artistListContainer.addView(tv);
        }
    }

    private void applyBlurredBackground(Bitmap albumCover) {
        if (albumCover == null) return;

        blurBackground.setImageBitmap(albumCover);
        blurBackground.setRenderEffect(RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP));
    }

    // Super simple fast blur (box blur)
    private Bitmap fastBlur(Bitmap sentBitmap) {
        Bitmap bitmap = Bitmap.createBitmap(sentBitmap);
        final int width = sentBitmap.getWidth();
        final int height = sentBitmap.getHeight();

        int[] pixels = new int[width * height];
        sentBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 1; i < pixels.length - 1; i++) {
            pixels[i] = (pixels[i - 1] + pixels[i] + pixels[i + 1]) / 3;
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
}
