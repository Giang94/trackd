package com.app.trackd.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.app.trackd.model.Album;

import java.util.List;

@Dao
public interface AlbumDao {

    @Insert
    void insert(Album album);

    @Query("SELECT * FROM Album ORDER BY id DESC LIMIT 10")
    List<Album> getRecentAlbums();

    @Query("SELECT * FROM Album")
    List<Album> getAllAlbums();
}