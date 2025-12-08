package com.app.trackd.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import android.content.Context;

import com.app.trackd.dao.IAlbumArtistDao;
import com.app.trackd.dao.IAlbumDao;
import com.app.trackd.dao.IArtistDao;
import com.app.trackd.model.Album;
import com.app.trackd.model.Artist;
import com.app.trackd.model.ref.AlbumArtistCrossRef;

@Database(entities = {Album.class, Artist.class, AlbumArtistCrossRef.class}, version = 2, exportSchema = false)
@TypeConverters({EmbeddingConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract IAlbumDao albumDao();

    public abstract IArtistDao artistDao();

    public abstract IAlbumArtistDao albumArtistDao();

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
