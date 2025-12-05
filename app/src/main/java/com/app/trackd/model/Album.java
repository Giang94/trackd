package com.app.trackd.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.opencv.core.Mat;

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
    private int orbRows;
    private int orbCols;
    private byte[] orbDescriptor;
    private int orbType;

    public Album(long id, String title, String artist, int year, Format format, String cover, int orbRows, int orbCols, int orbType, byte[] orbDescriptor) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.year = year;
        this.format = format;
        this.cover = cover;
        this.orbRows = orbRows;
        this.orbCols = orbCols;
        this.orbType = orbType;
        this.orbDescriptor = orbDescriptor;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public int getYear() {
        return year;
    }

    public Format getFormat() {
        return format;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public int getOrbRows() {
        return orbRows;
    }

    public void setOrbRows(int orbRows) {
        this.orbRows = orbRows;
    }

    public int getOrbCols() {
        return orbCols;
    }

    public void setOrbCols(int orbCols) {
        this.orbCols = orbCols;
    }

    public byte[] getOrbDescriptor() {
        return orbDescriptor;
    }

    public void setOrbDescriptor(byte[] orbDescriptor) {
        this.orbDescriptor = orbDescriptor;
    }

    public int getOrbType() {
        return orbType;
    }

    public void setOrbType(int orbType) {
        this.orbType = orbType;
    }
}