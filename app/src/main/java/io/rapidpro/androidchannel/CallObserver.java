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

package io.rapidpro.androidchannel;

import android.database.ContentObserver;
import android.database.Cursor;
import android.provider.CallLog.Calls;

import io.rapidpro.androidchannel.data.DBCommandHelper;
import io.rapidpro.androidchannel.payload.CallLog;

/**
 * Observes our call log for calls reporting them to the server as they occur.
  */
public class CallObserver extends ContentObserver {

    /** the last call we've seen */
    private long lastSeenId = 0;

    public CallObserver(){
        super(null);
    }

    @Override
    public boolean deliverSelfNotifications(){
        return false;
    }

    @Override
    public void onChange(boolean selfChange){
        // Stop if RapidPro is paused
        if (RapidPro.get().isPaused()) return;

        // ignore events if we are unclaimed
        if (!RapidPro.get().isClaimed()){
            return;
        }



        // get any new items in our call log
        Cursor cursor = RapidPro.get().getContentResolver().query(
                android.provider.CallLog.Calls.CONTENT_URI,
                null,
                Calls.NEW + " = ?",
                new String[] { "1" }, Calls.DATE + " DESC ");

        // whether we need to sync
        boolean doSync = false;

        // while there are records to look at
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndex(Calls._ID));
            if (id <= lastSeenId){
                break;
            }

            long time = cursor.getLong(cursor.getColumnIndex(Calls.DATE));
            int type = cursor.getInt(cursor.getColumnIndex(Calls.TYPE));
            String number = cursor.getString(cursor.getColumnIndex(Calls.NUMBER));
            int duration = cursor.getInt(cursor.getColumnIndex(Calls.DURATION));

            String typeSlug = CallLog.UNKNOWN;
            if (type == Calls.INCOMING_TYPE){
                typeSlug = CallLog.INCOMING;
            } else if (type == Calls.OUTGOING_TYPE){
                typeSlug = CallLog.OUTGOING;
            } else if (type == Calls.MISSED_TYPE){
                typeSlug = CallLog.MISSED;
            }

            // create a new command to notify the server
            DBCommandHelper.queueCommand(RapidPro.get(), new CallLog(typeSlug, number, time, duration));
            doSync = true;

            if (id > lastSeenId){
                lastSeenId = id;
            }
        }

        // trigger a sync if we need to
        if (doSync){
            RapidPro.get().refreshHome();
            RapidPro.get().sync();
        }

        cursor.close();
    }
}
