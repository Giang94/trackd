package com.app.trackd.util;

import android.content.Context;

public class SizeUtils {

    public static int dpToPx(Context context, int dp) {
        float scale = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * scale);
    }
}
