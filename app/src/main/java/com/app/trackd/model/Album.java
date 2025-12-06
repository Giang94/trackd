package com.app.trackd.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.opencv.core.Mat;

import java.sql.Blob;

@Entity
public class Album {
    public enum Format {CD, VINYL}

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String title;
    private String artist;
    private int year;
    private Format format;
    private String cover;
    private float[] embedding;

    public Album(long id, String title, String artist, int year, Format format, String cover, float[] embedding) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.year = year;
        this.format = format;
        this.cover = cover;
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

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
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
}