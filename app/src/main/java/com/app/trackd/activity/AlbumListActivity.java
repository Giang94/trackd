package com.app.trackd.activity;

import static com.app.trackd.activity.EditAlbumActivity.EXTRA_ALBUM_ID;
import static com.app.trackd.activity.EditAlbumActivity.EXTRA_UPDATED_ALBUM_ID;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.trackd.R;
import com.app.trackd.adapter.AlbumListAdapter;
import com.app.trackd.common.NoMultiTouchEditText;
import com.app.trackd.common.TwoFingerDoubleTapHelper;
import com.app.trackd.common.TwoFingerZoomHelper;
import com.app.trackd.database.AppDatabase;
import com.app.trackd.fragment.AlbumDetailBottomSheet;
import com.app.trackd.fragment.AlbumFilterBottomSheet;
import com.app.trackd.model.Album;
import com.app.trackd.model.AlbumWithArtists;
import com.app.trackd.model.enums.AlbumFormat;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AlbumListActivity extends FragmentActivity {

    public static final String EXTRA_FILTER_VINYL = "filterVinyl";
    public static final String EXTRA_FILTER_CDS = "filterCds";

    private RecyclerView rvAlbums;
    private TextInputLayout searchInputLayout;
    private NoMultiTouchEditText searchInput;
    private TextView tvTitle;
    private ImageButton btnFilter;

    private AlbumListAdapter adapter;
    private final List<AlbumWithArtists> albums = new ArrayList<>();

    private AppDatabase db;

    private boolean filterVinyl = true;
    private boolean filterCds = true;

    // --- ACTIVITY RESULT HANDLER ---
    private final ActivityResultLauncher<Intent> editAlbumLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK &&
                        result.getData() != null) {
                    long id = result.getData().getLongExtra(EXTRA_UPDATED_ALBUM_ID, -1);
                    if (id != -1) updateSingleAlbum(id);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_list);

        ViewGroup decor = (ViewGroup) getWindow().getDecorView();
        for (int i = 0; i < decor.getChildCount(); i++) {
            Log.d("DecorChild", "Child " + i + ": " + decor.getChildAt(i));
        }

        TwoFingerZoomHelper.enableTwoFingerZoom(this);
        TwoFingerDoubleTapHelper.enableTwoFingerDoubleTap(this);

        initDatabase();
        initViews();
        initRecycler();
        initSearch();

        // Read filter from intent
        filterVinyl = getIntent().getBooleanExtra(EXTRA_FILTER_VINYL, true);
        filterCds = getIntent().getBooleanExtra(EXTRA_FILTER_CDS, true);
        applyCombinedFilter();
    }

    private void initDatabase() {
        db = AppDatabase.get(this);
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        rvAlbums = findViewById(R.id.rvAlbums);
        searchInput = findViewById(R.id.searchInput);
        searchInputLayout = findViewById(R.id.searchInputLayout);
        btnFilter = findViewById(R.id.btnFilter);
        btnFilter.setOnClickListener(v -> showFilterSheet());
        updateHeader(albums);
    }

    private void initRecycler() {
        rvAlbums.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AlbumListAdapter(albums, this::applyCombinedFilter, this::openAlbumDetails);
        rvAlbums.setAdapter(adapter);
    }

    private void initSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    searchInputLayout.setEndIconDrawable(R.drawable.ic_edit_clear);
                    searchInputLayout.setEndIconContentDescription("Clear");
                } else {
                    searchInputLayout.setEndIconDrawable(R.drawable.ic_search);
                    searchInputLayout.setEndIconContentDescription("Search");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                applyCombinedFilter();
            }
        });

        searchInputLayout.setEndIconOnClickListener(v -> {
            if (searchInput.getText() != null && searchInput.getText().length() > 0) {
                searchInput.setText("");
            } else {
                searchInput.requestFocus();
            }
        });
    }

    // ----------------- FILTER HANDLING -----------------
    private void showFilterSheet() {
        AlbumFilterBottomSheet sheet = new AlbumFilterBottomSheet(
                filterVinyl,
                filterCds,
                (vinyl, cds) -> {
                    filterVinyl = vinyl;
                    filterCds = cds;
                    applyCombinedFilter();
                }
        );
        sheet.show(getSupportFragmentManager(), "album_filter_sheet");
    }

    private void applyCombinedFilter() {
        String text = searchInput.getText() != null ? searchInput.getText().toString().trim() : "";

        new Thread(() -> {
            List<Album> filtered;

            if (filterVinyl && filterCds) {
                filtered = db.albumDao().getAllAlbums();
            } else if (filterVinyl) {
                filtered = db.albumDao().getAlbumsByFormats(AlbumFormat.getVinylNames());
            } else if (filterCds) {
                filtered = db.albumDao().getAlbumsByFormats(List.of(AlbumFormat.CD.name()));
            } else {
                filtered = db.albumDao().getAllAlbums();
            }

            // Fetch AlbumWithArtists for these albums
            List<Long> ids = filtered.stream().map(Album::getId).toList();

            var data = new Object() {
                List<AlbumWithArtists> values = new ArrayList<>();
            };
            data.values = db.albumDao().getAlbumsWithArtistsByIds(ids);

            // Now filter by text input (album name OR artist name)
            if (!text.isEmpty()) {
                data.values = data.values.stream()
                        .filter(awa -> awa.getAlbum().getTitle().toLowerCase()
                                .contains(searchInput.getText().toString().trim().toLowerCase()) ||
                                awa.getArtists().stream()
                                        .anyMatch(ar -> ar.getDisplayName().toLowerCase()
                                                .contains(searchInput.getText().toString().trim().toLowerCase()))
                        )
                        .collect(Collectors.toList());
            }

            runOnUiThread(() -> {
                adapter.updateList(data.values);
                updateHeader(data.values);
            });
        }).start();
    }


    // ----------------- DETAILS + UPDATE -----------------
    private void openAlbumDetails(AlbumWithArtists album) {
        AlbumDetailBottomSheet sheet = new AlbumDetailBottomSheet(album);

        sheet.setOnAlbumEditListener(albumId -> {
            Intent intent = new Intent(this, EditAlbumActivity.class);
            intent.putExtra(EXTRA_ALBUM_ID, albumId);
            editAlbumLauncher.launch(intent);
        });

        sheet.setOnAlbumDeletedListener(albumId -> {
            applyCombinedFilter();
        });

        sheet.show(getSupportFragmentManager(), "album_detail_sheet");
    }

    private void updateHeader(List<AlbumWithArtists> currentList) {
        tvTitle.setText("Albums (" + currentList.size() + ")");
    }

    private void updateSingleAlbum(long albumId) {
        new Thread(() -> {
            AlbumWithArtists updated = db.albumDao().getAlbumWithArtistsById(albumId);
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
            if (albums.get(i).getAlbum().getId() == albumId) return i;
        }
        return -1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TwoFingerZoomHelper.cleanup(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyCombinedFilter();
    }
}
