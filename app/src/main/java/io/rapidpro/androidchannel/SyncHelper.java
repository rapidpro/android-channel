package io.rapidpro.androidchannel;

import static android.content.Context.CONNECTIVITY_SERVICE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Base64;

import androidx.preference.PreferenceManager;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.rapidpro.androidchannel.data.DBCommandHelper;
import io.rapidpro.androidchannel.payload.Command;
import io.rapidpro.androidchannel.payload.FCM;
import io.rapidpro.androidchannel.payload.MTTextMessage;
import io.rapidpro.androidchannel.payload.StatusCommand;
import io.rapidpro.androidchannel.payload.SyncPayload;
import io.rapidpro.androidchannel.util.Http;

public class SyncHelper {

    // how many seconds between polling
    public static final long POLLING_INTERVAL = 600;

    // how long we should go without a new message before going forward, prevents
    // us from contacting the server too often during periods of lots of activity
    public static final long QUIET_PERIOD = 15000;

    // how many messages before we ignore our quiet period
    public static final int QUIET_THRESHOLD = 10;

    // how long between receiving messages to attempt toggling airplane
    public static final long NO_INCOMING_FREQUENCY = 1000*60*20;


    public static String ENDPOINT = "https://rapidpro.io";

    private Context context;
    private long syncTime;
    private boolean force;

    public SyncHelper(Context context, long syncTime, boolean force) {
        this.context = context;
        this.syncTime = syncTime;
        this.force = force;
    }

    public void sync() {

        synchronized (RapidPro.get()) {
            // Stop if RapidPro is paused
            RapidPro.LOG.d("Paused: " + RapidPro.get().isPaused());
            if (RapidPro.get().isPaused()) {
                return;
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            long lastSync = prefs.getLong(RapidPro.LAST_SYNC_TIME, -1L);

            List<Command> commands = DBCommandHelper.getPendingCommands(context, DBCommandHelper.OUT, DBCommandHelper.BORN, 50, null, false);

            if (force) {
                // forcing honors our quiet period if we don't have enough messages
                if (commands.size() >= QUIET_THRESHOLD && syncTime - QUIET_PERIOD < lastSync) {
                    RapidPro.LOG.d("Skipping sync due enforced quiet period");
                    return;
                }
            } else {
                // not forcing and no commands, we're done
                if (commands.size() == 0) {
                    return;
                }
            }

            updateStatus("Syncing");
            String network = prefs.getString(SettingsActivity.DEFAULT_NETWORK, "none");
            String fcmId = prefs.getString(SettingsActivity.FCM_ID, "");


            String uuid = RapidPro.get().getUUID();

            // no gcm id?  don't even try to sync
            if (fcmId.length() == 0) {
                return;
            }

            String relayerId = prefs.getString(SettingsActivity.RELAYER_ID, null);
            String secret = prefs.getString(SettingsActivity.RELAYER_SECRET, null);


            long lastAirplane = prefs.getLong(SettingsActivity.LAST_AIRPLANE_TOGGLE, -1L);
            long lastReceived = prefs.getLong(SettingsActivity.LAST_SMS_RECEIVED, 0);
            long now = System.currentTimeMillis();

            // if this sync was before we actually synced, ignore
            if (syncTime < lastSync) {
                updateStatus("");
                return;
            }

            String endpoint = RapidPro.get().getServerURL(context);

            // flip to whatever network is preferred by our user
            setNetworkType(network);

            SyncPayload payload = new SyncPayload();
            payload.addCommand(new FCM(fcmId, uuid));
            payload.addCommand(new StatusCommand(context));
            boolean synced = false;

            // we've got everything we need to do proper syncing, go for it
            if (fcmId != null && relayerId != null && secret != null && secret.length() > 0) {
                for (Command command : commands) {
                    payload.addCommand(command);
                }

                String url = endpoint + "/relayers/relayer/sync/" + relayerId + "/";
                synced = firePayload(payload, url, secret);
            }
            // we don't know our secret or relayer payloadId, we should register
            else if (fcmId != null) {
                updateStatus("Registering");

                RapidPro.LOG.d("Endpoint:" + endpoint);
                String url = endpoint + "/relayers/relayer/register/";
                synced = firePayload(payload, url, null);
            }

            // send any queued messages
            RapidPro.get().runCommands();

            // if we successfully synced, then update our last sync time
            if (synced) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).edit();
                editor.putLong(RapidPro.LAST_SYNC_TIME, syncTime);
                editor.apply();
            }

            // if we have messages in an errored state and haven't sent a message in over 10 minutes, then toggle
            // our airplane mode if we haven't done so recently
            int retry = DBCommandHelper.getCommandCount(context, DBCommandHelper.IN, MTTextMessage.RETRY, MTTextMessage.CMD);
            long lastSMSSent = prefs.getLong(SettingsActivity.LAST_SMS_SENT, 0);

            RapidPro.broadcastUpdatedCounts(context);

            // if there are more pending commands, force another sync to work our way through the queue
            commands = DBCommandHelper.getPendingCommands(context, DBCommandHelper.OUT, DBCommandHelper.BORN, 50, null, false);
            if (commands.size() > 10) {
                RapidPro.get().sync(true);
            }
        }
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
        context.sendBroadcast(statusIntent);
    }

    protected void updateStatus(String status){
        Intent statusIntent = new Intent(Intents.UPDATE_STATUS);
        statusIntent.putExtra(Intents.STATUS_EXTRA, status);
        context.sendBroadcast(statusIntent);

        RapidPro.broadcastUpdatedCounts(context);
    }

    private void setDataEnabled(boolean enabled) {
        try{
            ConnectivityManager conman = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
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
                } catch (Throwable ignored){}
                data = conman.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                sleeps++;
            }
        } catch (Throwable t){
            RapidPro.LOG.d("Error trying to turn on mobile data");
        }
    }

    private void setWifiEnabled(boolean enabled){
        // enable wifi
        WifiManager manager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        manager.setWifiEnabled(enabled);

        // grab our connectivity manager to test whether it has worked
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        // sleep up to 30 seconds for it to take effect
        int sleeps = 0;
        while (sleeps < 30 && wifi.isConnected() != enabled){
            try{
                Thread.sleep(1000);
            } catch (Throwable ignored){}
            wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            sleeps++;
        }
    }

    private void checkDataEnabled(boolean enable){
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
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
        WifiManager manager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
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
        switch (type) {
            case "wifi":
                checkWifiEnabled(true);
                break;
            case "data":
                checkDataEnabled(true);
                break;
            case "none":
                // last case is a no-op, we don't change anything
                break;
        }
    }

    private boolean toggleConnection(){
        WifiManager manager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
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
                        cmd.execute(context.getApplicationContext(), payload);
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
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).edit();
                editor.remove(SettingsActivity.RELAYER_SECRET);
                editor.remove(SettingsActivity.RELAYER_ID);
                editor.remove(SettingsActivity.RESET);
                editor.apply();
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

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).edit();
        editor.putBoolean(SettingsActivity.CONNECTION_UP, connectionOk);
        editor.apply();

        return synced;
    }
}
