package com.app.trackd.common;

import android.app.Activity;
import android.content.Intent;

import com.app.trackd.activity.CameraActivity;

public class LongPressHelper {

    public static void enableLongPress(Activity activity) {
        LongPressLayout layout = new LongPressLayout(activity);
        layout.attachToActivity(activity);

        layout.setOnLongPressListener(() -> {
            Intent intent = new Intent(activity, CameraActivity.class);
            activity.startActivity(intent);
        });
    }
}
