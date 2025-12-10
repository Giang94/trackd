package com.app.trackd.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.app.trackd.model.Album;
import com.app.trackd.model.AlbumWithArtists;
import com.app.trackd.model.ref.AlbumArtistCrossRef;

import java.util.List;

@Dao
public interface IAlbumDao {

    @Insert
    long insert(Album album);

    @Query("SELECT * FROM Album ORDER BY id DESC LIMIT :limit")
    List<Album> getRecentAlbums(int limit);

    @Query("SELECT * FROM Album ORDER BY id DESC")
    List<Album> getAllAlbums();

    @Query("SELECT * FROM Album ORDER BY id DESC LIMIT :pageSize OFFSET :currentOffset")
    List<Album> getAlbumsPaged(int currentOffset, int pageSize);

    @Transaction
    @Query("SELECT * FROM Album WHERE id IN (:albumIds) ORDER BY id DESC")
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

    @Query("SELECT COUNT(*) FROM Album")
    int getAlbumCount();

    @Query("SELECT COUNT(*) FROM Album a WHERE a.format LIKE '%' || :format || '%'")
    int getAlbumCountByFormat(String format);

    @Delete
    void delete(Album album);

    @Query("DELETE FROM AlbumArtistCrossRef WHERE albumId = :albumId")
    void deleteArtistLinks(long albumId);

    @Query("SELECT * FROM Album WHERE id = :albumId")
    Album getAlbumById(long albumId);

    @Update
    void updateAlbum(Album album);
}