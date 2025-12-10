package com.app.trackd.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.app.trackd.model.Artist;
import com.app.trackd.model.ref.AlbumArtistCrossRef;

import java.util.List;

@Dao
public interface IAlbumArtistDao {

    @Insert
    long insert(AlbumArtistCrossRef albumArtistCrossRef);

    @Query("SELECT a.* FROM Artist a " +
            "INNER JOIN AlbumArtistCrossRef aa ON a.id = aa.artistId " +
            "WHERE aa.albumId = :albumId " +
            "LIMIT :limit")
    List<Artist> getArtistsForAlbum(long albumId, int limit);

    @Query("SELECT a.* FROM Artist a " +
            "INNER JOIN AlbumArtistCrossRef aa ON a.id = aa.artistId " +
            "WHERE aa.albumId = :albumId ")
    List<Artist> getArtistsForAlbum(long albumId);

    @Query("DELETE FROM AlbumArtistCrossRef WHERE albumId = :albumId AND artistId = :artistId")
    void deleteCrossRef(long albumId, long artistId);

    @Query("SELECT artistId FROM AlbumArtistCrossRef WHERE albumId = :albumId")
    List<Long> getArtistIdsForAlbum(long albumId);
}
