package com.app.trackd.common;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import com.app.trackd.R;
import com.app.trackd.activity.MatchAlbumActivity;

public class TwoFingerZoomHelper {

    public static void enableTwoFingerZoom(Activity activity) {
        View decor = activity.getWindow().getDecorView();

        if (decor.getTag(R.id.two_finger_zoom_tag) != null) return;
        decor.setTag(R.id.two_finger_zoom_tag, true);

        TwoFingerZoomLayout layout = new TwoFingerZoomLayout(activity);
        layout.attachToActivity(activity);

        layout.setOnZoomOutListener(() -> {
            activity.startActivity(new Intent(activity, MatchAlbumActivity.class));
        });
    }

    public static void cleanup(Activity activity) {
        ViewGroup contentParent = activity.findViewById(android.R.id.content);

        if (contentParent.getChildCount() > 0 &&
                contentParent.getChildAt(0) instanceof TwoFingerZoomLayout) {

            TwoFingerZoomLayout zoom = (TwoFingerZoomLayout) contentParent.getChildAt(0);
            View child = zoom.getChildAt(0);

            zoom.removeAllViews();
            contentParent.removeView(zoom);

            contentParent.addView(child);
        }
    }
}
