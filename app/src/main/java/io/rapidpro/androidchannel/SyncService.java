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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Base64;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import io.rapidpro.androidchannel.data.DBCommandHelper;
import io.rapidpro.androidchannel.payload.*;
import io.rapidpro.androidchannel.util.Http;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Syncs our messages with the server.
 */
public class SyncService extends WakefulIntentService {

    // how long we should go without a new message before going forward, prevents
    // us from contacting the server too often during periods of lots of activity
    public static final long QUIET_PERIOD = 15000;

    // how many messages before we ignore our quiet period
    public static final int QUIET_THRESHOLD = 10;

    // how long between receiving messages to attempt toggling airplane
    public static final long NO_INCOMING_FREQUENCY = 1000*60*20;

    // minimum time between us trying airplane mode shenanigans
    public static final long AIRPLANE_MODE_WAIT = 1000l * 60 * 10;

    public static String ENDPOINT = "https://rapidpro.io";

    public SyncService(){
        super(SyncService.class.getSimpleName());
    }

    public String computeHash(String secret, String input) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec((secret).getBytes(), "ASCII"));

        mac.update(input.getBytes("UTF-8"));
        byte[] byteData = mac.doFinal();

        return Base64.encodeToString(byteData, Base64.URL_SAFE).trim();
    }

    protected void showAsUnclaimed() {
        Intent statusIntent = new Intent(Intents.UPDATE_STATUS);
        statusIntent.putExtra(Intents.CLAIMED_EXTRA, false);
        sendBroadcast(statusIntent);
    }

    protected void updateStatus(String status){
        Intent statusIntent = new Intent(Intents.UPDATE_STATUS);
        statusIntent.putExtra(Intents.STATUS_EXTRA, status);
        sendBroadcast(statusIntent);

        RapidPro.broadcastUpdatedCounts(this);
    }

    private void setDataEnabled(boolean enabled) {
        try{
            ConnectivityManager conman = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo data = conman.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            Class conmanClass = Class.forName(conman.getClass().getName());
            Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            Object iConnectivityManager = iConnectivityManagerField.get(conman);
            Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
            Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);
            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);

            // sleep up to 30 seconds for it to take effect
            int sleeps = 0;
            while (sleeps < 30 && data.isConnected() != enabled){
                try{
                    Thread.sleep(1000);
                } catch (Throwable t){}
                data = conman.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                sleeps++;
            }
        } catch (Throwable t){
            RapidPro.LOG.d("Error trying to turn on mobile data");
        }
    }

    private void setWifiEnabled(boolean enabled){
        // enable wifi
        WifiManager manager = (WifiManager)this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        manager.setWifiEnabled(enabled);

        // grab our connectivity manager to test whether it has worked
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        // sleep up to 30 seconds for it to take effect
        int sleeps = 0;
        while (sleeps < 30 && wifi.isConnected() != enabled){
            try{
                Thread.sleep(1000);
            } catch (Throwable t){}
            wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            sleeps++;
        }
    }

    private void checkDataEnabled(boolean enable){
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo data = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        // if we aren't connected to mobile data, make sure it is enabled
        if (data.isConnected() != enable){
            if (enable){
                checkWifiEnabled(false);
            }
            setDataEnabled(enable);
        }
    }

    private void checkWifiEnabled(boolean enable){
        WifiManager manager = (WifiManager)this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        // not connected, turn it on manually
        if (manager.isWifiEnabled() != enable){
            setWifiEnabled(enable);
        }
        // our settings are right, but we aren't connected, try flipping things
        else if (enable && !wifi.isConnected()){
            setWifiEnabled(false);
            setWifiEnabled(true);
        }
    }

    private void setNetworkType(String type){
        if (type.equals("wifi")){
            checkWifiEnabled(true);
        }
        else if (type.equals("data")){
            checkDataEnabled(true);
        }
        else if (type.equals("none")){
            // last case is a no-op, we don't change anything
        }
    }

    private boolean toggleConnection(){
        WifiManager manager = (WifiManager)this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        boolean wifiPossible = prefs.getBoolean(SettingsActivity.WIFI_ENABLED, true);
        boolean dataPossible = prefs.getBoolean(SettingsActivity.DATA_ENABLED, true);

        // wifi is on
        if (manager.isWifiEnabled()){
            // if we can use data, flip to it
            if (dataPossible){
                RapidPro.LOG.d("Toggling connection to data");
                setNetworkType("data");
                return true;
            }
        }
        // wifi is off
        else {
            // so flip to wifi if we allow it
            if (wifiPossible){
                RapidPro.LOG.d("Toggling connection to wifi");
                setNetworkType("wifi");
                return true;
            }
        }

        RapidPro.LOG.d("Not toggling, no other choice available");
        return false;
    }

    protected boolean isGoogleUp(){
        HttpURLConnection conn = null;
        try{
            URL url = new URL("http://google.com/");
            conn =  (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod( "HEAD" );
            conn.connect();

            RapidPro.LOG.d("Google UP: " + conn.getResponseCode());
            return true;
        } catch (Throwable t){
            RapidPro.LOG.d("Google DOWN");
            return false;
        } finally {
            conn.disconnect();
        }
    }

    public void tickleAirplaneMode(){
        try {

            RapidPro.LOG.d("Attempting airplane toggle");
            // Don't attempt an airplane toggle if we are on 4.2 or higher
            if (Build.VERSION.SDK_INT >= 17) {
                return;
            }

            RapidPro.LOG.d("Toggling airplane mode");

            Context context = this;
            Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 1);

            // reload our settings to take effect
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", true);
            context.sendBroadcast(intent);

            // sleep 15 seconds for things to take effect
            try {
                Thread.sleep(15000);
            } catch (Throwable ignore){}

            // then toggle back
            Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);

            // reload our settings to take effect
            intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", false);
            context.sendBroadcast(intent);

            // sleep 20 seconds for things to take effect
            try {
                Thread.sleep(20000);
            } catch (Throwable ignore){}

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putLong(SettingsActivity.LAST_AIRPLANE_TOGGLE, System.currentTimeMillis());
            editor.commit();
        } catch (Throwable t) {
            // on 4.2, we don't get to toggle airplane mode anymore
        }
    }

    protected boolean firePayload(SyncPayload payload, String url, String secret){
        // whether we successfully synced
        boolean synced = false;

        Http http = new Http();
        String json = payload.asJSON().toString();

        SyncPayload response = null;
        boolean connectionOk = false;

        try{
            if (secret != null){
                String seconds = "" + (System.currentTimeMillis() / 1000);
                String signature = computeHash(secret+seconds, json);
                url += "?ts=" + seconds + "&signature=" + URLEncoder.encode(signature);
            }

            try{
                response = new SyncPayload(http.fetchJSON(url, json));
                connectionOk = true;
                synced = true;
            } catch (Throwable t){
                RapidPro.LOG.e("First try error contacting server", t);

                // if google is down, toggle our connection
                if (!isGoogleUp()){
                    toggleConnection();
                }

                response = new SyncPayload(http.fetchJSON(url, json));
                connectionOk = true;
                synced = true;
            }
        } catch (Throwable t){
            RapidPro.LOG.e("Second try error contacting server", t);
        }

        // we got a response.. execute our commands
        if (response != null){
            if (response.errorId == SyncPayload.NO_ERROR){
                for (Command cmd: response.commands){
                    try{
                        cmd.execute(getApplicationContext(), payload);
                    } catch (Throwable t){
                        RapidPro.LOG.e("Error executing cmd: " + cmd, t);
                    }
                }

                updateStatus("");
            }

            // error 1 means invalid signing, which means our secret is wrong
            else if (response.errorId == SyncPayload.INVALID_SECRET) {
                RapidPro.LOG.d("ErrorId: " + response.errorId);
                updateStatus("Error");

                // clear our secret and relayer payloadId
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                editor.remove(SettingsActivity.RELAYER_SECRET);
                editor.remove(SettingsActivity.RELAYER_ID);
                editor.remove(SettingsActivity.RESET);
                editor.commit();
                showAsUnclaimed();
            }

            // error 3 means our clock is out of date compared to the server
            else if (response.errorId == SyncPayload.OLD_REQUEST){
                updateStatus("Time Wrong");
                connectionOk = isGoogleUp();
            }

            // Other kind of application error
            else {
                updateStatus("App Error");
                connectionOk = isGoogleUp();
            }
        } else {
            updateStatus("Network Error");
            connectionOk = isGoogleUp();
        }

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        editor.putBoolean(SettingsActivity.CONNECTION_UP, connectionOk);
        editor.commit();

        return synced;
    }

    @Override
    protected void doWakefulWork (Intent intent) {
        // Stop if RapidPro is paused

        RapidPro.LOG.d("Paused: " + RapidPro.get().isPaused());
        if (RapidPro.get().isPaused()) return;

        updateStatus("Syncing");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        String network = prefs.getString(SettingsActivity.DEFAULT_NETWORK, "none");
        String gcmId = prefs.getString(SettingsActivity.GCM_ID, "");
        boolean useAirplane = prefs.getBoolean(SettingsActivity.AIRPLANE_RESET, false);

        RapidPro.LOG.d("Use airplane: " + useAirplane);

        String uuid = RapidPro.get().getUUID();

        // no gcm id?  don't even try to sync
        if (gcmId.length() == 0){
            return;
        }

        String relayerId = prefs.getString(SettingsActivity.RELAYER_ID, null);
        String secret = prefs.getString(SettingsActivity.RELAYER_SECRET, null);
        String endpoint = prefs.getString(SettingsActivity.SERVER, ENDPOINT);
        String ip = prefs.getString(SettingsActivity.IP_ADDRESS, null);

        boolean force = intent.getBooleanExtra(Intents.FORCE_EXTRA, false) || !RapidPro.get().isClaimed();
        long syncTime = intent.getLongExtra(Intents.SYNC_TIME, 0);

        long lastSync = prefs.getLong(RapidProAlarmListener.LAST_SYNC_TIME, -1l);
        long lastAirplane = prefs.getLong(SettingsActivity.LAST_AIRPLANE_TOGGLE, -1l);
        long lastReceived = prefs.getLong(SettingsActivity.LAST_SMS_RECEIVED, 0);
        long now = System.currentTimeMillis();

        // if this sync was before we actually synced, ignore
        if (syncTime < lastSync){
            updateStatus("");
            return;
        }

        // if our endpoint is an ip, add :8000 to it
        if (endpoint.startsWith("ip")){
            endpoint = "http://" + ip;
            if (!ip.contains(":")) {
                endpoint += ":8000";
            }
        }

        syncTime = System.currentTimeMillis();
        List<Command> commands = DBCommandHelper.getPendingCommands(this, DBCommandHelper.OUT, DBCommandHelper.BORN, 50, null, false);

        // no commands to send out and not forcing?  Return
        if (commands.size() == 0 && !force){
            updateStatus("");
            return;
        }

        // flip to whatever network is preferred by our user
        setNetworkType(network);

        SyncPayload payload = new SyncPayload();
        payload.addCommand(new GCM(gcmId, uuid));
        payload.addCommand(new StatusCommand(this));
        boolean synced = false;

        // we've got everything we need to do proper syncing, go for it
        if (gcmId != null && relayerId != null && secret != null && secret.length() > 0){
            for (Command command: commands){
                payload.addCommand(command);
            }

            String url = endpoint + "/relayers/relayer/sync/" + relayerId + "/";
            synced = firePayload(payload, url, secret);
        }
        // we don't know our secret or relayer payloadId, we should register
        else if (gcmId != null){
            updateStatus("Registering");

            String url = endpoint + "/relayers/relayer/register/";
            synced = firePayload(payload, url, null);
        }

        // send any queued messages
        RapidPro.get().runCommands();

        // if we successfully synced, then update our last sync time
        if (synced) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).edit();
            editor.putLong(RapidProAlarmListener.LAST_SYNC_TIME, syncTime);
            editor.commit();
        }

        // if we have messages in an errored state and haven't sent a message in over 10 minutes, then toggle
        // our airplane mode if we haven't done so recently
        int retry = DBCommandHelper.getCommandCount(this, DBCommandHelper.IN, MTTextMessage.RETRY, MTTextMessage.CMD);
        long lastSMSSent = prefs.getLong(SettingsActivity.LAST_SMS_SENT, 0);

        // see whether we should use the airplane mode hack to keep the phone online
        if (useAirplane && now - lastAirplane > AIRPLANE_MODE_WAIT) {

            boolean toggleAirplane = false;

            // been too long since a successful sync
            toggleAirplane = now - lastSync > AIRPLANE_MODE_WAIT;

            // been too long since we've received a message
            if (!toggleAirplane) {
                toggleAirplane = now - lastReceived >= NO_INCOMING_FREQUENCY;
            }

            // haven't successfully sent an SMS in a while
            if (!toggleAirplane) {
                toggleAirplane =  retry > 0 && now - lastSMSSent > AIRPLANE_MODE_WAIT;
            }

            if (toggleAirplane) {
                tickleAirplaneMode();
            }
        }

        RapidPro.broadcastUpdatedCounts(this);

        // if there are more pending commands, force another sync to work our way through the queue
        commands = DBCommandHelper.getPendingCommands(this, DBCommandHelper.OUT, DBCommandHelper.BORN, 50, null, false);
        if (commands.size() > 10){
            RapidPro.get().sync(true);
        }
    }
}
