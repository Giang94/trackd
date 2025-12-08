package com.app.trackd.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import com.app.trackd.activity.CameraActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageUtils {

    private static final int MAX_SIZE = 360;

    public static String toBase64(Bitmap bitmap) {
        if (bitmap == null) return null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos); // or JPEG if you prefer smaller size
        byte[] bytes = baos.toByteArray();

        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    public static Bitmap toBitmap(String base64String) {
        if (base64String == null || base64String.isEmpty()) return null;

        byte[] bytes = Base64.decode(base64String, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    public static Bitmap bitmapFromImageProxy(ImageProxy image) {
        Image img = image.getImage();
        if (img == null) return null;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuvImage = new YuvImage(
                toByteArray(img),
                ImageFormat.NV21,
                img.getWidth(),
                img.getHeight(),
                null
        );
        yuvImage.compressToJpeg(
                new android.graphics.Rect(0, 0, img.getWidth(), img.getHeight()),
                100,
                out
        );
        byte[] bytes = out.toByteArray();
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public static byte[] toByteArray(Image image) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Only YUV_420_888 images are supported.");
        }

        Image.Plane[] planes = image.getPlanes();
        int width = image.getWidth();
        int height = image.getHeight();

        int ySize = planes[0].getBuffer().remaining();
        int uSize = planes[1].getBuffer().remaining();
        int vSize = planes[2].getBuffer().remaining();

        byte[] nv21 = new byte[width * height + 2 * (width / 2) * (height / 2)];

        // Copy Y plane
        planes[0].getBuffer().get(nv21, 0, ySize);

        // UV planes are interleaved in NV21 format
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();
        int uvPos = width * height;

        uBuffer.rewind();
        vBuffer.rewind();

        int rowStride = planes[1].getRowStride();
        int pixelStride = planes[1].getPixelStride();

        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                int uIndex = row * rowStride + col * pixelStride;
                int vIndex = row * planes[2].getRowStride() + col * planes[2].getPixelStride();

                nv21[uvPos++] = vBuffer.get(vIndex); // V
                nv21[uvPos++] = uBuffer.get(uIndex); // U
            }
        }

        return nv21;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        if (rotationDegrees == 0) return bitmap;
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap resizeSquare(Bitmap bitmap, int size) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newSize = Math.min(width, height); // crop to square

        // Crop center square
        int x = (width - newSize) / 2;
        int y = (height - newSize) / 2;
        Bitmap squareBitmap = Bitmap.createBitmap(bitmap, x, y, newSize, newSize);

        // Resize to desired size
        return Bitmap.createScaledBitmap(squareBitmap, size, size, true);
    }

    public static Bitmap resizeBitmapKeepRatio(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();

        float ratio = (float) width / (float) height;

        int newWidth;
        int newHeight;

        if (ratio > 1) {
            newWidth = MAX_SIZE;
            newHeight = (int) (MAX_SIZE / ratio);
        } else {
            newHeight = MAX_SIZE;
            newWidth = (int) (MAX_SIZE * ratio);
        }

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    // Convert bitmap to grayscale
    public static Bitmap toGrayscale(Bitmap bitmap) {
        Bitmap grayBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(grayBitmap);
        android.graphics.Paint paint = new android.graphics.Paint();
        android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();
        cm.setSaturation(0); // 0 = grayscale
        android.graphics.ColorMatrixColorFilter f = new android.graphics.ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return grayBitmap;
    }

    public static Bitmap uriToBitmap(Context context, Uri uri) throws IOException {
        // Step 1: Load bitmap from URI
        ImageDecoder.Source src = ImageDecoder.createSource(context.getContentResolver(), uri);
        Bitmap rawBitmap = ImageDecoder.decodeBitmap(src);
        // Step 2: Ensure ARGB_8888 format
        if (rawBitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            rawBitmap = rawBitmap.copy(Bitmap.Config.ARGB_8888, true);
        }

        return rawBitmap;
    }
}