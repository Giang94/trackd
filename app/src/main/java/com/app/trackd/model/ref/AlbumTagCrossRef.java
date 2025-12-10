package com.app.trackd.model.ref;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import com.app.trackd.model.Album;
import com.app.trackd.model.Tag;

@Entity(
        tableName = "album_tag",
        primaryKeys = {"albumId", "tagId"},
        foreignKeys = {
                @ForeignKey(
                        entity = Album.class,
                        parentColumns = "id",
                        childColumns = "albumId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Tag.class,
                        parentColumns = "id",
                        childColumns = "tagId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index("albumId"),
                @Index("tagId")
        }
)
public class AlbumTagCrossRef {

    private long albumId;
    private long tagId;

    public AlbumTagCrossRef(long albumId, long tagId) {
        this.albumId = albumId;
        this.tagId = tagId;
    }

    public long getAlbumId() {
        return albumId;
    }

    public long getTagId() {
        return tagId;
    }
}
