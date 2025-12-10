package com.app.trackd.model;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.app.trackd.model.ref.AlbumArtistCrossRef;

import java.util.List;

public class AlbumWithArtists {
    @Embedded
    private Album album;

    @Relation(
            parentColumn = "id",
            entityColumn = "id",
            associateBy = @Junction(
                    value = AlbumArtistCrossRef.class,
                    parentColumn = "albumId",
                    entityColumn = "artistId"
            )
    )
    private List<Artist> artists;

    public AlbumWithArtists(Album album, List<Artist> artists) {
        this.album = album;
        this.artists = artists;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public List<Artist> getArtists() {
        return artists;
    }

    public void setArtists(List<Artist> artists) {
        this.artists = artists;
    }
}