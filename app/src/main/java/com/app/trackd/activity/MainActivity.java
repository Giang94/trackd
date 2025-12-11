package com.app.trackd.activity;

import static com.app.trackd.activity.AlbumListActivity.EXTRA_FILTER_CDS;
import static com.app.trackd.activity.AlbumListActivity.EXTRA_FILTER_VINYL;
import static com.app.trackd.activity.EditAlbumActivity.EXTRA_ALBUM_ID;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

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
import com.app.trackd.database.DatabaseHelper;
import com.app.trackd.fragment.AlbumDetailBottomSheet;
import com.app.trackd.model.Album;
import com.app.trackd.model.AlbumWithArtists;
import com.app.trackd.model.Tag;
import com.app.trackd.util.StringUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends FragmentActivity {

    View cardTotal, cardVinyl, cardCds;
    TextView totalValue, vinylValue, cdsValue;
    TextView totalLabel, vinylLabel, cdsLabel;
    private ImageView ivProfile;
    private RecyclerView rvRecent;
    private FloatingActionButton fabAddAlbum;
    private RecentAlbumListAdapter adapter;
    private List<AlbumWithArtists> albums;
    private AppDatabase db;
    private DatabaseHelper databaseHelper;
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.get(this);
        databaseHelper = DatabaseHelper.get(this);

        // seedTags(db, List.of("Autographed", "Boxset", "Limited Edition", "Deluxe"));

        TwoFingerZoomHelper.enableTwoFingerZoom(this);
        TwoFingerDoubleTapHelper.enableTwoFingerDoubleTap(this);

        initViews();
        setupData();
        setupLaunchers();
        setupMenu();
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
        cardTotal = findViewById(R.id.cardTotal);
        cardVinyl = findViewById(R.id.cardVinyl);
        cardCds = findViewById(R.id.cardCds);
        // find the TextViews *inside* each included card
        totalValue = cardTotal.findViewById(R.id.tvValue);
        totalLabel = cardTotal.findViewById(R.id.tvLabel);

        vinylValue = cardVinyl.findViewById(R.id.tvValue);
        vinylLabel = cardVinyl.findViewById(R.id.tvLabel);

        cdsValue = cardCds.findViewById(R.id.tvValue);
        cdsLabel = cardCds.findViewById(R.id.tvLabel);
        // Set values
        totalLabel.setText("Total");
        vinylLabel.setText("Vinyl");
        cdsLabel.setText("CDs");


        rvRecent = findViewById(R.id.rvRecent);
        ivProfile = findViewById(R.id.ivProfile);
        fabAddAlbum = findViewById(R.id.fabAddAlbum);

        cardTotal.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, AlbumListActivity.class);
            startActivity(i);
        });

        cardVinyl.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, AlbumListActivity.class);
            i.putExtra(EXTRA_FILTER_VINYL, true);
            i.putExtra(EXTRA_FILTER_CDS, false);
            startActivity(i);
        });

        cardCds.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, AlbumListActivity.class);
            i.putExtra(EXTRA_FILTER_VINYL, false);
            i.putExtra(EXTRA_FILTER_CDS, true);
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
        totalValue.setText(String.valueOf(total));
        vinylValue.setText(String.valueOf(vinyl));
        cdsValue.setText(String.valueOf(cds));
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

    private void setupLaunchers() {

        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        databaseHelper.exportDatabase(uri);
                        Toast.makeText(this, "Exported!", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        databaseHelper.importDatabase(uri);
                        restartApp();
                    }
                }
        );
    }

    private void setupMenu() {
        ivProfile.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(this, v);
            menu.getMenu().add("Export Database");
            menu.getMenu().add("Import Database");

            menu.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Export Database")) {
                    startExportPicker();
                } else if (item.getTitle().equals("Import Database")) {
                    startImportPicker();
                }
                return true;
            });

            menu.show();
        });
    }

    private void startExportPicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/x-sqlite3");

        String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String fileName = "trackd_backup_" + time + ".db";
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        exportLauncher.launch(intent);
    }

    private void startImportPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        importLauncher.launch(intent);
    }

    private void restartApp() {
        Intent i = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());

        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }

        finish();
        Runtime.getRuntime().exit(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TwoFingerZoomHelper.cleanup(this);
    }
}
