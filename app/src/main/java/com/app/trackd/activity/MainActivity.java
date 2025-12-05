package com.app.trackd.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.app.trackd.R;
import com.app.trackd.adapter.RecentAdapter;
import com.app.trackd.model.Album;
import com.app.trackd.service.AlbumService;
import com.google.android.material.chip.Chip;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView tvTotalItems, tvVinyl, tvCds, tvCollections;
    ImageView ivProfile;
    Button btnOpenCamera;
    RecyclerView rvRecent;
    RecentAdapter adapter;
    List<Album> albums;
    AlbumService albumService;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupGestures();
        initViews();
        setupChips();
        setupData();
    }

    private void setupGestures() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                openAddAlbumActivity();
                return true;
            }
        });

        View root = findViewById(android.R.id.content);
        root.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void initViews() {
        tvTotalItems = findViewById(R.id.tvTotalItems);
        tvVinyl = findViewById(R.id.tvVinyl);
        tvCds = findViewById(R.id.tvCds);
        tvCollections = findViewById(R.id.tvCollections);
        rvRecent = findViewById(R.id.rvRecent);
        ivProfile = findViewById(R.id.ivProfile);
        btnOpenCamera = findViewById(R.id.btnOpenCamera);

        btnOpenCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        });
    }

    private void setupChips() {
        Chip chipArtist = findViewById(R.id.chipArtist);
        chipArtist.setOnClickListener(v -> {
            // TODO: browse by artist
        });
    }

    private void setupData() {
        albumService = new AlbumService(this);
        albums = albumService.getRecent();

        setupRecycler();
        updateStats();
        populateCollections();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }

    private void refreshData() {
        albums = albumService.getRecent();
        adapter.updateData(albums);  // you must add this method in adapter
        updateStats();
    }

    private void setupRecycler() {
        adapter = new RecentAdapter(albums, album -> {
        });

        GridLayoutManager glm = new GridLayoutManager(this, 2); // 2 cards per row
        rvRecent.setLayoutManager(glm);
        rvRecent.setAdapter(adapter);
    }

    private void updateStats() {
        int total = albums.size();
        int vinyl = 0;
        int cds = 0;
        for (Album a : albums) {
            if ("Vinyl".equalsIgnoreCase(a.getFormat().toString())) vinyl++;
            if ("CD".equalsIgnoreCase(a.getFormat().toString())) cds++;
        }
        tvTotalItems.setText(String.valueOf(total));
        tvVinyl.setText(String.valueOf(vinyl));
        tvCds.setText(String.valueOf(cds));
        tvCollections.setText("3"); // placeholder
    }

    private void populateCollections() {
        LinearLayout ll = findViewById(R.id.llCollections);
        String[] names = new String[]{"Classical Essentials", "Opera Collection", "J-Pop Favorites", "Limited Editions"};
        for (String s : names) {
            TextView t = new TextView(this);
            t.setText("• " + s);
            t.setTextSize(15f);
            t.setPadding(0, 8, 0, 8);
            ll.addView(t);
        }
    }

    private void openAddAlbumActivity() {
        Intent i = new Intent(MainActivity.this, AddAlbumActivity.class);
        startActivity(i);
    }
}
