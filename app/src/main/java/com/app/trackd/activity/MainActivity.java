package com.app.trackd.activity;

import static com.app.trackd.activity.AlbumDetailsActivity.EXTRA_ALBUM_ID_TO_ALBUM_DETAILS;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.trackd.R;
import com.app.trackd.adapter.AlbumListAdapter;
import com.app.trackd.adapter.RecentAlbumListAdapter;
import com.app.trackd.common.DoubleTapHelper;
import com.app.trackd.common.TwoFingerZoomHelper;
import com.app.trackd.model.Album;
import com.app.trackd.model.AlbumWithArtists;
import com.app.trackd.service.AlbumService;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    MaterialCardView cvTotal;
    TextView tvTotalItems, tvVinyl, tvCds;
    ImageView ivProfile;
    RecyclerView rvRecent;
    RecentAlbumListAdapter adapter;
    List<Album> albums;
    AlbumService albumService;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TwoFingerZoomHelper.enableTwoFingerZoom(this);
        DoubleTapHelper.enableDoubleTap(this);

        initViews();
        setupChips();
        setupData();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void initViews() {
        cvTotal = findViewById(R.id.cvTotal);
        tvTotalItems = findViewById(R.id.tvTotalItems);
        tvVinyl = findViewById(R.id.tvVinyl);
        tvCds = findViewById(R.id.tvCds);
        rvRecent = findViewById(R.id.rvRecent);
        ivProfile = findViewById(R.id.ivProfile);

        cvTotal.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, AlbumListActivity.class);
            startActivity(i);
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
        adapter.updateData(albums);
        updateStats();
    }

    private void setupRecycler() {
        int fullCount = albums.size();

        adapter = new RecentAlbumListAdapter(albums, fullCount, album -> {
            openAlbumDetails(album);
        });

        rvRecent.setLayoutManager(new GridLayoutManager(this, 2));
        rvRecent.setAdapter(adapter);

        adapter.setShowAllCallback(() -> {
            Intent i = new Intent(MainActivity.this, AlbumListActivity.class);
            startActivity(i);
        });
    }

    private void updateStats() {
        int total = albums.size();
        int vinyl = 0;
        int cds = 0;
        for (Album a : albums) {
            String formatStr = a.getFormat().toString();
            if (formatStr.toLowerCase().startsWith("vinyl")) vinyl++;
            if (formatStr.equalsIgnoreCase("cd")) cds++;
        }
        tvTotalItems.setText(String.valueOf(total));
        tvVinyl.setText(String.valueOf(vinyl));
        tvCds.setText(String.valueOf(cds));
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

    private void openAlbumDetails(Album album) {
        Intent i = new Intent(this, AlbumDetailsActivity.class);
        i.putExtra(EXTRA_ALBUM_ID_TO_ALBUM_DETAILS, album.getId());
        startActivity(i);
    }
}
