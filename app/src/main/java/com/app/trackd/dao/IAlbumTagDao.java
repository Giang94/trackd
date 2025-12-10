package com.app.trackd.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.app.trackd.model.ref.AlbumTagCrossRef;

import java.util.List;

@Dao
public interface IAlbumTagDao {

    @Insert
    void insert(AlbumTagCrossRef crossRef);

    @Insert
    void insertAll(List<AlbumTagCrossRef> refs);

    @Delete
    void delete(AlbumTagCrossRef crossRef);

    @Query("DELETE FROM album_tag WHERE albumId = :albumId")
    void deleteAllForAlbum(long albumId);

    @Query("DELETE FROM album_tag WHERE tagId = :tagId")
    void deleteAllByTagId(long tagId);

    @Query("SELECT tagId FROM album_tag WHERE albumId = :albumId")
    List<Long> getTagIdsForAlbum(long albumId);

    @Query("SELECT albumId FROM album_tag WHERE tagId = :tagId")
    List<Long> getAlbumIdsForTag(long tagId);
}
