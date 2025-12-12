package com.app.trackd.matcher;

import static org.tensorflow.lite.support.image.ImageProcessor.Builder;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import com.app.trackd.model.Album;
import com.app.trackd.util.ImageUtils;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TFPhotoMatcher {
    private final int MODEL_INPUT_SIZE = 224; // Standard for MobileNet
    private final int EMBEDDING_SIZE = 1280; // MobileNetV2 output vector size
    private final double ACCEPTED_SCORE = 0.25;
    private Interpreter tflite;

    public TFPhotoMatcher(Context context) {
        try {
            // Load the model from assets folder
            MappedByteBuffer tfliteModel = loadModelFile(context, "model.tflite");
            Interpreter.Options options = new Interpreter.Options();
            tflite = new Interpreter(tfliteModel, options);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Main method to compare a captured Bitmap with a Base64 string from DB.
     *
     * @return similarity score (0.0 to 1.0)
     */
    public float compareCapturedWithDatabase(Bitmap capturedPhoto, String dbBase64String) {
        // 1. Convert DB Base64 to Bitmap
        Bitmap dbPhoto = ImageUtils.toBitmap(dbBase64String);

        if (dbPhoto == null) return 0.0f;

        // 2. Get embeddings (feature vectors) for both images
        float[] vectorCaptured = getEmbedding(capturedPhoto);
        float[] vectorDb = getEmbedding(dbPhoto);

        // 3. Calculate similarity
        return calculateCosineSimilarity(vectorCaptured, vectorDb);
    }

    public float[] getEmbedding(Bitmap bitmap) {
        // Load Bitmap into TensorImage
        TensorImage inputImage = new TensorImage(DataType.FLOAT32);
        inputImage.load(bitmap);

        // Pre-process: Resize to 224x224 and Normalize
        // MobileNet expects values between 0 and 1 (or -1 and 1 depending on version).
        // Common normalization: (value - mean) / std_dev
        // For [0,1] normalization, usually mean=0, std=255 if input is 0-255.
        ImageProcessor imageProcessor = new Builder()
                .add(new ResizeOp(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0.0f, 255.0f)) // Normalizes pixels to [0,1]
                .build();

        inputImage = imageProcessor.process(inputImage);

        // Output buffer to hold the embedding
        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(new int[]{1, EMBEDDING_SIZE}, DataType.FLOAT32);

        // Run Inference
        tflite.run(inputImage.getBuffer(), outputBuffer.getBuffer().rewind());

        return outputBuffer.getFloatArray();
    }

    // --- Math Logic: Cosine Similarity ---
    // Returns 1.0 for identical images, 0.0 for completely different, -1.0 for opposite.
    private float calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }

        if (normA == 0 || normB == 0) return 0.0f;

        return (float) (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    // --- Utility: Load Model ---
    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }

    public List<Album> findTopMatches(Bitmap capturedBitmap, List<Album> albums, int limitResult) {
        List<TFMatchResult> results = new ArrayList<>();
        float[] vectorCaptured = getEmbedding(capturedBitmap);

        Log.d("MATCHING", "--- Starting matching process ---");
        for (Album album : albums) {
            float similarity = this.calculateCosineSimilarity(vectorCaptured, album.getEmbedding());
            // Log.d("MATCHING", "Album: " + album.getTitle() + " - Similarity: " + similarity);
            results.add(new TFMatchResult(album, similarity));
        }
        // Sort by ascending distance (smaller = better)
        results.sort(Comparator.comparing((TFMatchResult a) -> a.similarity).reversed());

        List<Album> topMatches = new ArrayList<>();
        for (int i = 0; i < Math.min(limitResult, results.size()); i++) {
            if (results.get(i).similarity >= ACCEPTED_SCORE)
                topMatches.add(results.get(i).album);
        }
        Log.d("MATCHING", "--- Finish matching process. Processed " + albums.size() + " photo, likely matched " + topMatches.size() + " photo ---");
        return topMatches;
    }

    private static class TFMatchResult {
        Album album;
        float similarity;

        TFMatchResult(Album album, float similarity) {
            this.album = album;
            this.similarity = similarity;
        }
    }
}
