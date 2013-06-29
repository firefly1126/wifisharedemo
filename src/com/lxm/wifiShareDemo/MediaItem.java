package com.lxm.wifiShareDemo;

import android.database.Cursor;
import android.provider.MediaStore;

import java.util.Properties;

/**
 * 媒体对象
 * @author simon.L
 * @version 1.0.0
 */
public class MediaItem {

    public static final String[] COLUMNS = {MediaStore.Audio.Media._ID
        , MediaStore.Audio.Media.DATA
        , MediaStore.Audio.Media.TITLE
        , MediaStore.Audio.Media.MIME_TYPE
        , MediaStore.Audio.Media.ARTIST
        , MediaStore.Audio.Media.ALBUM
        , MediaStore.Audio.Media.SIZE
        , MediaStore.Audio.Media.DURATION};

    long mId;
    long mSize;
    long mDuration;
    String mData;
    String mTitle;
    String mMimeType;
    String mArtist;
    String mAlbum;

    public MediaItem(Cursor cursor) {
        if (cursor != null && !cursor.isClosed() && !cursor.isAfterLast()) {
            mId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
            mSize = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
            mDuration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            mData = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            mTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            mMimeType = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE));
            mArtist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            mAlbum  = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
        }
    }

    public MediaItem(Properties properties) {
        try {
            mId = Integer.parseInt(properties.getProperty(MediaStore.Audio.Media._ID));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        try {
            mSize = Integer.parseInt(properties.getProperty(MediaStore.Audio.Media.SIZE));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        try {
            mId = Integer.parseInt(properties.getProperty(MediaStore.Audio.Media.DURATION));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        mData = properties.getProperty(MediaStore.Audio.Media.DATA);
        mTitle = properties.getProperty(MediaStore.Audio.Media.TITLE);
        mMimeType = properties.getProperty(MediaStore.Audio.Media.MIME_TYPE);
        mArtist = properties.getProperty(MediaStore.Audio.Media.ARTIST);
        mAlbum = properties.getProperty(MediaStore.Audio.Media.ALBUM);
    }
}
