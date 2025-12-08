package com.app.trackd.activity;

import static com.app.trackd.util.ImageUtils.resizeBitmapKeepRatio;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.trackd.R;
import com.app.trackd.adapter.ArtistSuggestionAdapter;
import com.app.trackd.common.OpenCVLoader;
import com.app.trackd.common.SwipeBackHelper;
import com.app.trackd.database.AlbumFormatConverter;
import com.app.trackd.database.AppDatabase;
import com.app.trackd.matcher.TFPhotoMatcher;
import com.app.trackd.model.Album;
import com.app.trackd.model.Artist;
import com.app.trackd.model.enums.AlbumFormat;
import com.app.trackd.model.ref.AlbumArtistCrossRef;
import com.app.trackd.util.ImageUtils;
import com.app.trackd.util.StringUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Objects;

public class AddAlbumActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 101;
    private static final String DISCOGS_TOKEN = "YgPKoMgqNODZdUYKraucDYuTHZwRSyuYtxQLJmhI";

    private EditText etTitle, etYear;
    private AutoCompleteTextView etArtist;
    private Spinner spFormat;
    private ImageView ivCover;
    private Button btnSave;
    private TextInputLayout lyBarcode;
    private String coverBase64 = "";
    private Bitmap currentBitmap;

    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Intent> barcodeLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_album);

        SwipeBackHelper.enableSwipeBack(this);
        OpenCVLoader.init();

        initViews();
        initDropdowns();
        initImagePicker();
        initBarcodeScanner();
        loadArtistSuggestions();
        setupListeners();
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etArtist = findViewById(R.id.etArtist);
        etYear = findViewById(R.id.etYear);
        lyBarcode = findViewById(R.id.lyBarcode);
        spFormat = findViewById(R.id.spFormat);
        ivCover = findViewById(R.id.ivCover);
        btnSave = findViewById(R.id.btnSave);
    }

    private void initDropdowns() {
        setupFormatDropdown();
    }

    private void initImagePicker() {
        setupImagePicker();
    }

    private void initBarcodeScanner() {
        setupBarcodeScanner();
    }

    private void setupListeners() {
        // open barcode scan
        lyBarcode.setEndIconOnClickListener(v -> {
            Intent intent = new Intent(this, BarcodeScanActivity.class);
            barcodeLauncher.launch(intent);
        });

        // save
        btnSave.setOnClickListener(v -> saveAlbumWithArtists());

        // handle barcode input action
        EditText etBarcode = lyBarcode.getEditText();

        etBarcode.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                String barcode = etBarcode.getText().toString().trim();
                if (!barcode.isEmpty()) {
                    fetchDiscogsData(barcode);
                }
                return true;
            }
            return false;
        });
    }

    private void setupBarcodeScanner() {
        barcodeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String barcode = result.getData().getStringExtra("SCAN_RESULT");
                        Objects.requireNonNull(lyBarcode.getEditText()).setText(barcode);
                        fetchDiscogsData(barcode);
                    }
                }
        );
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

    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri selectedImageUri = data.getData();
                            // Set the image to your ImageView, e.g., with Glide
                            Glide.with(this).load(selectedImageUri).into(ivCover);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri imgUri = data.getData();
            try {
                InputStream is = getContentResolver().openInputStream(imgUri);
                Bitmap bitmap = BitmapFactory.decodeStream(is);

                ivCover.setImageBitmap(bitmap);
                Bitmap resizedBitmap = resizeBitmapKeepRatio(bitmap);
                coverBase64 = bitmapToBase64(resizedBitmap);
                currentBitmap = resizedBitmap;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String bitmapToBase64(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Bitmap scaled = Bitmap.createScaledBitmap(bm, 300, 300, true);
        scaled.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private void saveAlbumWithArtists() {
        String title = etTitle.getText().toString().trim();
        String artistInput = etArtist.getText().toString().trim();
        String yearStr = etYear.getText().toString().trim();

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

        TFPhotoMatcher tfPhotoMatcher = new TFPhotoMatcher(this);
        float[] embedding = tfPhotoMatcher.getEmbedding(currentBitmap);

        String[] artistNames = artistInput.split(",");

        if (coverBase64 == null || coverBase64.isEmpty()) {
            Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cover_placeholder);
            Bitmap resizedBitmap = resizeBitmapKeepRatio(defaultBitmap);
            coverBase64 = ImageUtils.toBase64(resizedBitmap);
        }

        new Thread(() -> {
            AppDatabase db = AppDatabase.get(this);

            // 1. Insert album
            Album album = new Album(0, title, parsedYear.value, format, coverBase64, embedding);
            long albumId = db.albumDao().insert(album);

            for (String name : artistNames) {
                String trimmedName = name.trim();
                if (trimmedName.isEmpty()) continue;

                // 2. Normalize for comparison
                String normalized = StringUtils.normalize(trimmedName);

                // 3. Check if artist exists
                Artist artist = db.artistDao().findByNormalizedName(normalized);
                long artistId;
                if (artist == null) {
                    // Insert new artist
                    Artist newArtist = new Artist(0, trimmedName, normalized);
                    artistId = db.artistDao().insert(newArtist);
                } else {
                    artistId = artist.getId();
                }

                // 4. Insert cross reference
                AlbumArtistCrossRef crossRef = new AlbumArtistCrossRef(albumId, artistId);
                db.albumArtistDao().insert(crossRef);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "Album added!", Toast.LENGTH_SHORT).show();
                finish();
            });

        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchDiscogsData(String barcode) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.discogs.com/database/search?barcode=" + barcode + "&token=" + DISCOGS_TOKEN);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    String response = sb.toString();
                    JSONObject json = new JSONObject(response);
                    JSONArray results = json.getJSONArray("results");
                    if (results.length() > 0) {
                        JSONObject item = results.getJSONObject(0);
                        runOnUiThread(() -> {
                            etTitle.setText(item.optString("title", ""));
                            etArtist.setText(item.optString("artist", ""));
                            etYear.setText(item.optString("year", ""));
                            // Format mapping
                            String format = item.optJSONArray("format") != null
                                    ? item.optJSONArray("format").optString(0) : "CD";
                            Log.d("DISCOGS", "Fetched Title: " + item.optString("title", "") + " format: " + format);
                            spFormat.setSelection(getFormatIndex(format));

                            // Cover image
                            String coverUrl = item.optString("cover_image");
                            if (!coverUrl.isEmpty()) {
                                Glide.with(this)
                                        .asBitmap()
                                        .load(coverUrl)
                                        .into(new CustomTarget<Bitmap>() {
                                            @Override
                                            public void onResourceReady(@NonNull Bitmap bitmap,
                                                                        @Nullable Transition<? super Bitmap> transition) {
                                                ivCover.setImageBitmap(bitmap);
                                                Bitmap resizedBitmap = resizeBitmapKeepRatio(bitmap);
                                                coverBase64 = bitmapToBase64(resizedBitmap);
                                                currentBitmap = resizedBitmap;
                                            }

                                            @Override
                                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                            }
                                        });
                            }
                        });
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(this, "No data found for this barcode", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Failed to fetch data", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error fetching data", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private int getFormatIndex(String format) {
        switch (format.toUpperCase()) {
            case "CASSETTE":
                return 1;
            case "VINYL":
                return 2;
            default:
                return 0; // CD
        }
    }

    private void loadArtistSuggestions() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.get(this);
            List<String> names = db.artistDao().getAllArtistNames();

            runOnUiThread(() -> {
                ArtistSuggestionAdapter adapter = new ArtistSuggestionAdapter(this, names);

                MultiAutoCompleteTextView actv = (MultiAutoCompleteTextView) etArtist;
                actv.setAdapter(adapter);
                actv.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
            });
        }).start();
    }
}
