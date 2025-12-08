package com.app.trackd.model;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.app.trackd.model.ref.AlbumArtistCrossRef;

import java.util.List;

public class AlbumWithArtists {
    @Embedded
    public Album album;

    @Relation(
            parentColumn = "id",
            entityColumn = "id",
            associateBy = @Junction(
                    value = AlbumArtistCrossRef.class,
                    parentColumn = "albumId",
                    entityColumn = "artistId"
            )
    )
    public List<Artist> artists;
}