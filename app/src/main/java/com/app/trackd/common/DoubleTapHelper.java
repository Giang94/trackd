package com.app.trackd.common;

import android.app.Activity;
import android.content.Intent;

import com.app.trackd.activity.AddAlbumActivity;

public class DoubleTapHelper {

    public static void enableDoubleTap(Activity activity) {
        DoubleTapLayout layout = new DoubleTapLayout(activity);
        layout.attachToActivity(activity);

        layout.setOnDoubleTapListener(() -> {
            Intent intent = new Intent(activity, AddAlbumActivity.class);
            activity.startActivity(intent);
        });
    }
}