package com.app.trackd.activity;

import static com.app.trackd.activity.EditAlbumActivity.EXTRA_ALBUM_ID;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.trackd.R;
import com.app.trackd.adapter.AlbumListAdapter;
import com.app.trackd.common.TwoFingerDoubleTapHelper;
import com.app.trackd.common.TwoFingerZoomHelper;
import com.app.trackd.database.AppDatabase;
import com.app.trackd.fragment.AlbumDetailBottomSheet;
import com.app.trackd.model.Album;
import com.app.trackd.model.AlbumWithArtists;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AlbumListActivity extends FragmentActivity {

    private RecyclerView rvAlbums;
    private TextInputLayout searchInputLayout;
    private TextInputEditText searchInput;
    private TextView tvTitle;
    private FloatingActionButton fabAddAlbum;

    private AlbumListAdapter adapter;
    private final List<AlbumWithArtists> albums = new ArrayList<>();

    private AppDatabase db;

    private static final int PAGE_SIZE = 10;
    private int currentOffset = 0;
    private boolean isLoading = false;
    private boolean noMore = false;

    // --- ACTIVITY RESULT HANDLER ---
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

    // ---------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_list);

        initGestures();
        initDatabase();
        initViews();
        initRecycler();
        initAddButton();
        initSearch();

        loadPagedAlbums();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHeader();
    }

    // ---------------------------------------------------------------------
    // INIT
    // ---------------------------------------------------------------------

    private void initGestures() {
        TwoFingerDoubleTapHelper.enableTwoFingerDoubleTap(this);
        TwoFingerZoomHelper.enableTwoFingerZoom(this);
    }

    private void initDatabase() {
        db = AppDatabase.get(this);
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        rvAlbums = findViewById(R.id.rvAlbums);
        searchInput = findViewById(R.id.searchInput);
        searchInputLayout = findViewById(R.id.searchInputLayout);
        updateHeader();
    }

    private void initRecycler() {
        rvAlbums.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AlbumListAdapter(
                albums,
                this::loadPagedAlbums,
                this::openAlbumDetails
        );

        rvAlbums.setAdapter(adapter);
    }

    private void initAddButton() {
        fabAddAlbum = findViewById(R.id.fabAddAlbum);
        fabAddAlbum.setOnClickListener(v ->
                startActivity(new Intent(this, AddAlbumActivity.class))
        );
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
                handleSearch(editable.toString().trim());
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

    // ---------------------------------------------------------------------
    // SEARCH HANDLING
    // ---------------------------------------------------------------------

    private void handleSearch(String text) {
        if (text.isEmpty()) {
            resetFullList();
            loadPagedAlbums();
            return;
        }

        Log.d("FILTER", "Searching: " + text);

        new Thread(() -> {
            List<Album> results =
                    db.albumDao().searchAlbums("%" + text + "%");

            List<Long> ids = results.stream()
                    .map(Album::getId)
                    .collect(Collectors.toList());

            List<AlbumWithArtists> fullData =
                    db.albumDao().getAlbumsWithArtistsByIds(ids);

            runOnUiThread(() -> adapter.updateList(fullData));
        }).start();
    }

    private void resetFullList() {
        albums.clear();
        adapter.notifyDataSetChanged();

        currentOffset = 0;
        noMore = false;
        isLoading = false;
    }

    // ---------------------------------------------------------------------
    // PAGINATION
    // ---------------------------------------------------------------------

    private void loadPagedAlbums() {
        if (isLoading || noMore) return;
        isLoading = true;

        new Thread(() -> {
            List<Album> page =
                    db.albumDao().getAlbumsPaged(currentOffset, PAGE_SIZE);

            if (page.isEmpty()) {
                noMore = true;
                isLoading = false;
                return;
            }

            List<Long> ids = page.stream()
                    .map(Album::getId)
                    .collect(Collectors.toList());

            List<AlbumWithArtists> fullData =
                    db.albumDao().getAlbumsWithArtistsByIds(ids);

            runOnUiThread(() -> {
                albums.addAll(fullData);
                adapter.notifyDataSetChanged();
                currentOffset += page.size();
                isLoading = false;
            });
        }).start();
    }

    // ---------------------------------------------------------------------
    // DETAILS + UPDATE
    // ---------------------------------------------------------------------

    private void openAlbumDetails(AlbumWithArtists album) {
        AlbumDetailBottomSheet sheet = new AlbumDetailBottomSheet(album);

        sheet.setOnAlbumEditListener(albumId -> {
            Intent intent = new Intent(this, EditAlbumActivity.class);
            intent.putExtra(EXTRA_ALBUM_ID, albumId);
            editAlbumLauncher.launch(intent);
        });
        sheet.setOnAlbumDeletedListener(albumId -> {
            resetFullList();
            updateHeader();
            loadPagedAlbums();
        });

        sheet.show(getSupportFragmentManager(), "album_detail_sheet");
    }

    private void updateHeader() {
        int count = db.albumDao().getAlbumCount();
        tvTitle.setText("Albums (" + count + ")");
    }

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
}
