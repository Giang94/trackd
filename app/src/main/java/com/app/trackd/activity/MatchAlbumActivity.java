package com.app.trackd.activity;

import static android.view.View.GONE;
import static com.app.trackd.util.ImageUtils.rotateBitmap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.trackd.R;
import com.app.trackd.adapter.MatchAlbumListAdapter;
import com.app.trackd.common.OpenCVLoader;
import com.app.trackd.database.AppDatabase;
import com.app.trackd.matcher.TFPhotoMatcher;
import com.app.trackd.model.Album;
import com.app.trackd.util.ImageUtils;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class MatchAlbumActivity extends AppCompatActivity {

    private PreviewView previewView;
    private RecyclerView recyclerView;
    private ImageButton btnTakePhoto;
    private ImageButton btnPickPhoto;
    private TextView tvMatchesResults;
    private Bitmap latestBitmap;
    private boolean isPhotoTaken = false;

    private List<Album> albums;
    private MatchAlbumListAdapter matchAlbumListAdapter;
    private TFPhotoMatcher tfPhotoMatcher;
    private AppDatabase db;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    Log.d("CameraActivity", "Camera permission denied");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_album);

        OpenCVLoader.init();
        db = AppDatabase.get(this);

        loadAlbumsAndInitMatcher();
        initViews();
        setupRecyclerView();
        setupGalleryButton();
        setupTakePhotoButton();
        checkCameraPermissionAndStart();
    }

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void initViews() {
        previewView = findViewById(R.id.cameraPreview);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnTakePhoto.setImageResource(R.drawable.ic_camera);
        tvMatchesResults = findViewById(R.id.tvMatchesResults);
        recyclerView = findViewById(R.id.recyclerViewMatches);
    }

    private void setupRecyclerView() {
        int fullCount = albums.size();
        GridLayoutManager glm = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(glm);
        matchAlbumListAdapter = new MatchAlbumListAdapter(new ArrayList<>());
        recyclerView.setAdapter(matchAlbumListAdapter);
    }

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        Bitmap bitmap = ImageUtils.uriToBitmap(this, uri);

                        // Show photo on screen
                        ImageView imageCaptured = findViewById(R.id.imageCaptured);
                        imageCaptured.setImageBitmap(bitmap);
                        imageCaptured.setVisibility(View.VISIBLE);
                        previewView.setVisibility(View.GONE);

                        // Stop camera mode
                        isPhotoTaken = true;
                        btnTakePhoto.setImageResource(R.drawable.ic_redo);

                        // Match
                        matchAndShow(bitmap);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

    private void setupGalleryButton() {
        btnPickPhoto = findViewById(R.id.btnPickFromGallery);  // your new button

        btnPickPhoto.setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });
    }


    private void setupTakePhotoButton() {
        ImageView imageCaptured = findViewById(R.id.imageCaptured);

        btnTakePhoto.setOnClickListener(v -> {
            if (!isPhotoTaken) {
                if (latestBitmap != null) {
                    // Show captured photo
                    imageCaptured.setImageBitmap(latestBitmap);
                    imageCaptured.setVisibility(View.VISIBLE);
                    previewView.setVisibility(GONE);

                    btnTakePhoto.setImageResource(R.drawable.ic_redo);
                    isPhotoTaken = true;

                    matchAndShow(latestBitmap);
                }
            } else {
                // Retake
                imageCaptured.setVisibility(GONE);
                previewView.setVisibility(View.VISIBLE);

                tvMatchesResults.setVisibility(GONE);
                recyclerView.setVisibility(View.GONE);

                btnTakePhoto.setImageResource(R.drawable.ic_camera);
                isPhotoTaken = false;

                startCamera();
                matchAlbumListAdapter.updateData(new ArrayList<>());
            }
        });
    }

    private void loadAlbumsAndInitMatcher() {
        albums = db.albumDao().getAllAlbums();
        // photoMatcher = new ORBPhotoMatcher();
        tfPhotoMatcher = new TFPhotoMatcher(this);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(getExecutor(), image -> {
                    if (!isPhotoTaken) {
                        latestBitmap = rotateBitmap(ImageUtils.bitmapFromImageProxy(image), 90);
                    }
                    image.close();
                });


                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, getExecutor());
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    private void matchAndShow(Bitmap bitmap) {
        Log.d("MATCHING", "Selected bitmap " + bitmap.describeContents());
        if (albums == null || albums.isEmpty()) {
            tvMatchesResults.setText("No matches found");
            tvMatchesResults.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        // Use photoMatcher to find top matches
        List<Album> topMatches = tfPhotoMatcher.findTopMatches(bitmap, albums, 5);
        if (topMatches.isEmpty()) {
            tvMatchesResults.setText("No matches found");
            tvMatchesResults.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvMatchesResults.setText("Possible Matches (" + topMatches.size() + ")");
            tvMatchesResults.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        matchAlbumListAdapter.updateData(topMatches);
    }
}
