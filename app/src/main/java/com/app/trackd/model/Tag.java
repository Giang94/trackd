package com.app.trackd.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.app.trackd.util.StringUtils;

@Entity
public class Tag {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    public String name;

    @NonNull
    public String normalized;

    public Tag(@NonNull String name) {
        this.name = name;
        this.normalized = StringUtils.normalize(name);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getNormalized() {
        return normalized;
    }

    public void setNormalized(@NonNull String normalized) {
        this.normalized = normalized;
    }
}
