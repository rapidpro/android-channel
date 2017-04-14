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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.util.Log;


public class StatusReceiver extends BroadcastReceiver {
    public static final String TAG = StatusReceiver.class.getSimpleName();

    public static final String POWER_STATUS = "powerStatus";
    public static final String POWER_LEVEL = "powerLevel";
    public static final String POWER_SOURCE = "powerSource";
    public static final String NETWORK_TYPE = "networkType";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        final String action = intent.getAction();

        if (action.equals(Intent.ACTION_BATTERY_CHANGED)){
            int powerSource = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            int powerLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int powerStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

            if (powerSource != -1){
                editor.putInt(POWER_SOURCE, powerSource);
            }
            if (powerLevel != -1 ){
                editor.putInt(POWER_LEVEL, powerLevel);
            }
            if (powerStatus != -1){
                editor.putInt(POWER_STATUS, powerStatus);
            }
        }

        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)){
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);

            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            String networkType = "NONE";
            if (networkInfo != null){
                switch (networkInfo.getType()) {
                    case ConnectivityManager.TYPE_WIFI:
                        networkType = "WIFI";
                        break;
                    case 7: // since ConnectivityManager.TYPE_BLUETOOTH constant is supported since API 13 only
                        networkType = "BLUETOOTH";
                        break;
                    case ConnectivityManager.TYPE_WIMAX:
                        networkType = "WIMAX";
                        break;
                    default:
                        networkType = networkInfo.getSubtypeName();
                        break;
                }
            }

            editor.putString(NETWORK_TYPE, networkType);
        }

        editor.commit();

    }
}
