package com.app.trackd.database;

import androidx.room.TypeConverter;

import com.google.gson.Gson;

public class EmbeddingConverter {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromFloatArray(float[] array) {
        return array == null ? null : gson.toJson(array);
    }

    @TypeConverter
    public static float[] toFloatArray(String data) {
        return data == null ? null : gson.fromJson(data, float[].class);
    }
}
