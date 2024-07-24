/*
 * RapidPro Android Channel - Relay SMS messages where MNO connections aren't practical.
 * Copyright (C) 2014 Nyaruka, UNICEF
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.rapidpro.androidchannel.contentprovider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;

import io.rapidpro.androidchannel.data.DBCommandHelper;

public class DBCommandContentProvider extends ContentProvider {

    // m_db
    private DBCommandHelper m_db;

    // Used for the UriMacher
    private static final int CMDS = 10;
    private static final int CMD_ID = 20;

    private static final String AUTHORITY = "io.rapidpro.androidchannel.commands";
    private static final String BASE_PATH = "cmds";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + BASE_PATH;
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + BASE_PATH;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, CMDS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", CMD_ID);
    }

    @Override
    public boolean onCreate() {
        m_db = new DBCommandHelper(getContext());
        return false;
    }

    private String[] addToSelectionArgs(String[] selection, String col, String match) {
        String[] newArgs  = new String[selection.length + 2];
        System.arraycopy(selection, 0, newArgs , 0, selection.length);
        newArgs[selection.length] = col;
        newArgs[selection.length + 1] = match;
        return newArgs;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // Check if the caller has requested a column which does not exists
        checkColumns(projection);

        // Set the table
        queryBuilder.setTables(DBCommandHelper.TABLE);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case CMDS:
                break;
            case CMD_ID:
                // Adding the ID to the original query
                selectionArgs = addToSelectionArgs(selectionArgs, DBCommandHelper.COL_ID, uri.getLastPathSegment());
                if (TextUtils.isEmpty(selection)) {
                    selection = "? = ?";
                } else {
                    selection += " and ? = ?";
                }

                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = m_db.getReadableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase db = m_db.getWritableDatabase();
        long id = 0;

        try {
            switch (uriType) {
                case CMDS:
                    id = db.insert(DBCommandHelper.TABLE, null, values);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }

            getContext().getContentResolver().notifyChange(uri, null);
            return Uri.parse(BASE_PATH + "/" + id);
        } finally {
            //db.close();
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase db = m_db.getWritableDatabase();
        int rowsDeleted = 0;

        try{
            switch (uriType) {
                case CMDS:
                    rowsDeleted = db.delete(DBCommandHelper.TABLE, selection, selectionArgs);
                    break;

                case CMD_ID:
                    String id = uri.getLastPathSegment();
                    if (TextUtils.isEmpty(selection)) {
                        rowsDeleted = db.delete(DBCommandHelper.TABLE, "? = ?", new String[]{ DBCommandHelper.COL_ID, id });
                    } else {
                        selectionArgs = addToSelectionArgs(selectionArgs, DBCommandHelper.COL_ID, id);
                        rowsDeleted = db.delete(DBCommandHelper.TABLE,selection + " and ? = ?", selectionArgs);
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }

            getContext().getContentResolver().notifyChange(uri, null);
            return rowsDeleted;
        } finally {
            //db.close();
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase db = m_db.getWritableDatabase();
        int rowsUpdated = 0;

        try{
            switch (uriType) {
                case CMDS:
                    rowsUpdated = db.update(DBCommandHelper.TABLE,
                            values,
                            selection,
                            selectionArgs);
                    break;

                case CMD_ID:
                    String id = uri.getLastPathSegment();
                    if (TextUtils.isEmpty(selection)) {
                        rowsUpdated = db.update(DBCommandHelper.TABLE,
                                values,
                                "? = ?", new String[]{ DBCommandHelper.COL_ID, id });
                    } else {
                        selectionArgs = addToSelectionArgs(selectionArgs, DBCommandHelper.COL_ID, id);
                        rowsUpdated = db.update(DBCommandHelper.TABLE,
                                values,
                                selection + " and ? = ?",
                                selectionArgs);
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }

            getContext().getContentResolver().notifyChange(uri, null);
            return rowsUpdated;
        } finally {
            //db.close();
        }
    }

    private void checkColumns(String[] projection) {
        if (projection != null) {
            if (projection.length == 1 && projection[0].equals("count(*)")) {
                return;
            }

            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(DBCommandHelper.ALL_COLS));

            // Check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }

}
