package com.app.trackd.common;

import android.util.Log;

public class OpenCVLoader {

    public static boolean init() {
        if (!org.opencv.android.OpenCVLoader.initLocal()) {
            Log.e("OpenCV", "Unable to load OpenCV");
            return false;
        } else {
            Log.d("OpenCV", "OpenCV loaded successfully");
            return true;
        }
    }
}
