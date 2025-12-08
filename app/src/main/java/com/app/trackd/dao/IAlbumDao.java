package com.app.trackd.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.app.trackd.model.Album;
import com.app.trackd.model.AlbumWithArtists;
import com.app.trackd.model.ref.AlbumArtistCrossRef;

import java.util.List;

@Dao
public interface IAlbumDao {

    @Insert
    long insert(Album album);

    @Insert
    void insertCrossRef(AlbumArtistCrossRef ref);

    @Query("SELECT * FROM Album ORDER BY id DESC LIMIT 10")
    List<Album> getRecentAlbums();

    @Query("SELECT * FROM Album")
    List<Album> getAllAlbums();

    @Query("SELECT * FROM Album ORDER BY id DESC LIMIT :pageSize OFFSET :currentOffset")
    List<Album> getAlbumsPaged(int currentOffset, int pageSize);

    @Transaction
    @Query("SELECT * FROM Album WHERE id IN (:albumIds)")
    List<AlbumWithArtists> getAlbumsWithArtistsByIds(List<Long> albumIds);

    @Transaction
    @Query("SELECT * FROM Album WHERE id = :albumId")
    AlbumWithArtists getAlbumWithArtistsById(Long albumId);

    @Query("SELECT DISTINCT al.* FROM Album al " +
            "LEFT JOIN AlbumArtistCrossRef aa ON al.id = aa.albumId " +
            "LEFT JOIN Artist ar ON ar.id = aa.artistId " +
            "WHERE LOWER(al.title) LIKE LOWER(:query) " +
            "   OR LOWER(ar.displayName) LIKE LOWER(:query)")
    List<Album> searchAlbums(String query);
}