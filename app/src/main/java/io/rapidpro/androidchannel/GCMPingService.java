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

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.commonsware.cwac.wakeful.WakefulIntentService;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Message.Builder;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

import java.io.IOException;

/**
 * Pings our GCM service every minute to keep that connection awake
 */
public class GCMPingService extends WakefulIntentService {

    public GCMPingService(){
        super(GCMPingService.class.getSimpleName());
    }

    @Override
    protected void doWakefulWork (Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String registrationId = prefs.getString(SettingsActivity.GCM_ID, null);

        // we are registered, so lets go ping ourself
        if (registrationId != null){
            Sender s = new Sender(Config.GCM_API_KEY);
            Message.Builder mb = new Builder();
            Message m = mb.delayWhileIdle(false).addData("msg", "ping").addData("ts", "" + System.currentTimeMillis()).build();

            try {
                Result r = s.send(m, registrationId, 10);
                if(r != null) {
                    if(r.getMessageId() == null) {
                      RapidPro.LOG.d("ERROR: Failed to send message (" + r.getErrorCodeName() + ")");
                    } else {
                      RapidPro.LOG.d("GCM Message sent ok");
                    }
                } else {
                    RapidPro.LOG.d("ERROR: GCM is unavailable (null Result)");
                }
            } catch (IOException e) {
                RapidPro.LOG.d("ERROR: IOException: " + e.getMessage());
            }
        }

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putLong(RapidProAlarmListener.LAST_GCM_TIME, System.currentTimeMillis());
        editor.commit();
    }
}
