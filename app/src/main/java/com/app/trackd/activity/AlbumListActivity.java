package com.app.trackd.activity;

import static com.app.trackd.activity.EditAlbumActivity.EXTRA_ALBUM_ID;
import static com.app.trackd.activity.EditAlbumActivity.EXTRA_UPDATED_ALBUM_ID;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.trackd.R;
import com.app.trackd.adapter.AlbumListAdapter;
import com.app.trackd.common.NoMultiTouchEditText;
import com.app.trackd.common.TwoFingerDoubleTapHelper;
import com.app.trackd.common.TwoFingerZoomHelper;
import com.app.trackd.database.AppDatabase;
import com.app.trackd.fragment.AlbumDetailBottomSheet;
import com.app.trackd.model.Album;
import com.app.trackd.model.AlbumWithArtists;
import com.app.trackd.model.enums.AlbumFormat;
import com.app.trackd.util.ThemeHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class AlbumListActivity extends FragmentActivity {

    private static final String PREFS_ALBUM_LIST = "album_list_prefs";
    private static final String PREF_KEY_VIEW_MODE = "view_mode";

    private enum FormatFilter {
        ALL,
        LP_12,
        LP_10,
        LP_7,
        CD,
        DVD,
        CASSETTE
    }

    private final List<AlbumWithArtists> albums = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private RecyclerView rvAlbums;
    private TextInputLayout searchInputLayout;
    private NoMultiTouchEditText searchInput;
    private TextView tvTitle;
    private ImageButton btnLayoutSwitch;
    private ChipGroup chipGroupFormatFilter;
    private Chip chipFilterAll;
    private Chip chipFilterLp12;
    private Chip chipFilterLp10;
    private Chip chipFilterLp7;
    private Chip chipFilterCd;
    private Chip chipFilterDvd;
    private Chip chipFilterCassette;
    private AlbumListAdapter adapter;
    private AppDatabase db;
    private boolean firstLoad = true;
    private final ActivityResultLauncher<Intent> editAlbumLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK &&
                        result.getData() != null) {
                    long id = result.getData().getLongExtra(EXTRA_UPDATED_ALBUM_ID, -1);
                    if (id != -1) updateSingleAlbum(id);
                }
            });
    private String currentQuery = "";
    private Runnable searchRunnable;
    private int totalMatchingCount = 0;
    private FormatFilter selectedFormatFilter = FormatFilter.ALL;
    private boolean suppressFormatChipCallback = false;
    private AlbumListAdapter.ViewMode currentViewMode = AlbumListAdapter.ViewMode.LIST;

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
        btnLayoutSwitch = findViewById(R.id.btnLayoutSwitch);
        chipGroupFormatFilter = findViewById(R.id.chipGroupFormatFilter);
        chipFilterAll = findViewById(R.id.chipFilterAll);
        chipFilterLp12 = findViewById(R.id.chipFilterLp12);
        chipFilterLp10 = findViewById(R.id.chipFilterLp10);
        chipFilterLp7 = findViewById(R.id.chipFilterLp7);
        chipFilterCd = findViewById(R.id.chipFilterCd);
        chipFilterDvd = findViewById(R.id.chipFilterDvd);
        chipFilterCassette = findViewById(R.id.chipFilterCassette);

        SharedPreferences preferences = getSharedPreferences(PREFS_ALBUM_LIST, MODE_PRIVATE);
        currentViewMode = readSavedViewMode(preferences);

        btnLayoutSwitch.setOnClickListener(v -> toggleViewMode());
        chipGroupFormatFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (suppressFormatChipCallback) return;
            if (checkedIds.isEmpty()) return;
            selectedFormatFilter = mapCheckedChipToFilter(checkedIds.get(0));
            refreshGridLayoutManagerIfNeeded();
            applyCombinedFilter();
        });
        updateHeader();
        updateLayoutSwitchButton();
    }

    private void initRecycler() {
        applyRecyclerPaddingForViewMode(currentViewMode);
        adapter = new AlbumListAdapter(albums, this::openAlbumDetails);
        adapter.setViewMode(currentViewMode);
        adapter.setSectionHeadersEnabled(shouldShowSectionHeaders());
        rvAlbums.setAdapter(adapter);
        rvAlbums.setLayoutManager(createLayoutManager(currentViewMode));
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
        tvTitle.setText(getString(R.string.header_albums_count, totalMatchingCount));
    }

    private void updateSingleAlbum(long albumId) {
        new Thread(() -> {
            AlbumWithArtists updated = db.albumDao().getAlbumWithArtistsById(albumId);
            int index = findAlbumIndex(albumId);
            if (index == -1) return;
            runOnUiThread(() -> {
                albums.set(index, updated);
                adapter.notifyAlbumsChanged();
            });
        }).start();
    }

    private int findAlbumIndex(long albumId) {
        for (int i = 0; i < albums.size(); i++) {
            if (albums.get(i).getAlbum().getId() == albumId) return i;
        }
        return -1;
    }

    private RecyclerView.LayoutManager createLayoutManager(AlbumListAdapter.ViewMode viewMode) {
        if (viewMode == AlbumListAdapter.ViewMode.GRID) {
            int spanCount = getGridSpanCount();
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, spanCount);
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return resolveGridSpanSize(position, gridLayoutManager.getSpanCount());
                }
            });
            return gridLayoutManager;
        }
        return new LinearLayoutManager(this);
    }

    private int getGridSpanCount() {
        if (selectedFormatFilter == FormatFilter.ALL) {
            // 12 lets us mix 3-up LP tiles (span 4) and 4-up CD/DVD tiles (span 3).
            return 12;
        }
        return isCdLikeFilter(selectedFormatFilter) ? 4 : 3;
    }

    private int resolveGridSpanSize(int position, int gridSpanCount) {
        if (adapter == null) return 1;
        if (adapter.isSectionHeaderPosition(position)) return gridSpanCount;
        if (selectedFormatFilter != FormatFilter.ALL) return 1;

        AlbumFormat format = adapter.getGridItemFormatAt(position);
        if (format == AlbumFormat.CD || format == AlbumFormat.CASSETTE) {
            return 3;
        }
        return 4;
    }

    private void applyRecyclerPaddingForViewMode(AlbumListAdapter.ViewMode viewMode) {
        int base = getResources().getDimensionPixelSize(R.dimen.screen_padding);
        int horizontal = viewMode == AlbumListAdapter.ViewMode.GRID ? base / 2 : base;
        rvAlbums.setPadding(horizontal, base, horizontal, base);
        rvAlbums.setClipToPadding(false);
    }

    private FormatFilter mapCheckedChipToFilter(int checkedChipId) {
        if (checkedChipId == R.id.chipFilterLp12) return FormatFilter.LP_12;
        if (checkedChipId == R.id.chipFilterLp10) return FormatFilter.LP_10;
        if (checkedChipId == R.id.chipFilterLp7) return FormatFilter.LP_7;
        if (checkedChipId == R.id.chipFilterCd) return FormatFilter.CD;
        if (checkedChipId == R.id.chipFilterDvd) return FormatFilter.DVD;
        if (checkedChipId == R.id.chipFilterCassette) return FormatFilter.CASSETTE;
        return FormatFilter.ALL;
    }

    private boolean shouldShowSectionHeaders() {
        return selectedFormatFilter == FormatFilter.ALL;
    }

    private boolean isCdLikeFilter(FormatFilter filter) {
        return filter == FormatFilter.CD
                || filter == FormatFilter.DVD
                || filter == FormatFilter.CASSETTE;
    }

    private void refreshGridLayoutManagerIfNeeded() {
        if (currentViewMode != AlbumListAdapter.ViewMode.GRID || rvAlbums == null) return;
        RecyclerView.LayoutManager layoutManager = createLayoutManager(currentViewMode);
        rvAlbums.setLayoutManager(layoutManager);
    }

    private AlbumListAdapter.ViewMode readSavedViewMode(SharedPreferences preferences) {
        String saved = preferences.getString(PREF_KEY_VIEW_MODE, AlbumListAdapter.ViewMode.LIST.name());
        try {
            return AlbumListAdapter.ViewMode.valueOf(saved);
        } catch (IllegalArgumentException ignored) {
            return AlbumListAdapter.ViewMode.LIST;
        }
    }

    private void persistViewMode(AlbumListAdapter.ViewMode viewMode) {
        getSharedPreferences(PREFS_ALBUM_LIST, MODE_PRIVATE)
                .edit()
                .putString(PREF_KEY_VIEW_MODE, viewMode.name())
                .apply();
    }

    private void toggleViewMode() {
        AlbumListAdapter.ViewMode nextMode = currentViewMode == AlbumListAdapter.ViewMode.LIST
                ? AlbumListAdapter.ViewMode.GRID
                : AlbumListAdapter.ViewMode.LIST;
        applyViewMode(nextMode);
    }

    private void applyViewMode(AlbumListAdapter.ViewMode nextMode) {
        if (adapter == null || rvAlbums == null) return;
        if (currentViewMode == nextMode) {
            updateLayoutSwitchButton();
            return;
        }

        int anchorPosition = 0;
        int anchorOffset = 0;
        RecyclerView.LayoutManager existing = rvAlbums.getLayoutManager();
        if (existing instanceof LinearLayoutManager) {
            LinearLayoutManager existingLinear = (LinearLayoutManager) existing;
            anchorPosition = Math.max(0, existingLinear.findFirstVisibleItemPosition());
            View anchorView = existingLinear.findViewByPosition(anchorPosition);
            if (anchorView != null) {
                anchorOffset = anchorView.getTop() - rvAlbums.getPaddingTop();
            }
        }

        currentViewMode = nextMode;
        adapter.setViewMode(nextMode);
        adapter.setSectionHeadersEnabled(shouldShowSectionHeaders());
        applyRecyclerPaddingForViewMode(nextMode);

        RecyclerView.LayoutManager nextLayoutManager = createLayoutManager(nextMode);
        rvAlbums.setLayoutManager(nextLayoutManager);
        if (nextLayoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager nextLinear = (LinearLayoutManager) nextLayoutManager;
            nextLinear.scrollToPositionWithOffset(anchorPosition, anchorOffset);
        }

        persistViewMode(nextMode);
        updateLayoutSwitchButton();
    }

    private void updateLayoutSwitchButton() {
        if (btnLayoutSwitch == null) return;

        if (currentViewMode == AlbumListAdapter.ViewMode.LIST) {
            btnLayoutSwitch.setImageResource(R.drawable.ic_view_grid);
            btnLayoutSwitch.setContentDescription(getString(R.string.cd_switch_to_grid_view));
        } else {
            btnLayoutSwitch.setImageResource(R.drawable.ic_view_list);
            btnLayoutSwitch.setContentDescription(getString(R.string.cd_switch_to_list_view));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TwoFingerZoomHelper.cleanup(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firstLoad) {
            firstLoad = false;
            return;
        }
        applyCombinedFilter();
    }

    private void applyCombinedFilter() {
        rvAlbums.post(() -> {
            currentQuery = searchInput.getText() == null
                    ? ""
                    : searchInput.getText().toString().trim().toLowerCase();

            new Thread(() -> {
                List<AlbumWithArtists> allMatchingAlbums = queryAllMatchingAlbumsWithArtists();
                Map<AlbumFormat, Integer> countsByFormat = countByFormat(allMatchingAlbums);
                boolean selectedHasResults = selectedFormatFilter == FormatFilter.ALL
                        || getFormatCountForFilter(countsByFormat, selectedFormatFilter) > 0;
                FormatFilter effectiveFilter = selectedHasResults ? selectedFormatFilter : FormatFilter.ALL;
                List<AlbumWithArtists> filteredAlbums = applyFormatFilter(allMatchingAlbums, effectiveFilter);

                runOnUiThread(() -> {
                    selectedFormatFilter = effectiveFilter;
                    updateFormatChipLabels(countsByFormat, allMatchingAlbums.size());
                    refreshGridLayoutManagerIfNeeded();

                    albums.clear();
                    albums.addAll(filteredAlbums);
                    adapter.setSectionHeadersEnabled(shouldShowSectionHeaders());
                    adapter.setSectionCounts(countsByFormat);
                    adapter.notifyAlbumsChanged();
                    totalMatchingCount = filteredAlbums.size();
                    updateHeader();
                });
            }).start();
        });
    }

    private List<AlbumWithArtists> queryAllMatchingAlbumsWithArtists() {
        boolean hasSearch = !currentQuery.isEmpty();
        List<String> allFormats = AlbumFormat.getNames();

        List<Album> matching;
        if (hasSearch) {
            matching = db.albumDao().searchAlbumsWithFormats(
                    allFormats,
                    true,
                    "%" + currentQuery + "%"
            );
        } else {
            matching = db.albumDao().getAllAlbums();
        }

        if (matching.isEmpty()) return List.of();
        List<Long> ids = matching.stream().map(Album::getId).toList();
        return db.albumDao().getAlbumsWithArtistsByIds(ids);
    }

    private Map<AlbumFormat, Integer> countByFormat(List<AlbumWithArtists> filteredAlbums) {
        Map<AlbumFormat, Integer> counts = new EnumMap<>(AlbumFormat.class);
        for (AlbumWithArtists item : filteredAlbums) {
            AlbumFormat format = item.getAlbum().getFormat();
            if (format == null) continue;
            counts.merge(format, 1, Integer::sum);
        }
        return counts;
    }

    private List<AlbumWithArtists> applyFormatFilter(
            List<AlbumWithArtists> allAlbums,
            FormatFilter filter
    ) {
        if (filter == FormatFilter.ALL) {
            return allAlbums;
        }

        AlbumFormat targetFormat = mapFilterToFormat(filter);
        if (targetFormat == null) return allAlbums;

        List<AlbumWithArtists> filtered = new ArrayList<>();
        for (AlbumWithArtists item : allAlbums) {
            if (item.getAlbum().getFormat() == targetFormat) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private AlbumFormat mapFilterToFormat(FormatFilter filter) {
        switch (filter) {
            case LP_12:
                return AlbumFormat.VINYL;
            case LP_10:
                return AlbumFormat.VINYL_10;
            case LP_7:
                return AlbumFormat.VINYL_7;
            case CD:
                return AlbumFormat.CD;
            case DVD:
                return null;
            case CASSETTE:
                return AlbumFormat.CASSETTE;
            case ALL:
            default:
                return null;
        }
    }

    private int getFormatCountForFilter(Map<AlbumFormat, Integer> countsByFormat, FormatFilter filter) {
        if (filter == FormatFilter.ALL) {
            return countsByFormat.values().stream().mapToInt(Integer::intValue).sum();
        }
        AlbumFormat targetFormat = mapFilterToFormat(filter);
        if (targetFormat == null) return 0;
        return countsByFormat.getOrDefault(targetFormat, 0);
    }

    private void updateFormatChipLabels(Map<AlbumFormat, Integer> countsByFormat, int totalCount) {
        int count12 = countsByFormat.getOrDefault(AlbumFormat.VINYL, 0);
        int count10 = countsByFormat.getOrDefault(AlbumFormat.VINYL_10, 0);
        int count7 = countsByFormat.getOrDefault(AlbumFormat.VINYL_7, 0);
        int countCd = countsByFormat.getOrDefault(AlbumFormat.CD, 0);
        int countCassette = countsByFormat.getOrDefault(AlbumFormat.CASSETTE, 0);
        int countDvd = 0;

        chipFilterAll.setText("All (" + totalCount + ")");
        chipFilterLp12.setText("LP 12\" (" + count12 + ")");
        chipFilterLp10.setText("LP 10\" (" + count10 + ")");
        chipFilterLp7.setText("LP 7\" (" + count7 + ")");
        chipFilterCd.setText("CD (" + countCd + ")");
        chipFilterDvd.setText("DVD (" + countDvd + ")");
        chipFilterCassette.setText("Cassette (" + countCassette + ")");

        chipFilterLp12.setVisibility(count12 > 0 ? View.VISIBLE : View.GONE);
        chipFilterLp10.setVisibility(count10 > 0 ? View.VISIBLE : View.GONE);
        chipFilterLp7.setVisibility(count7 > 0 ? View.VISIBLE : View.GONE);
        chipFilterCd.setVisibility(countCd > 0 ? View.VISIBLE : View.GONE);
        chipFilterDvd.setVisibility(countDvd > 0 ? View.VISIBLE : View.GONE);
        chipFilterCassette.setVisibility(countCassette > 0 ? View.VISIBLE : View.GONE);

        if (selectedFormatFilter != FormatFilter.ALL && getFormatCountForFilter(countsByFormat, selectedFormatFilter) <= 0) {
            selectedFormatFilter = FormatFilter.ALL;
        }

        int targetChipId = mapFilterToChipId(selectedFormatFilter);
        if (chipGroupFormatFilter.getCheckedChipId() != targetChipId) {
            suppressFormatChipCallback = true;
            chipGroupFormatFilter.check(targetChipId);
            suppressFormatChipCallback = false;
        }
    }

    private int mapFilterToChipId(FormatFilter filter) {
        switch (filter) {
            case LP_12:
                return R.id.chipFilterLp12;
            case LP_10:
                return R.id.chipFilterLp10;
            case LP_7:
                return R.id.chipFilterLp7;
            case CD:
                return R.id.chipFilterCd;
            case DVD:
                return R.id.chipFilterDvd;
            case CASSETTE:
                return R.id.chipFilterCassette;
            case ALL:
            default:
                return R.id.chipFilterAll;
        }
    }


}
