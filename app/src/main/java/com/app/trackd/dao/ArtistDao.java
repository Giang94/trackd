package com.app.trackd.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.app.trackd.model.Artist;

@Dao
public interface ArtistDao {
    @Query("SELECT * FROM Artist WHERE normalizedName = :normalizedName LIMIT 1")
    Artist findByNormalizedName(String normalizedName);

    @Insert
    long insert(Artist artist);
}