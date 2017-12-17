package com.jonfouk.bookshelf.database;

import android.provider.BaseColumns;

/**
 * Created by joncf on 2/2/2017.
 */

public final class BookDatabaseContract {
    private BookDatabaseContract() {}

    // create table
    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS " + BookEntry.TABLE_NAME
                    + " (" + BookEntry._ID + " INTEGER PRIMARY KEY,"
                    + BookEntry.COL_NAME_ISBN + BookEntry.COL_TYPE_ISBN + ","
                    + BookEntry.COL_NAME_NAME + BookEntry.COL_TYPE_NAME + ","
                    + BookEntry.COL_NAME_WIDTH + BookEntry.COL_TYPE_WIDTH + ","
                    + BookEntry.COL_NAME_CHECKED_IN + BookEntry.COL_TYPE_CHECKED_IN + ","
                    + BookEntry.COL_NAME_ROW + BookEntry.COL_TYPE_ROW + ","
                    + BookEntry.COL_NAME_POSITION + BookEntry.COL_TYPE_POSITION + ","
                    + BookEntry.COL_NAME_COVER_IMAGE + BookEntry.COL_TYPE_COVER_IMAGE + ","
                    + BookEntry.COL_NAME_IMAGE_FETCHED + BookEntry.COL_TYPE_IMAGE_FETCHED + ")";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + BookEntry.TABLE_NAME;

    // Inner class that defines the table contents
    public static class BookEntry implements BaseColumns {

        // names
        public static final String TABLE_NAME = "books";
        public static final String COL_NAME_ISBN = "ISBN";
        public static final String COL_NAME_NAME = "NAME";
        public static final String COL_NAME_WIDTH = "WIDTH";
        public static final String COL_NAME_CHECKED_IN = "CHECKED_IN";
        public static final String COL_NAME_ROW = "ROW";
        public static final String COL_NAME_POSITION ="POSITION";
        public static final String COL_NAME_COVER_IMAGE = "COVER_IMAGE";
        public static final String COL_NAME_IMAGE_FETCHED = "IMAGE_FETCHED";

        // types
        public static final String COL_TYPE_ISBN = " TEXT";
        public static final String COL_TYPE_NAME = " TEXT";
        public static final String COL_TYPE_WIDTH = " REAL";
        public static final String COL_TYPE_CHECKED_IN = " INT";
        public static final String COL_TYPE_ROW = " INT";
        public static final String COL_TYPE_POSITION =" REAL";
        public static final String COL_TYPE_COVER_IMAGE = " BLOB";
        public static final String COL_TYPE_IMAGE_FETCHED = " INT";

    }


}
