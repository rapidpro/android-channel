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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import com.commonsware.cwac.wakeful.WakefulIntentService;

public class RapidProAlarmListener implements WakefulIntentService.AlarmListener {
    public static final long ONE_SECOND = 1000;
    public static final long ONE_MINUTE = ONE_SECOND * 60;
    public static final long ACTIVE_PERIOD = ONE_MINUTE * 15;
    public static final long UNCLAIMED_ACTIVE_PERIOD = ONE_MINUTE * 90;
    public static final long MAX_AGE = ONE_SECOND * 90;

    public static final String LAST_SYNC_TIME = "lastSync";
    public static final String LAST_FCM_TIME = "lastFcm";
    public static final String LAST_RUN_COMMANDS_TIME = "lastRun";
    public static final String FIRST_FCM_TIME = "firstGcm";

    public static final long MAX_FCM_AGE = ONE_MINUTE * 4;
    public static final long SYNC_FREQUENCY = ONE_MINUTE * 5;
    public static final long COMMAND_FREQUENCY = ONE_MINUTE * 1;
    public static final long UNCLAIMED_SYNC_FREQUENCY = ONE_SECOND * 60;

    /** Empty constructor required */
    public RapidProAlarmListener(){
    }

    @Override
    public void scheduleAlarms(AlarmManager mgr, PendingIntent intent, Context context) {
        // Stop if RapidPro is paused
        if (RapidPro.get().isPaused()) return;

        long wait = ONE_MINUTE;
        mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + wait, intent);
    }

    @Override
    public void sendWakefulWork(Context ctxt) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
        int powerStatus = prefs.getInt(StatusReceiver.POWER_STATUS, BatteryManager.BATTERY_STATUS_DISCHARGING);

        long now = System.currentTimeMillis();

        // if we aren't on battery, ping FCM every time for faster pushes, otherwise only every 4 mins
        long lastFCMTime = prefs.getLong(LAST_FCM_TIME, 0);
        if (powerStatus == BatteryManager.BATTERY_STATUS_CHARGING || now - lastFCMTime >= MAX_FCM_AGE){
            RapidPro.get().pingFCM();
        }

        long lastSent = prefs.getLong(SettingsActivity.LAST_SMS_SENT, 0);
        long lastSyncTime = prefs.getLong(LAST_SYNC_TIME, 0);
        long firstFcm = prefs.getLong(FIRST_FCM_TIME, 0);
        long lastReceived = prefs.getLong(SettingsActivity.LAST_SMS_RECEIVED, 0);

        if (now - lastSyncTime >= SYNC_FREQUENCY || now - firstFcm < UNCLAIMED_ACTIVE_PERIOD || now - lastSent < ACTIVE_PERIOD || now - lastReceived < ACTIVE_PERIOD){
            RapidPro.LOG.d("Scheduled task SYNC STARTED");
            RapidPro.get().sync(true);
        }

        long lastRunCommands_time = prefs.getLong(LAST_RUN_COMMANDS_TIME, 0);
        if (now - lastRunCommands_time >= COMMAND_FREQUENCY){
            RapidPro.LOG.d("Scheduled task RUNCOMMANDS Started");
            RapidPro.get().runCommands();
        }

        WakefulIntentService.scheduleAlarms(new RapidProAlarmListener(), ctxt);
    }

    @Override
    public long getMaxAge() {
        return MAX_AGE;
    }
}
