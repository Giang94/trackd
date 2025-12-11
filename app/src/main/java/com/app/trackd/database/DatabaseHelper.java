package com.app.trackd.database;

import static com.app.trackd.database.AppDatabase.DATABASE_VERSION;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "trackd.db";

    private static volatile DatabaseHelper instance;
    private final Context appContext;

    private DatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        this.appContext = context.getApplicationContext();
    }

    public static DatabaseHelper get(Context context) {
        if (instance == null) {
            synchronized (DatabaseHelper.class) {
                if (instance == null) {
                    instance = new DatabaseHelper(context);
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void exportDatabase(Uri destUri) {
        try {
            SQLiteDatabase db = getReadableDatabase();

            File temp = new File(appContext.getCacheDir(), "export_temp.db");
            db.execSQL("VACUUM INTO '" + temp.getAbsolutePath() + "'");

            try (InputStream in = new FileInputStream(temp);
                 OutputStream out = appContext.getContentResolver().openOutputStream(destUri)) {

                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            }

            temp.delete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void importDatabase(Uri srcUri) {
        try {
            File dbFile = appContext.getDatabasePath(DATABASE_NAME);

            // close helper first
            close();

            try (InputStream in = appContext.getContentResolver().openInputStream(srcUri);
                 OutputStream out = new FileOutputStream(dbFile, false)) {

                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            }

            // delete WAL + SHM to avoid corruption
            new File(dbFile.getAbsolutePath() + "-wal").delete();
            new File(dbFile.getAbsolutePath() + "-shm").delete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
