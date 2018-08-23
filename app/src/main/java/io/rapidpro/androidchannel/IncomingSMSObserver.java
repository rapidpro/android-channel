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

import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Telephony;

import io.rapidpro.androidchannel.data.DBCommandHelper;
import io.rapidpro.androidchannel.payload.MOTextMessage;

import java.util.Date;


/**
 * Observers the Incoming SMS to relay them to the server as they are received
 */
public class IncomingSMSObserver extends ContentObserver{

    // the time of the last SMS we've seen
    private long m_lastSMS = System.currentTimeMillis();

    public IncomingSMSObserver(){
        super(new Handler(Looper.getMainLooper()));
    }

    @Override
    public boolean deliverSelfNotifications() {
        return false;
    }

    @Override
    public void onChange(boolean selfChange) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RapidPro.get().getApplicationContext());

        // If we are paused or unclaimed, just update our last SMS seen so we don't process these later
        if (RapidPro.get().isPaused() || !RapidPro.get().isClaimed()){
            m_lastSMS = System.currentTimeMillis();
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(RapidPro.get().getApplicationContext()).edit();
            editor.putLong(SettingsActivity.LAST_SMS_RECEIVED, m_lastSMS);
            editor.commit();
            return;
        }

        Uri inboxUri = Telephony.Sms.Inbox.CONTENT_URI;

        // get any new SMS in the inbox
        Cursor cursor = RapidPro.get().getContentResolver().query(inboxUri, null, null, null, "date DESC");

        // whether we need to sync
        boolean doSync = false;
        long newLastSMS = m_lastSMS;

        // when did we last receive a message?
        m_lastSMS = prefs.getLong(SettingsActivity.LAST_SMS_RECEIVED, m_lastSMS);
        boolean deleteSMSAllowed = prefs.getBoolean(SettingsActivity.SMS_AUTO_DELETE, false);

        // While there are records to look at
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndex("_id"));
            long time = cursor.getLong(cursor.getColumnIndex("date"));

            // update our most recent message if needbe
            if (time > newLastSMS){
                newLastSMS = time;
            }

            // if we have seen this message, break out
            if (time <= m_lastSMS){
                break;
            }

            String address = cursor.getString(cursor.getColumnIndex("address"));
            String message = cursor.getString(cursor.getColumnIndex("body"));
            RapidPro.LOG.d("SMS[" + id + "] Arrived: " + time + " From: " + address + " message body: " + message + " (last seen " + m_lastSMS + ")");

            DBCommandHelper.queueCommand(RapidPro.get(), new MOTextMessage(address, message, new Date(time)));
            doSync = true;

            if (deleteSMSAllowed) {
                RapidPro.LOG.d("DELETE SMS: at " + id + " message: " + message);
                RapidPro.get().getContentResolver().delete(Uri.parse("content://sms/" + id), null, null);
            }
        }

        // trigger a sync if we need it
        if (doSync){
            m_lastSMS = newLastSMS;
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(RapidPro.get().getApplicationContext()).edit();
            editor.putLong(SettingsActivity.LAST_SMS_RECEIVED, m_lastSMS);
            editor.commit();

            RapidPro.get().refreshHome();
            RapidPro.broadcastUpdatedCounts(RapidPro.get().getApplicationContext());
            RapidPro.get().sync(true);
        }
    }
}
