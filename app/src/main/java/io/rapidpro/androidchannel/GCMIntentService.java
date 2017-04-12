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

import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {

    public GCMIntentService() {
        super("sender_id");
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        String msg = intent.getStringExtra("msg");
        RapidPro.LOG.d("\n===Message: " + msg);

        if (msg.equals("ping")){
            RapidPro.LOG.d("\n===Ping received, taking no action.");
        } else if (msg.equals("sync")){
            RapidPro.get().sync(true);
        }
    }

    @Override
    protected void onError(Context context, String s) {
        RapidPro.LOG.d("\n===Error: " + s);
    }

    @Override
    protected void onRegistered(Context context, String s) {
        RapidPro.LOG.d("\n===Registered: " + s);
        SettingsActivity.setGCM(context, s);
    }

    @Override
    protected void onUnregistered(Context context, String s) {
        RapidPro.LOG.d("\n===Unregistered: " + s);
        SettingsActivity.setGCM(context, null);
    }
}
