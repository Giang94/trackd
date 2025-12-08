package com.app.trackd.activity;

import static com.app.trackd.activity.AlbumDetailsActivity.EXTRA_ALBUM_ID_TO_ALBUM_DETAILS;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.trackd.R;
import com.app.trackd.adapter.AlbumListAdapter;
import com.app.trackd.common.DoubleTapHelper;
import com.app.trackd.common.TwoFingerZoomHelper;
import com.app.trackd.database.AppDatabase;
import com.app.trackd.model.Album;
import com.app.trackd.model.AlbumWithArtists;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AlbumListActivity extends AppCompatActivity {

    RecyclerView rvAlbums;
    TextInputEditText searchInput;
    AlbumListAdapter adapter;
    List<AlbumWithArtists> items = new ArrayList<>();

    AppDatabase db;

    int PAGE_SIZE = 10;
    int currentOffset = 0;
    boolean isLoading = false;
    boolean noMore = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_list);

        DoubleTapHelper.enableDoubleTap(this);
        TwoFingerZoomHelper.enableTwoFingerZoom(this);

        rvAlbums = findViewById(R.id.rvAlbums);
        rvAlbums.setLayoutManager(new LinearLayoutManager(this));

        db = AppDatabase.get(this);

        adapter = new AlbumListAdapter(
                items,
                this::loadPagedAlbums,
                album -> openAlbumDetails(album)
        );
        rvAlbums.setAdapter(adapter);

        loadPagedAlbums();
        setupSearchInput();
    }

    private void loadPagedAlbums() {
        if (isLoading || noMore) return;
        isLoading = true;

        // 1) Load base album list
        List<Album> baseAlbums = db.albumDao().getAlbumsPaged(currentOffset, PAGE_SIZE);

        if (baseAlbums.isEmpty()) {
            noMore = true;
            isLoading = false;
            return;
        }

        // 3) Load AlbumWithArtists for these IDs
        List<AlbumWithArtists> fullData = db.albumDao()
                .getAlbumsWithArtistsByIds(baseAlbums.stream().map(Album::getId).toList());

        // 4) Add to adapter list
        items.addAll(fullData);
        adapter.notifyDataSetChanged();

        // 5) Update offset
        currentOffset += baseAlbums.size();

        isLoading = false;
    }

    private void setupSearchInput() {
        searchInput = findViewById(R.id.searchInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String text = editable.toString().trim();

                if (text.isEmpty()) {
                    Log.d("FILTER", "Clearing search, reloading full list");

                    items.clear();
                    adapter.notifyDataSetChanged();

                    currentOffset = 0;
                    noMore = false;
                    isLoading = false;

                    loadPagedAlbums();
                } else {
                    Log.d("FILTER", "Searching for: " + text);
                    new Thread(() -> {
                        List<Album> results = db.albumDao().searchAlbums("%" + text + "%");
                        List<AlbumWithArtists> fullData = db.albumDao()
                                .getAlbumsWithArtistsByIds(results.stream().map(Album::getId).toList());
                        runOnUiThread(() -> {
                            adapter.updateList(fullData);
                        });
                    }).start();
                }
            }
        });
    }

    private void openAlbumDetails(AlbumWithArtists album) {
        Intent i = new Intent(this, AlbumDetailsActivity.class);
        i.putExtra(EXTRA_ALBUM_ID_TO_ALBUM_DETAILS, album.getAlbum().getId());
        startActivity(i);
    }
}
