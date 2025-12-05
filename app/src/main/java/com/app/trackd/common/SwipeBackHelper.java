package com.app.trackd.common;

import android.app.Activity;

public class SwipeBackHelper {
    public static void enableSwipeBack(Activity activity) {
        SwipeBackLayout swipeBackLayout = new SwipeBackLayout(activity);
        swipeBackLayout.attachToActivity(activity);
    }
}
