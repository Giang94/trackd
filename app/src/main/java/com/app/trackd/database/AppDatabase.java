package com.app.trackd.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import android.content.Context;

import com.app.trackd.dao.AlbumDao;
import com.app.trackd.model.Album;

@Database(entities = {Album.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract AlbumDao albumDao();

    private static AppDatabase instance;

    public static synchronized AppDatabase get(Context context) {
        if (instance == null) {

            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "trackd.db"
                    )
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}
