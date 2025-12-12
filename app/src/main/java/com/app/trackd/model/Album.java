package com.app.trackd.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.app.trackd.model.enums.AlbumFormat;

@Entity
public class Album {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String title;
    private int year;
    private AlbumFormat format;
    private String cover;
    private String spotifyUrl;
    private float[] embedding;

    public Album(long id, String title, int year, AlbumFormat format, String cover, String spotifyUrl, float[] embedding) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.format = format;
        this.cover = cover;
        this.spotifyUrl = spotifyUrl;
        this.embedding = embedding;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public AlbumFormat getFormat() {
        return format;
    }

    public void setFormat(AlbumFormat format) {
        this.format = format;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSpotifyUrl() {
        return spotifyUrl;
    }

    public void setSpotifyUrl(String spotifyUrl) {
        this.spotifyUrl = spotifyUrl;
    }
}