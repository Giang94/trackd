package com.app.trackd.activity;

import static com.app.trackd.activity.EditAlbumActivity.EXTRA_ALBUM_ID;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
        adapter = new RecentAlbumListAdapter(albums, this::openAlbumDetails);

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

    private final ActivityResultLauncher<Intent> editAlbumLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK &&
                                result.getData() != null) {

                            long id = result.getData().getLongExtra(EditAlbumActivity.EXTRA_UPDATED_ALBUM_ID, -1);
                            if (id != -1) updateSingleAlbum(id);
                        }
                    });

    private void updateSingleAlbum(long albumId) {
        new Thread(() -> {
            AlbumWithArtists updated =
                    db.albumDao().getAlbumWithArtistsById(albumId);

            int index = findAlbumIndex(albumId);
            if (index == -1) return;

            runOnUiThread(() -> {
                albums.set(index, updated);
                adapter.notifyItemChanged(index);
            });
        }).start();
    }

    private int findAlbumIndex(long albumId) {
        for (int i = 0; i < albums.size(); i++) {
            if (albums.get(i).getAlbum().getId() == albumId) {
                return i;
            }
        }
        return -1;
    }

    private void openAlbumDetails(AlbumWithArtists album) {
        AlbumDetailBottomSheet sheet = new AlbumDetailBottomSheet(album);

        sheet.setOnAlbumEditListener(albumId -> {
            Intent intent = new Intent(this, EditAlbumActivity.class);
            intent.putExtra(EXTRA_ALBUM_ID, albumId);
            editAlbumLauncher.launch(intent);
        });
        sheet.setOnAlbumDeletedListener(albumId -> {
            refreshData();
        });

        sheet.show(getSupportFragmentManager(), "album_detail_sheet");
    }
}
