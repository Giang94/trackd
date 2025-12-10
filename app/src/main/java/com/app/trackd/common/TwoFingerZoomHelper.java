package com.app.trackd.common;

import android.app.Activity;
import android.content.Intent;

import com.app.trackd.activity.MatchAlbumActivity;

public class TwoFingerZoomHelper {

    public static void enableTwoFingerZoom(Activity activity) {
        TwoFingerZoomLayout layout = new TwoFingerZoomLayout(activity);
        layout.attachToActivity(activity);

        layout.setOnZoomOutListener(() -> {
            Intent intent = new Intent(activity, MatchAlbumActivity.class);
            activity.startActivity(intent);
        });
    }
}
