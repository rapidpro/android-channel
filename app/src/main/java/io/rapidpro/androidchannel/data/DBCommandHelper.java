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

package io.rapidpro.androidchannel.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import io.rapidpro.androidchannel.RapidPro;
import io.rapidpro.androidchannel.contentprovider.DBCommandContentProvider;
import io.rapidpro.androidchannel.payload.Command;
import io.rapidpro.androidchannel.payload.MOTextMessage;
import io.rapidpro.androidchannel.payload.MTTextMessage;
import io.rapidpro.androidchannel.payload.QueueingCommand;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DBCommandHelper extends SQLiteOpenHelper {

    public static final int BORN = 0;
    public static final int COMPLETE = 100;

    public static final int IN = 0;
    public static final int OUT = 1;

    public static final int HIDDEN = 0;
    public static final int VISIBLE = 1;

    public static final int NONE = -1;

    public static final String COL_ID = "_id";
    public static final String COL_CMD = "cmd";
    public static final String COL_BLOB = "blob";
    public static final String COL_TITLE = "title";
    public static final String COL_BODY = "body";
    public static final String COL_DIRECTION = "direction";
    public static final String COL_STATE = "state";
    public static final String COL_HIDDEN = "hidden";
    public static final String COL_SERVER_ID = "serverId";
    public static final String COL_CREATED = "created";
    public static final String COL_MODIFIED = "modified";
    public static final String COL_EXTRA = "extra";

    public static final int ID_IDX = 0;
    public static final int CMD_IDX = 1;
    public static final int BLOB_IDX = 2;
    public static final int TITLE_IDX = 3;
    public static final int BODY_IDX = 4;
    public static final int DIRECTION_IDX = 5;
    public static final int STATE_IDX = 6;
    public static final int HIDDEN_IDX = 7;
    public static final int SERVER_ID_IDX = 8;
    public static final int CREATED_IDX = 9;
    public static final int MODIFIED_IDX = 10;
    public static final int EXTRA_IDX = 11;

    public static final String TABLE = "commands";
    public static final String DATABASE_NAME = "commands.db";
    public static final int DATABASE_VERSION = 3;

    public static final String[] ALL_COLS = new String[] {
            COL_ID,
            COL_CMD,
            COL_BLOB,
            COL_TITLE,
            COL_BODY,
            COL_DIRECTION,
            COL_STATE,
            COL_HIDDEN,
            COL_SERVER_ID,
            COL_CREATED,
            COL_MODIFIED,
            COL_EXTRA
    };

    public DBCommandHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String createCommandsTable = "CREATE TABLE `commands` " + "("
                + "`_id` INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "`cmd` TEXT,"
                + "`blob` TEXT, "
                + "`title` TEXT, "
                + "`body` TEXT, "
                + "`direction` INTEGER, "
                + "`state` INTEGER, "
                + "`hidden` INTEGER, "
                + "`serverId` INTEGER, "
                + "`created` DATETIME, "
                + "`modified` DATETIME, "
                + "`extra` TEXT"
                + ") ";
        sqLiteDatabase.execSQL(createCommandsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("drop table commands");
        onCreate(sqLiteDatabase);
    }

    public static void clearCommands(Context context){
        context.getContentResolver().delete(DBCommandContentProvider.CONTENT_URI, null, null);
    }

    public static void trimCommands(Context context){
        trimCommands(context, IN, COMPLETE, null, 100);
        trimCommands(context, OUT, COMPLETE, null, 100);
    }

    public static void trimCommands(Context context, int direction, int state, String cmd, int max){
        String[] values = new String[]{ "" + direction, "" + state };
        String match = "direction = ? and state = ?";

        // are we filtering by command?
        if (cmd != null) {
            values = new String[]{ "" + direction ,"" + state, cmd};
            match = "direction = ? and state = ? and cmd = ?";
        }

        int count = getCommandCount(context, direction, state, cmd);
        if (count > max){
            Cursor cursor = context.getContentResolver().query(DBCommandContentProvider.CONTENT_URI,
                    new String[]{ "_id" }, match, values,
                    "_id DESC LIMIT " + max + ",1");

            int lastId = -1;
            try {
                if (cursor.moveToNext()) {
                    lastId = cursor.getInt(0);
                }
            } finally {
                cursor.close();
            }

            if (lastId > 0){
                context.getContentResolver().delete(DBCommandContentProvider.CONTENT_URI,
                        "direction = ? and state = ? and _id <= ?",
                        new String[]{ "" + direction, "" + state, "" + lastId });
            }
        }
    }

    public static void markCommandComplete(Context context, long commandId){
        ContentValues values = new ContentValues();
        values.put(COL_STATE, COMPLETE);
        values.put(COL_MODIFIED, new Date().getTime());

        context.getContentResolver().update(DBCommandContentProvider.CONTENT_URI,
                values, "_id = ?", new String[]{ "" + commandId });
    }

    public static void updateCommandStateWithServerId(Context context, String cmd, int serverId, int state, String extra){

        Log.d(DBCommandHelper.class.getSimpleName(), "Update state: " + cmd + ":" + serverId + " => " + state);

        ContentValues values = new ContentValues();
        values.put(COL_STATE, state);
        values.put(COL_MODIFIED, new Date().getTime());

        if (extra != null) {
            values.put(COL_EXTRA, extra);
        }

        context.getContentResolver().update(DBCommandContentProvider.CONTENT_URI,
                values, "serverId = ? AND cmd = ?", new String[]{ "" + serverId, "" + cmd });
    }

    public static int getMessagesReceivedInWindow(Context context) {
        long thirtyMinutesAgo = System.currentTimeMillis() - RapidPro.MESSAGE_THROTTLE_WINDOW;

        String match = "cmd = ? and direction = ? and modified > ?";
        String[] values =  new String[]{
                MOTextMessage.CMD,
                "" + OUT ,
                "" + thirtyMinutesAgo};

        Cursor cursor = context.getContentResolver().query(DBCommandContentProvider.CONTENT_URI,
                new String[]{ "count(*)" }, match, values, null);

        try {
            if (cursor.moveToNext()) {
                return cursor.getInt(0);
            }
        } finally {
            cursor.close();
        }

        return 0;
    }

    public static Command withServerId(Context context, String cmd, int serverId){
        Cursor cursor = context.getContentResolver().query(DBCommandContentProvider.CONTENT_URI,
                ALL_COLS, "serverId = ? and cmd = ?", new String[]{ "" + serverId, cmd}, null);

        try {
            if (cursor.moveToNext()){
                return Command.fromCursor(cursor);
            } else {
                return null;
            }
        } finally {
            cursor.close();
        }
    }

    public static int getCommandState(Context context, String cmd, int serverId){
        Cursor cursor = context.getContentResolver().query(DBCommandContentProvider.CONTENT_URI,
                ALL_COLS, "serverId = ? and cmd = ?", new String[]{ "" + serverId, cmd}, null);
        try {
            if (cursor.moveToNext()){
                return cursor.getInt(STATE_IDX);
            } else {
                return NONE;
            }
        } finally {
            cursor.close();
        }
    }

    public static int queueCommand(Context context, QueueingCommand cmd){
        int result = queueCommand(context, cmd, BORN);
        return result;
    }

    public static int queueCommand(Context context, QueueingCommand cmd, int complete){

        long now = System.currentTimeMillis();

        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_CREATED, now);
        contentValues.put(COL_MODIFIED, now);
        contentValues.put(COL_CMD, cmd.getCommand());
        contentValues.put(COL_BLOB, cmd.asJSON().toString());
        contentValues.put(COL_TITLE, cmd.getTitle());
        contentValues.put(COL_BODY, cmd.getBody());
        contentValues.put(COL_DIRECTION, cmd.getDirection());
        contentValues.put(COL_HIDDEN, cmd.isHidden());
        contentValues.put(COL_SERVER_ID, cmd.getServerId());
        contentValues.put(COL_STATE, complete);

        RapidPro.LOG.d("Inserting: " + contentValues);

        Uri uri = context.getContentResolver().insert(DBCommandContentProvider.CONTENT_URI, contentValues);
        return Integer.parseInt(uri.getLastPathSegment());
    }

    // the number of seconds to wait for each level of retry
    public static final long MINUTE = 60 * 1000l;
    public static final long[] RETRY_WAITS = new long[]{ 1 * MINUTE,
                                                         1 * MINUTE,
                                                         1 * MINUTE,
                                                         5 * MINUTE,
                                                         5 * MINUTE,
                                                        15 * MINUTE,
                                                        15 * MINUTE,
                                                        30 * MINUTE,
                                                        30 * MINUTE,
                                                        60 * MINUTE};

    /**
     * Builds the query and executes trying to get all commands which need to be tried.  We use the last modified date
     * along with the extra parameter (which represents how many times it has been tried) to build an OR query that
     * should qualify all the candidates.
     *
     * @param context
     * @param max
     * @return
     */
    public static List<Command> getRetryMessages(Context context, int max){
        String match = "state = ? and direction = ? and cmd = ? ";
        String[] values = new String[]{ "" + MTTextMessage.RETRY ,"" + DBCommandHelper.IN, MTTextMessage.CMD,
                                        null, null, null, null, null, null, null, null, null, null,
                                        null, null, null, null, null, null, null, null, null, null };

        // the last_modified is different depending on the number of retries, we build up a query that captures all of them
        long now = System.currentTimeMillis();
        match += " and (";
        String delim = "";
        for (int i=0; i<RETRY_WAITS.length; i++){
            match += delim;
            match += "(extra = ? and modified < ?)";
            values[3 + i*2] = "" + (i+1);
            values[4 + i*2] = "" + (now - RETRY_WAITS[i]);
            delim = " or ";
        }
        match += ")";

        Cursor cursor = context.getContentResolver().query(DBCommandContentProvider.CONTENT_URI, ALL_COLS, match , values , "_id" + ((max != -1) ? " LIMIT " + max : ""));

        try {
            List<Command> commands = new ArrayList<Command>();
            while (cursor.moveToNext()) {
                commands.add(Command.fromCursor(cursor));
            }
            Log.d(DBCommandHelper.class.getSimpleName(), "Got " + commands.size() + " messages to retry");
            return commands;

        } finally {
            cursor.close();
        }
    }

    /**
     * Get all the pending commands to send to the server as JSON
     */
    public static List<Command> getPendingCommands(Context context, int direction, int state, int max, String cmd, boolean excludeCommand){

        String match = "state = ? and direction = ?";
        String[] values = new String[]{ "" + state ,"" + direction};

        // are we filtering by command?
        if (cmd != null) {
            values = new String[]{ "" + state ,"" + direction, cmd};
            match = "state = ? and direction = ? and cmd = ?";
            if (excludeCommand) {
                match = "state = ? and direction = ? and cmd != ?";
            }
        }

        Cursor cursor = context.getContentResolver().query(DBCommandContentProvider.CONTENT_URI, ALL_COLS, match , values , "_id" + ((max != -1) ? " LIMIT " + max : ""));

        try {
            List<Command> commands = new ArrayList<Command>();
            while (cursor.moveToNext()) {
                commands.add(Command.fromCursor(cursor));
            }

            if (direction == DBCommandHelper.IN) {
                Log.d(DBCommandHelper.class.getSimpleName(), cmd + " - Got " + commands.size() + " messages; state " + state);
            }

            return commands;

        } finally {
            cursor.close();
        }

    }

    public static int getCommandCount(Context context, int direction, int state, String cmd){
        String match = "state = ? and direction = ?";
        String[] values = new String[]{ "" + state ,"" + direction};

        // are we filtering by command?
        if (cmd != null) {
            values = new String[]{ "" + state ,"" + direction, cmd};
            match = "state = ? and direction = ? and cmd = ?";
        }

        Cursor cursor = context.getContentResolver().query(DBCommandContentProvider.CONTENT_URI,
                new String[]{ "count(*)" }, match , values , null);

        try {
            if (cursor.moveToNext()) {
                return cursor.getInt(0);
            }
        } finally {
            cursor.close();
        }

        return 0;
    }

}
