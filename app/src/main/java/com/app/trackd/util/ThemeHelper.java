package com.app.trackd.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.app.trackd.R;

public class ThemeHelper {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_DARK = "dark_theme";

    public static void applyTheme(Activity activity) {
        if (isDarkTheme(activity)) {
            activity.setTheme(R.style.Theme_VintageTrackD_Dark);
        } else {
            activity.setTheme(R.style.Theme_VintageTrackD);
        }
    }

    public static void toggleTheme(Activity activity) {
        boolean dark = !isDarkTheme(activity);
        setDarkTheme(activity, dark);

        Log.d("ThemeHelper", "Toggling theme, dark: " + dark);
        Log.d("ThemeHelper", "RESTART!");

        Intent intent = activity.getPackageManager()
                .getLaunchIntentForPackage(activity.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    public static void setDarkTheme(Context context, boolean dark) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DARK, dark).commit();
    }

    public static boolean isDarkTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Log.d("ThemeHelper", "isDarkTheme: " + prefs.getBoolean(KEY_DARK, false));
        return prefs.getBoolean(KEY_DARK, false);
    }
}
