package com.app.trackd.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.trackd.R;
import com.app.trackd.adapter.RecentAlbumListAdapter;
import com.app.trackd.common.TwoFingerDoubleTapHelper;
import com.app.trackd.common.TwoFingerZoomHelper;
import com.app.trackd.dao.ITagDao;
import com.app.trackd.database.AppDatabase;
import com.app.trackd.fragment.AlbumDetailBottomSheet;
import com.app.trackd.model.Album;
import com.app.trackd.model.AlbumWithArtists;
import com.app.trackd.model.Tag;
import com.app.trackd.util.StringUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends FragmentActivity {

    private MaterialCardView cvTotal;
    private TextView tvTotalItems, tvVinyl, tvCds;
    private ImageView ivProfile;
    private RecyclerView rvRecent;
    private FloatingActionButton fabAddAlbum;
    private RecentAlbumListAdapter adapter;
    private List<AlbumWithArtists> albums;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.get(this);
        seedTags(db, List.of("Autographed", "Boxset", "Limited Edition", "Deluxe"));

        TwoFingerZoomHelper.enableTwoFingerZoom(this);
        TwoFingerDoubleTapHelper.enableTwoFingerDoubleTap(this);

        initViews();
        setupData();
    }

    public static void seedTags(AppDatabase db, List<String> tagsToSeed) {
        ITagDao tagDao = db.tagDao();

        new Thread(() -> {
            for (String rawName : tagsToSeed) {
                String normalized = StringUtils.normalize(rawName);

                // Skip if exists
                if (tagDao.findByNormalized(normalized) == null) {
                    tagDao.insert(new Tag(rawName));
                }
            }
        }).start();
    }

    private void initViews() {
        cvTotal = findViewById(R.id.cvTotal);
        tvTotalItems = findViewById(R.id.tvTotalItems);
        tvVinyl = findViewById(R.id.tvVinyl);
        tvCds = findViewById(R.id.tvCds);
        rvRecent = findViewById(R.id.rvRecent);
        ivProfile = findViewById(R.id.ivProfile);
        fabAddAlbum = findViewById(R.id.fabAddAlbum);

        cvTotal.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, AlbumListActivity.class);
            startActivity(i);
        });

        fabAddAlbum.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, TaggingActivity.class);
            i.putExtra(TaggingActivity.EXTRA_ALBUM_ID, 1L);
            startActivity(i);
        });
    }

    private void setupData() {
        List<Album> recentAlbums = db.albumDao().getRecentAlbums(7);
        albums = db.albumDao()
                .getAlbumsWithArtistsByIds(recentAlbums.stream().map(Album::getId).toList());
        setupRecycler();
        updateStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }

    private void refreshData() {
        List<Album> recentAlbums = db.albumDao().getRecentAlbums(7);
        albums = db.albumDao()
                .getAlbumsWithArtistsByIds(recentAlbums.stream().map(Album::getId).toList());
        adapter.updateData(albums);
        updateStats();
    }

    private void setupRecycler() {
        int fullCount = albums.size();

        adapter = new RecentAlbumListAdapter(albums, fullCount, this::openAlbumDetails);

        rvRecent.setLayoutManager(new GridLayoutManager(this, 2));
        rvRecent.setAdapter(adapter);

        adapter.setShowAllCallback(() -> {
            Intent i = new Intent(MainActivity.this, AlbumListActivity.class);
            startActivity(i);
        });
    }

    private void updateStats() {
        int total = db.albumDao().getAlbumCount();
        int vinyl = db.albumDao().getAlbumCountByFormat("VINYL");
        int cds = db.albumDao().getAlbumCountByFormat("CD");
        tvTotalItems.setText(String.valueOf(total));
        tvVinyl.setText(String.valueOf(vinyl));
        tvCds.setText(String.valueOf(cds));
    }

    private void openAlbumDetails(AlbumWithArtists album) {
        AlbumDetailBottomSheet sheet = new AlbumDetailBottomSheet(album);
        sheet.show(getSupportFragmentManager(), "album_detail_sheet");
        sheet.setOnAlbumDeletedListener(albumId -> {
            refreshData();
        });
    }
}
