package com.app.trackd.model.ref;

import androidx.room.Entity;

@Entity(primaryKeys = {"albumId", "artistId"})
public class AlbumArtistCrossRef {
    public long albumId;
    public long artistId;

    public AlbumArtistCrossRef(long albumId, long artistId) {
        this.albumId = albumId;
        this.artistId = artistId;
    }
}