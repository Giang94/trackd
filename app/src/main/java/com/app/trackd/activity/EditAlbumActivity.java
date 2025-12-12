package com.app.trackd.activity;

import static com.app.trackd.util.ImageUtils.resizeBitmapKeepRatio;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.app.trackd.R;
import com.app.trackd.database.AlbumFormatConverter;
import com.app.trackd.database.AppDatabase;
import com.app.trackd.matcher.TFPhotoMatcher;
import com.app.trackd.model.Album;
import com.app.trackd.model.AlbumWithArtists;
import com.app.trackd.model.Artist;
import com.app.trackd.model.enums.AlbumFormat;
import com.app.trackd.model.ref.AlbumArtistCrossRef;
import com.app.trackd.util.ImageUtils;
import com.app.trackd.util.SpotifyUrlHelper;
import com.app.trackd.util.StringUtils;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

public class EditAlbumActivity extends AppCompatActivity {

    public static final String EXTRA_ALBUM_ID = "albumId";
    public static final String EXTRA_UPDATED_ALBUM_ID = "updatedAlbumId";
    private ScrollView scrollView;
    private ImageView ivCover;
    private EditText etTitle, etYear, etSpotifyUrl;
    private MultiAutoCompleteTextView etArtist;
    private Spinner spFormat;
    private Button btnSave;
    private AppDatabase db;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private Bitmap currentBitmap = null;
    private String coverBase64 = null;
    private boolean isCoverChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_album);

        db = AppDatabase.get(this);

        ivCover = findViewById(R.id.ivCover);
        etTitle = findViewById(R.id.etTitle);
        etArtist = findViewById(R.id.etArtist);
        etYear = findViewById(R.id.etYear);
        spFormat = findViewById(R.id.spFormat);
        etSpotifyUrl = findViewById(R.id.etSpotifyUrl);
        btnSave = findViewById(R.id.btnSave);

        scrollView = findViewById(R.id.scrollView);
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            View focused = getCurrentFocus();
            if (focused != null) {
                scrollView.smoothScrollTo(0, focused.getBottom());
            }
        });

        setupFormatDropdown();

        // Example: Get album data from intent
        long albumId = getIntent().getLongExtra(EXTRA_ALBUM_ID, 0);
        if (albumId == 0) {
            Toast.makeText(this, "Invalid album ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        AlbumWithArtists albumWithArtists = db.albumDao().getAlbumWithArtistsById(albumId);
        populateAlbumData(albumWithArtists);

        btnSave.setOnClickListener(v -> {
            // Save album logic here
            saveAlbum(albumId);
            Toast.makeText(this, "Album updated!", Toast.LENGTH_SHORT).show();
        });

        setupImagePicker();
        setupEditTextFields();
    }

    private void setupEditTextFields() {
        etTitle.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        etTitle.setRawInputType(InputType.TYPE_CLASS_TEXT);

        etArtist.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        etArtist.setRawInputType(InputType.TYPE_CLASS_TEXT);

        etSpotifyUrl.setImeOptions(EditorInfo.IME_ACTION_DONE);
        etSpotifyUrl.setRawInputType(InputType.TYPE_CLASS_TEXT);
    }

    private void saveAlbum(long albumId) {
        String title = etTitle.getText().toString().trim();
        String artistInput = etArtist.getText().toString().trim();
        String yearStr = etYear.getText().toString().trim();
        String spotifyFullUrl = etSpotifyUrl.getText().toString().trim();
        String spotifyUrl = SpotifyUrlHelper.normalize(spotifyFullUrl);

        if (title.isEmpty() || artistInput.isEmpty()) {
            Toast.makeText(this, "Should provide album name and artist", Toast.LENGTH_SHORT).show();
            return;
        }

        var parsedYear = new Object() {
            int value = 0;
        };
        try {
            parsedYear.value = Integer.parseInt(yearStr);
        } catch (NumberFormatException ignored) {
        }

        String formatText = AlbumFormatConverter.mapFormatForDb(spFormat.getSelectedItem().toString());
        AlbumFormat format = AlbumFormat.valueOf(formatText);

        Album album = db.albumDao().getAlbumById(albumId);
        album.setTitle(title);
        album.setYear(parsedYear.value);
        album.setSpotifyUrl(spotifyUrl);
        album.setFormat(format);

        // Update cover, recalculate embedding if changed
        if (isCoverChanged) {
            if (coverBase64 == null || coverBase64.isEmpty()) {
                Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cover_placeholder);
                Bitmap resizedBitmap = resizeBitmapKeepRatio(defaultBitmap);
                coverBase64 = ImageUtils.toBase64(resizedBitmap);
                currentBitmap = resizedBitmap;
            }

            TFPhotoMatcher tfPhotoMatcher = new TFPhotoMatcher(this);
            float[] embedding = tfPhotoMatcher.getEmbedding(currentBitmap);
            album.setEmbedding(embedding);
            album.setCover(coverBase64);
        }

        // Update album table
        db.albumDao().update(album);

        // Update artist relationship
        updateArtistsForAlbum(albumId);

        Toast.makeText(this, "Album updated!", Toast.LENGTH_SHORT).show();
        Intent result = new Intent();
        result.putExtra(EXTRA_UPDATED_ALBUM_ID, albumId);
        setResult(RESULT_OK, result);
        finish();
    }

    private void updateArtistsForAlbum(long albumId) {

        // 1. Parse artist names from text input
        String artistInput = etArtist.getText().toString().trim();
        if (artistInput.isEmpty()) return;

        // split by comma
        List<String> newArtistNames = Stream.of(artistInput.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        // 2. Convert names → IDs
        List<Long> newArtistIds = new java.util.ArrayList<>();

        for (String name : newArtistNames) {
            String normalized = StringUtils.normalize(name);
            Artist existing = db.artistDao().findByNormalizedName(normalized);
            long id;
            if (existing != null) {
                id = existing.getId();
            } else {
                Artist artist = new Artist(0, name, normalized);
                id = db.artistDao().insert(artist);
            }
            newArtistIds.add(id);
        }

        // 3. Get OLD artist IDs from DB
        List<Long> oldArtistIds = db.albumArtistDao().getArtistIdsForAlbum(albumId);

        // 4. Compute what to add and what to delete
        List<Long> toAdd = new java.util.ArrayList<>(newArtistIds);
        toAdd.removeAll(oldArtistIds);

        List<Long> toRemove = new java.util.ArrayList<>(oldArtistIds);
        toRemove.removeAll(newArtistIds);

        // 5. Insert new cross-refs
        for (Long id : toAdd) {
            db.albumArtistDao().insert(new AlbumArtistCrossRef(albumId, id));
        }

        // 6. Delete removed cross-refs
        for (Long id : toRemove) {
            db.albumArtistDao().deleteCrossRef(albumId, id);
        }
    }


    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imgUri = result.getData().getData();
                        try {
                            InputStream is = getContentResolver().openInputStream(imgUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(is);

                            ivCover.setImageBitmap(bitmap);
                            Bitmap resizedBitmap = resizeBitmapKeepRatio(bitmap);
                            coverBase64 = ImageUtils.toBase64(resizedBitmap);
                            currentBitmap = bitmap;
                            isCoverChanged = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        ivCover.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK);
            pickIntent.setType("image/*");
            pickImageLauncher.launch(pickIntent);
        });
    }

    private void setupFormatDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"CD", "CASSETTE", "VINYL 12\"", "VINYL 10\"", "VINYL 7\""}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFormat.setAdapter(adapter);
    }

    private void populateAlbumData(AlbumWithArtists albumWithArtists) {
        Album album = albumWithArtists.getAlbum();

        if (album.getCover() != null) {
            Bitmap coverBitmap = ImageUtils.toBitmap(album.getCover());
            ivCover.setImageBitmap(coverBitmap);
        }

        if (album.getYear() == 0) {
            etYear.setText("");
            etYear.setHint("Unknown release date");
        } else {
            etYear.setText(album.getYear() + "");
        }

        spFormat.setSelection(getFormatIndex(album.getFormat().name()));

        etTitle.setText(album.getTitle());

        List<String> artists = albumWithArtists.getArtists().stream()
                .map(Artist::getDisplayName)
                .toList();

        etArtist.setText(TextUtils.join(", ", artists));

        etSpotifyUrl.setText(SpotifyUrlHelper.toFullUrl(album.getSpotifyUrl()));
    }

    private int getFormatIndex(String format) {
        switch (format.toUpperCase()) {
            case "CASSETTE":
                return 1;
            case "VINYL":
                return 2;
            case "VINYL_10":
                return 3;
            case "VINYL_7":
                return 4;
            default:
                return 0; // CD
        }
    }
}