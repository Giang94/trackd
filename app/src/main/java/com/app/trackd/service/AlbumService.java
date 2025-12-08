package com.app.trackd.service;

import android.content.Context;

import com.app.trackd.dao.IAlbumDao;
import com.app.trackd.database.AppDatabase;
import com.app.trackd.model.Album;

import java.util.List;

public class AlbumService {

    private final IAlbumDao dao;

    public AlbumService(Context context) {
        dao = AppDatabase.get(context).albumDao();
        // seed();
    }

//    private void seed() {
//        if (!dao.getRecentAlbums().isEmpty()) return;
//
//        dao.insert(new Album(
//                1,
//                "Rossini, Vivaldi, Bach",
//                "Julia Lezhneva",
//                2013,
//                Album.Format.CD,
//                null
//        ));
//        dao.insert(new Album(
//                2,
//                "Dopo notte",
//                "Handel",
//                2025,
//                Album.Format.VINYL,
//                null
//        ));
//        dao.insert(new Album(
//                3,
//                "Celeste Aida",
//                "Verdi",
//                2016,
//                Album.Format.VINYL,
//                null
//        ));
//        dao.insert(new Album(
//                4,
//                "Celeste Aida 02",
//                "Verdi",
//                2018,
//                Album.Format.VINYL,
//                null
//        ));
//    }

    public List<Album> getRecent() {
        return dao.getRecentAlbums();
    }

    public List<Album> getAll() {
        return dao.getAllAlbums();
    }
}
