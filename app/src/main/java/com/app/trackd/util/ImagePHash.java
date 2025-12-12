package com.app.trackd.util;

import android.graphics.Bitmap;
import android.graphics.Color;

public class ImagePHash {

    private final int size = 32; // resize to 32x32
    private final int smallerSize = 8; // DCT size

    public ImagePHash() {
    }

    public static int hammingDistance(String hash1, String hash2) {
        if (hash1.length() != hash2.length()) return -1;
        int counter = 0;
        for (int i = 0; i < hash1.length(); i++) {
            if (hash1.charAt(i) != hash2.charAt(i)) counter++;
        }
        return counter;
    }

    public String getHash(Bitmap img) {
        Bitmap resized = Bitmap.createScaledBitmap(img, size, size, false);
        double[][] vals = new double[size][size];

        // Grayscale
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int color = resized.getPixel(x, y);
                vals[x][y] = (Color.red(color) + Color.green(color) + Color.blue(color)) / 3.0;
            }
        }

        double[][] dctVals = applyDCT(vals);

        double total = 0;
        for (int x = 0; x < smallerSize; x++) {
            for (int y = 0; y < smallerSize; y++) {
                if (x != 0 || y != 0)
                    total += dctVals[x][y];
            }
        }

        double avg = total / (smallerSize * smallerSize - 1);

        StringBuilder hash = new StringBuilder();
        for (int x = 0; x < smallerSize; x++) {
            for (int y = 0; y < smallerSize; y++) {
                if (x != 0 || y != 0) {
                    hash.append(dctVals[x][y] > avg ? "1" : "0");
                }
            }
        }
        return hash.toString();
    }

    private double[][] applyDCT(double[][] f) {
        int N = size;
        double[][] F = new double[N][N];
        double c1 = Math.sqrt(2.0 / N);
        for (int u = 0; u < N; u++) {
            for (int v = 0; v < N; v++) {
                double sum = 0.0;
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        sum += f[i][j] *
                                Math.cos((2 * i + 1) * u * Math.PI / (2 * N)) *
                                Math.cos((2 * j + 1) * v * Math.PI / (2 * N));
                    }
                }
                double cu = (u == 0) ? 1 / Math.sqrt(2) : 1.0;
                double cv = (v == 0) ? 1 / Math.sqrt(2) : 1.0;
                F[u][v] = c1 * cu * cv * sum;
            }
        }
        return F;
    }
}
