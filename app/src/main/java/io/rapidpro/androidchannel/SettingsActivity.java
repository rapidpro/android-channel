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
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    // These strings are not camel case because they represent commands sent to the server
    public static final String FCM_ID = "fcm_id";
    public static final String RELAYER_ID = "relayer_id";
    public static final String RELAYER_SECRET = "relayer_secret";
    public static final String RELAYER_CLAIM_CODE = "relayer_claim_code";
    public static final String SERVER = "server";
    public static final String IP_ADDRESS = "ip_address";
    public static final String RELAYER_ORG = "relayer_org";
    public static final String CONNECTION_UP = "connection_up";
    public static final String LAST_AIRPLANE_TOGGLE = "last_airplane_toggle";
    public static final String UUID = "uuid";

    public static final String APP_VERSION = "appVersion";

    public static final String RESET = "reset";
    public static final String AIRPLANE_RESET = "airplane_reset";

    public static final String IS_PAUSED = "rapidproPaused";

    private Boolean showAdvancedSettings;

    public static final String DEFAULT_NETWORK = "default_network";
    public static final String DATA_ENABLED = "network_data";
    public static final String WIFI_ENABLED = "network_wifi";
    public static final String LAST_SMS_SENT = "last_sms_sent";
    public static final String LAST_SMS_RECEIVED = "last_sms_received";
    public static final String SMS_AUTO_DELETE = "sms_auto_delete";


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.settings);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        showAdvancedSettings =  prefs.getBoolean(HomeActivity.SHOW_ADVANCED_SETTINGS, false);

        if (!showAdvancedSettings) {
            getPreferenceScreen().removePreference(getPreferenceManager().findPreference("advanced_settings"));
        }

        findPreference(RESET).setOnPreferenceClickListener(this);
    }

    public static void setFCM(Context context, String id){
        RapidPro.LOG.d("=== Setting FCM: " + id);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        if (id == null){
            editor.remove(SettingsActivity.FCM_ID);
        } else {
            editor.putString(SettingsActivity.FCM_ID, id);
            editor.putLong(RapidPro.FIRST_FCM_TIME, System.currentTimeMillis());
        }
        editor.apply();

        // update our state
        Intent intent = new Intent(Intents.UPDATE_RELAYER_STATE);
        RapidPro.get().sendBroadcast(intent);
        RapidPro.get().sync();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (SERVER.equals(key)) {
            RapidPro.LOG.d("Unregistering with server");
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(RELAYER_ID);
            editor.remove(RELAYER_ORG);
            editor.remove(RELAYER_SECRET);
            editor.remove(RELAYER_CLAIM_CODE);
            editor.putBoolean(IS_PAUSED, false);
            editor.apply();
            RapidPro.get().sync();
            finish();
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (RESET.equals(preference.getKey())){
            RapidPro.get().reset();
            finish();
            return true;
        }

        return false;
    }
}
