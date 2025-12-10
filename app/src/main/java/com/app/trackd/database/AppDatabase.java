package com.app.trackd.database;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import android.content.Context;

import com.app.trackd.dao.IAlbumArtistDao;
import com.app.trackd.dao.IAlbumDao;
import com.app.trackd.dao.IArtistDao;
import com.app.trackd.model.Album;
import com.app.trackd.model.Artist;
import com.app.trackd.model.ref.AlbumArtistCrossRef;

@Database(entities = {Album.class, Artist.class, AlbumArtistCrossRef.class}, version = 3, exportSchema = false)
@TypeConverters({EmbeddingConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract IAlbumDao albumDao();

    public abstract IArtistDao artistDao();

    public abstract IAlbumArtistDao albumArtistDao();

    private static AppDatabase instance;

    static final Migration MIGRATION_2_TO_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add spotifyUrl column with default empty string (or null)
            database.execSQL("ALTER TABLE Album ADD COLUMN spotifyUrl TEXT");
        }
    };

    public static synchronized AppDatabase get(Context context) {
        if (instance == null) {

            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "trackd.db"
                    )
                    .allowMainThreadQueries()
                    .addMigrations(MIGRATION_2_TO_3)
                    .build();
        }
        return instance;
    }
}
