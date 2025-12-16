package com.app.trackd.activity;

import static com.app.trackd.activity.EditAlbumActivity.EXTRA_ALBUM_ID;
import static com.app.trackd.activity.EditAlbumActivity.EXTRA_UPDATED_ALBUM_ID;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
import com.app.trackd.util.ThemeHelper;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class AlbumListActivity extends FragmentActivity {

    public static final String EXTRA_FILTER_VINYL = "filterVinyl";
    public static final String EXTRA_FILTER_CDS = "filterCds";
    private static final int PAGE_SIZE = 10;
    private final List<AlbumWithArtists> albums = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private RecyclerView rvAlbums;
    private TextInputLayout searchInputLayout;
    private NoMultiTouchEditText searchInput;
    private TextView tvTitle;
    private ImageButton btnFilter;
    private AlbumListAdapter adapter;
    private AppDatabase db;
    private final ActivityResultLauncher<Intent> editAlbumLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK &&
                        result.getData() != null) {
                    long id = result.getData().getLongExtra(EXTRA_UPDATED_ALBUM_ID, -1);
                    if (id != -1) updateSingleAlbum(id);
                }
            });
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private String currentQuery = "";
    private Runnable searchRunnable;
    private int totalMatchingCount = 0;
    private boolean filterVinyl = true;
    private boolean filterCds = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_list);

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
        updateHeader();
    }

    private void initRecycler() {
        rvAlbums.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AlbumListAdapter(albums, this::openAlbumDetails);
        rvAlbums.setAdapter(adapter);
        rvAlbums.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || isLoading || !hasMore) return;

                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;

                int visible = lm.getChildCount();
                int total = lm.getItemCount();
                int firstVisible = lm.findFirstVisibleItemPosition();

                if (firstVisible + visible >= total - 2) {
                    loadNextPage();
                }
            }
        });
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
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> applyCombinedFilter();
                handler.postDelayed(searchRunnable, 300);
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

    private void updateHeader() {
        tvTitle.setText(String.format("Albums (%d)", totalMatchingCount));
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

    private void applyCombinedFilter() {
        rvAlbums.post(() -> {
            currentPage = 0;
            hasMore = true;
            isLoading = false;

            currentQuery = searchInput.getText() == null
                    ? ""
                    : searchInput.getText().toString().trim().toLowerCase();

            albums.clear();
            adapter.notifyDataSetChanged();

            // 🔥 NEW: calculate total count
            new Thread(() -> {
                int count = queryTotalCount();

                runOnUiThread(() -> {
                    totalMatchingCount = count;
                    updateHeader();
                });
            }).start();

            loadNextPage();
        });
    }

    private int queryTotalCount() {
        boolean hasSearch = !currentQuery.isEmpty();
        List<String> formats = getActiveFormats();
        boolean allFormats = formats.isEmpty();

        if (hasSearch && allFormats) {
            return db.albumDao()
                    .countSearchAlbums("%" + currentQuery + "%");
        }

        if (hasSearch) {
            return db.albumDao()
                    .countAlbumsByFormatsAndSearch(
                            formats,
                            "%" + currentQuery + "%"
                    );
        }

        if (allFormats) {
            return db.albumDao().countAllAlbums();
        }

        return db.albumDao()
                .countAlbumsByFormats(formats);
    }


    private List<String> getActiveFormats() {
        if (filterVinyl && filterCds) return AlbumFormat.getNames();
        if (filterVinyl) return AlbumFormat.getVinylNames();
        if (filterCds) return List.of(AlbumFormat.CD.name());
        return List.of();
    }

    private void loadNextPage() {
        if (isLoading || !hasMore) return;
        isLoading = true;

        new Thread(() -> {
            List<AlbumWithArtists> next = queryNextPage();

            runOnUiThread(() -> {
                if (!next.isEmpty()) {
                    int start = albums.size();
                    albums.addAll(next);
                    adapter.notifyItemRangeInserted(start, next.size());
                    updateHeader();
                }
                isLoading = false;
            });
        }).start();
    }

    private List<AlbumWithArtists> queryNextPage() {
        int offset = currentPage * PAGE_SIZE;
        boolean hasSearch = !currentQuery.isEmpty();
        List<String> formats = getActiveFormats();
        boolean allFormats = formats.isEmpty();

        List<Album> page;

        if (hasSearch) {
            page = db.albumDao().searchAlbumsPagedWithFormats(
                    formats,
                    allFormats,
                    "%" + currentQuery + "%",
                    PAGE_SIZE,
                    offset
            );
        } else if (allFormats) {
            page = db.albumDao().getAlbumsPaged(offset, PAGE_SIZE);
        } else {
            page = db.albumDao()
                    .getAlbumsByFormatsPaged(formats, PAGE_SIZE, offset);
        }

        if (page.isEmpty()) {
            hasMore = false;
            return List.of();
        }

        currentPage++;

        List<Long> ids = page.stream().map(Album::getId).toList();
        return db.albumDao().getAlbumsWithArtistsByIds(ids);
    }


}
