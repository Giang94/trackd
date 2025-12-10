package com.app.trackd.common;

import android.app.Activity;
import android.content.Intent;

import com.app.trackd.activity.AddAlbumActivity;

public class TwoFingerDoubleTapHelper {

    public static void enableTwoFingerDoubleTap(Activity activity) {
        TwoFingerDoubleTapLayout layout = new TwoFingerDoubleTapLayout(activity);
        layout.attachToActivity(activity);

        layout.setOnTwoFingerDoubleTapListener(() -> {
            Intent intent = new Intent(activity, AddAlbumActivity.class);
            activity.startActivity(intent);
        });
    }
}
