package com.app.trackd.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.app.trackd.model.Album;
import com.app.trackd.model.AlbumWithArtists;

import java.util.List;

@Dao
public interface IAlbumDao {

    @Insert
    long insert(Album album);

    @Query("SELECT * FROM Album ORDER BY id DESC LIMIT :limit")
    List<Album> getRecentAlbums(int limit);

    @Query("SELECT * FROM Album ORDER BY id DESC")
    List<Album> getAllAlbums();

    @Query("SELECT * FROM Album WHERE cover IS NOT NULL AND cover != '' AND embedding IS NOT NULL ORDER BY id DESC")
    List<Album> getAllAlbumsWithCoverAndEmbedding();

    @Query("SELECT * FROM Album ORDER BY id DESC LIMIT :pageSize OFFSET :currentOffset")
    List<Album> getAlbumsPaged(int currentOffset, int pageSize);

    @Transaction
    @Query("SELECT * FROM Album WHERE id IN (:albumIds) ORDER BY id DESC")
    List<AlbumWithArtists> getAlbumsWithArtistsByIds(List<Long> albumIds);

    @Transaction
    @Query("SELECT * FROM Album WHERE id = :albumId")
    AlbumWithArtists getAlbumWithArtistsById(Long albumId);

    @Query("SELECT COUNT(*) FROM Album")
    int getAlbumCount();

    @Query("SELECT COUNT(*) FROM Album a WHERE a.format LIKE '%' || :format || '%'")
    int getAlbumCountByFormat(String format);

    @Delete
    void delete(Album album);

    @Query("DELETE FROM AlbumArtistCrossRef WHERE albumId = :albumId")
    void deleteArtistLinks(long albumId);

    @Query("SELECT * FROM Album WHERE id = :albumId")
    Album getAlbumById(long albumId);

    @Update
    void update(Album album);

//    @Query("SELECT * FROM album WHERE id IN (SELECT albumId FROM album_tag WHERE tagId = :tagId)")
//    List<Album> getAlbumsByTag(long tagId);
//
//    @Query("SELECT * FROM Album WHERE format IN (:formats)")
//    List<Album> getAlbumsByFormats(List<String> formats);
//
//    @Transaction
//    @Query(" SELECT * FROM Album " +
//            " WHERE (:vinyl = 1 OR format NOT LIKE '%VINYL%') " +
//            " AND (:cds = 1 OR format NOT LIKE '%CD%') " +
//            " ORDER BY id DESC LIMIT :limit OFFSET :offset")
//    List<AlbumWithArtists> getAlbumsPagedWithArtists(
//            boolean vinyl,
//            boolean cds,
//            int limit,
//            int offset
//    );
//    @Query(" SELECT DISTINCT al.* FROM Album al " +
//            " LEFT JOIN AlbumArtistCrossRef aa ON al.id = aa.albumId" +
//            " LEFT JOIN Artist ar ON ar.id = aa.artistId" +
//            " WHERE LOWER(al.title) LIKE LOWER(:query)" +
//            " OR LOWER(ar.displayName) LIKE LOWER(:query)" +
//            " ORDER BY al.id DESC " +
//            " LIMIT :limit OFFSET :offset")
//    List<Album> searchAlbumsPaged(String query, int limit, int offset);

    @Query(" SELECT * FROM Album " +
            " WHERE format IN (:formats) " +
            " ORDER BY id DESC " +
            " LIMIT :limit OFFSET :offset "             )
    List<Album> getAlbumsByFormatsPaged(List<String> formats, int limit, int offset);

    @Query("SELECT * FROM Album WHERE format IN (:formats) ORDER BY id DESC")
    List<Album> getAlbumsByFormats(List<String> formats);

    @Query("SELECT COUNT(*) FROM album")
    int countAllAlbums();

    @Query("SELECT COUNT(*) FROM album WHERE format IN (:formats)")
    int countAlbumsByFormats(List<String> formats);

    @Query("SELECT COUNT(DISTINCT a.id) FROM album a " +
            " LEFT JOIN AlbumArtistCrossRef aar ON a.id = aar.albumId " +
            " LEFT JOIN artist ar ON aar.artistId = ar.id " +
            " WHERE a.title LIKE :query OR ar.displayName LIKE :query")
    int countSearchAlbums(String query);

    @Query("SELECT COUNT(DISTINCT a.id) FROM album a " +
            " LEFT JOIN AlbumArtistCrossRef aar ON a.id = aar.albumId " +
            " LEFT JOIN artist ar ON aar.artistId = ar.id " +
            " WHERE a.format IN (:formats) AND (a.title LIKE :query OR ar.displayName LIKE :query)")
    int countAlbumsByFormatsAndSearch(List<String> formats, String query);

    @Query("SELECT DISTINCT a.* FROM album a " +
            " LEFT JOIN AlbumArtistCrossRef aa ON aa.albumId = a.id " +
            " LEFT JOIN artist ar ON ar.id = aa.artistId " +
            " WHERE (:formatsEmpty = 1 OR a.format IN (:formats)) " +
            " AND (a.title LIKE :query OR ar.displayName LIKE :query) " +
            " LIMIT :limit OFFSET :offset")
    List<Album> searchAlbumsPagedWithFormats(
            List<String> formats,
            boolean formatsEmpty,
            String query,
            int limit,
            int offset
    );

    @Query("SELECT DISTINCT a.* FROM album a " +
            " LEFT JOIN AlbumArtistCrossRef aa ON aa.albumId = a.id " +
            " LEFT JOIN artist ar ON ar.id = aa.artistId " +
            " WHERE (:formatsEmpty = 1 OR a.format IN (:formats)) " +
            " AND (a.title LIKE :query OR ar.displayName LIKE :query) " +
            " ORDER BY a.id DESC")
    List<Album> searchAlbumsWithFormats(
            List<String> formats,
            boolean formatsEmpty,
            String query
    );

    @Query("SELECT a.format AS format, COUNT(DISTINCT a.id) AS count FROM album a " +
            " LEFT JOIN AlbumArtistCrossRef aar ON a.id = aar.albumId " +
            " LEFT JOIN artist ar ON aar.artistId = ar.id " +
            " WHERE (:formatsEmpty = 1 OR a.format IN (:formats)) " +
            " AND (a.title LIKE :query OR ar.displayName LIKE :query) " +
            " GROUP BY a.format")
    List<FormatCountRow> countAlbumsByFormatAndSearch(
            List<String> formats,
            boolean formatsEmpty,
            String query
    );

    class FormatCountRow {
        public String format;
        public int count;
    }

}