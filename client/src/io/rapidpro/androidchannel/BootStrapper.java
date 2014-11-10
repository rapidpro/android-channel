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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * BootStrapper just makes sure our service is running after the phone is booted
 */
public class BootStrapper extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent){
        checkService(context);
    }

    public static boolean checkService(Context context){
        // check if our service is running, start it if not
        if (!isServiceRunning(context)){
            startService(context);
            return false;
        } else {
            return true;
        }
    }

    private static void startService(Context context){
        RapidPro.get().sync();
    }

    private static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("io.rapidpro.androidchannel.SyncService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
