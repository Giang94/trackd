package com.app.trackd.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.app.trackd.model.Tag;

import java.util.List;

@Dao
public interface ITagDao {

    @Insert
    List<Long> insertAll(List<Tag> tags);

    @Update
    int update(Tag tag);

    @Delete
    int delete(Tag tag);

    @Query("SELECT * FROM tag ORDER BY name ASC")
    List<Tag> getAll();

    @Query("SELECT * FROM tag WHERE id = :tagId LIMIT 1")
    Tag getById(long tagId);

    @Query("SELECT * FROM tag WHERE name LIKE :query ORDER BY name ASC")
    List<Tag> searchByName(String query);

    @Query("SELECT * FROM tag WHERE id IN (SELECT tagId FROM album_tag WHERE albumId = :albumId)")
    List<Tag> getTagsForAlbum(long albumId);


    @Query("SELECT * FROM tag WHERE normalized = :normalized LIMIT 1")
    Tag findByNormalized(String normalized);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Tag tag); // returns -1 if already exists
}
