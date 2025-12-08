package com.app.trackd.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.app.trackd.model.Artist;

import java.util.List;

@Dao
public interface IArtistDao {
    @Query("SELECT * FROM Artist WHERE normalizedName = :normalizedName LIMIT 1")
    Artist findByNormalizedName(String normalizedName);

    @Insert
    long insert(Artist artist);

    @Query("SELECT displayName FROM Artist ORDER BY displayName ASC")
    List<String> getAllArtistNames();

}