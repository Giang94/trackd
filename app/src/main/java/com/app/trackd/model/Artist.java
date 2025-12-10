package com.app.trackd.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Artist {

    @PrimaryKey(autoGenerate = true)
    public long id;
    public String displayName;
    public String normalizedName;

    public Artist(long id, String displayName, String normalizedName) {
        this.id = id;
        this.displayName = displayName;
        this.normalizedName = normalizedName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }
}