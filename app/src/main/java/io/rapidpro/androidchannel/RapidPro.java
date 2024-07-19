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

import android.app.Application;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.CallLog.Calls;
import android.provider.Telephony;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import io.rapidpro.androidchannel.data.DBCommandHelper;
import io.rapidpro.androidchannel.payload.MTTextMessage;
import io.rapidpro.androidchannel.payload.ResetCommand;


public class RapidPro extends Application {

    public static Logger LOG = new Logger();
    public static final boolean SHOW_WIRE = true;
    
    private static RapidPro s_this;

    public static final String LAST_PACK = "lastPackUsed";

    public static final String LAST_SYNC_TIME = "lastSync";
    public static final String LAST_FCM_TIME = "lastFcm";
    public static final String LAST_RUN_COMMANDS_TIME = "lastRun";
    public static final String FIRST_FCM_TIME = "firstGcm";
    public static final String SYNCING = "syncing";

    public static final int NOTIFICATION_ID = 1;
    public static final String NOTIFICATION_CHANNEL_ID = "RapidPro_notification_channel";
    public static final String NOTIFICATION_CHANNEL_NAME = "RapidPro";


    /** how many messages we are willing to send per pack per 30 minutes */
    public static int MESSAGE_THROTTLE = 30;
    public static int MESSAGE_THROTTLE_MINUTES = 30;
    public static long MESSAGE_THROTTLE_WINDOW = 1000 * 60 * (MESSAGE_THROTTLE_MINUTES + 2);
    public static final long MESSAGE_RATE_LIMITER = 1000;

    public static final String MESSAGE_PACK_VERSION_SUPPORTED = "1.2";

    public static final String PREF_LAST_UPDATE = "lastUpdate";

    private SMSModem m_modem;
    private CallObserver m_callObserver;
    private IncomingSMSObserver m_incomingSMSObserver;

    private List<String> m_installedPacks = new ArrayList<String>();

    private HashMap<String, ArrayList<Long>> m_sendReports = new HashMap<String, ArrayList<Long>>();

    @Override
    public void onCreate() {
        super.onCreate();

        PreferenceManager.setDefaultValues(this, R.layout.preference_settings, false);

        s_this = this;

    }

    public void registerObservers() {

        // register our sms modem
        m_modem = new SMSModem(this, new SMSListener());

        // register our Incoming SMS listener
        m_incomingSMSObserver = new IncomingSMSObserver();
        getContentResolver().registerContentObserver(Telephony.Sms.CONTENT_URI, true, m_incomingSMSObserver);

        // register our call listener
        m_callObserver = new CallObserver();
        getContentResolver().registerContentObserver(Calls.CONTENT_URI, true, m_callObserver);

        // register for device details
        IntentFilter statusChanged = new IntentFilter();
        statusChanged.addAction(Intent.ACTION_BATTERY_CHANGED);
        statusChanged.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        StatusReceiver receiver = new StatusReceiver();
        getBaseContext().registerReceiver(receiver, statusChanged);

        refreshInstalledPacks();

        updateNotification();

        JobInfo syncJob = new JobInfo.Builder(SyncJobService.JOB_ID, new ComponentName(getPackageName(), SyncJobService.class.getName()))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setOverrideDeadline(5000).build();

        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(syncJob);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, importance);
            notificationChannel.enableLights(true);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public boolean isResetting(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean(SettingsActivity.RESET, false);
    }

    public boolean isClaimed() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return !isResetting() && (prefs.getInt(SettingsActivity.RELAYER_ORG, -1) != -1);
    }

    public boolean isPaused() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean(SettingsActivity.IS_PAUSED, false);
    }

    public boolean isRegistered() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return !isResetting() && (prefs.getString(SettingsActivity.RELAYER_ID, null) != null);
    }

    public boolean hasFCM(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return !isResetting() && (prefs.getString(SettingsActivity.FCM_ID, "").length() > 0);
    }

    public void refreshInstalledPacks() {

        final PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        List<String> packs = new ArrayList<String>();
        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.startsWith("io.rapidpro.androidchannel") &&
                    packageInfo.packageName.indexOf("surveyor") <= 0) {
                packs.add(packageInfo.packageName);
            }
        }

        LOG.d("Found " + packs.size() + " installed messaging packs");
        for (String pack : packs) {
            LOG.d("   - " + pack);
        }

        m_installedPacks = packs;
    }

    public void updateNotification(){
        if (isPaused() || !isClaimed()){
            hideNotification();
        } else {
            showNotification();
        }
    }

    public void showNotification() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("RapidPro")
                        .setContentText("RapidPro is active and relaying messages.")
                        .setOngoing(true);

        Intent resultIntent = new Intent(this, HomeActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(HomeActivity.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(resultPendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());

    }

    public void hideNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);

    }

    public ArrayList<Long> getSendsForPack(String pack) {
        ArrayList<Long> sends = m_sendReports.get(pack);

        if (sends == null) {
            sends = new ArrayList<Long>();
            m_sendReports.put(pack, sends);
        }

        prunePack(sends);
        return sends;
    }

    public void addSendForPack(String pack, int numSends) {
        for (int i=0; i<numSends; i++){
            getSendsForPack(pack).add(System.currentTimeMillis());
        }
    }

    public int getTotalSent(){
        int totalSent = 0;
        List<String> packs = getInstalledPacks();
        for (int i=0; i<packs.size(); i++){
            String candidate = packs.get(i);
            totalSent += getSendsForPack(candidate).size();
        }
        return totalSent;
    }

    public int getSendCapacity(){
        List<String> packs = getInstalledPacks();
        return packs.size() * MESSAGE_THROTTLE;
    }

    private void prunePack(ArrayList<Long> sends) {
        // prune our list of messages which are too old
        long cutoff = System.currentTimeMillis() - MESSAGE_THROTTLE_WINDOW;
        for (Iterator<Long> it = sends.iterator(); it.hasNext();) {
            long send = it.next();
            if (send < cutoff) {
                it.remove();
            } else {
                break;
            }
        }
    }

    public String getNextPack(int numMessages) {
        List<String> packs = getInstalledPacks();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // start with the next pack in line
        int packNumber = (prefs.getInt(LAST_PACK, 0) + 1) % packs.size();

        for (int i=0; i<packs.size(); i++) {
            String candidate = packs.get(packNumber);
            if (getSendsForPack(candidate).size() + numMessages < RapidPro.MESSAGE_THROTTLE) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(LAST_PACK, packNumber);
                editor.apply();
                return candidate;
            }

            packNumber = (packNumber + 1) % packs.size();
        }

        return null;
    }

    public List<String> getInstalledPacks() {
        return m_installedPacks;
    }


    public static RapidPro get(){ return s_this; }

    public void refreshHome() {
        // trigger our home view to refresh
        Intent intent = new Intent(Intents.UPDATE_STATUS);
        sendBroadcast(intent);
    }

    public void sync(){
        sync(false);
    }

    public void sync(boolean force) {
        // Stop if RapidPro is paused
        if (isPaused()) return;

        Intent intent = new Intent(this, SyncIntentService.class);
        intent.putExtra(Intents.SYNC_TIME, System.currentTimeMillis());
        if (force){
            intent.putExtra(Intents.FORCE_EXTRA, true);
        }

        SyncIntentService.enqueueWork(this, intent);
    }

    public void pingFCM(){
        FCMPingService.enqueueWork(this, new Intent(this, FCMPingService.class));
    }

    public void runCommands(){
        CommandRunner.enqueueWork(this, new Intent(this, CommandRunner.class));
    }

    public SMSModem getModem() {
        return m_modem;
    }

    @Override
    public void onTerminate(){
        super.onTerminate();
        getContentResolver().unregisterContentObserver(m_callObserver);
        getContentResolver().unregisterContentObserver(m_incomingSMSObserver);
    }

    public int getTotalSentInWindow() {
        int count = 0;
        for (String pack : getInstalledPacks()) {
            count += getSendsForPack(pack).size();
        }
        return count;
    }

    /**
     * Pauses the RapidPro application
     */
    public void pause(){
        if (!isPaused()) {
            // set a flag in settings to pause
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putBoolean(SettingsActivity.IS_PAUSED, true);
            editor.apply();

            // hide the notification in status bar
            updateNotification();
            RapidPro.broadcastUpdatedCounts(this);

        }
    }

    /**
     * Resume the RapidPro application
     */
    public void resume() {
        if (isPaused()) {
            // set the pause flag to resume
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putBoolean(SettingsActivity.IS_PAUSED, false);
            editor.apply();

            // show notification
            updateNotification();
            RapidPro.broadcastUpdatedCounts(this);

            // force trigger a sync
            sync(true);
        }
    }

    /**
     * Triggers our relayer to be reset.
     */
    public void reset(){
        // remove all our previous commands
        DBCommandHelper.clearCommands(this);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();

        // trigger a reset
        editor.putBoolean(SettingsActivity.RESET, true);
        editor.putBoolean(SettingsActivity.IS_PAUSED, false);
        editor.apply();

        // queue our reset command
        DBCommandHelper.queueCommand(this, new ResetCommand());
        sync(true);
    }

    /**
     * Releases our relayer, resetting all messages and data.
     *
     * @param context
     */
    public static void release(Context context){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove(SettingsActivity.RELAYER_SECRET);
        editor.remove(SettingsActivity.RELAYER_ID);
        editor.remove(SettingsActivity.RELAYER_ORG);
        editor.apply();

        // remove all commands
        DBCommandHelper.clearCommands(context);

        // notify everybody that our state has changed
        Intent intent = new Intent(Intents.UPDATE_RELAYER_STATE);
        context.sendBroadcast(intent);
    }

    public static void broadcastUpdatedCounts(Context context){
        Intent intent = new Intent();
        intent.setAction(Intents.UPDATE_COUNTS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);

        int sent = RapidPro.get().getTotalSent();
        int capacity = RapidPro.get().getSendCapacity();
        int outgoing = DBCommandHelper.getCommandCount(context, DBCommandHelper.IN, DBCommandHelper.BORN, MTTextMessage.CMD) +
                DBCommandHelper.getCommandCount(context, DBCommandHelper.IN, MTTextMessage.PENDING, MTTextMessage.CMD);
        int incoming = DBCommandHelper.getMessagesReceivedInWindow(context);
        int retry = DBCommandHelper.getCommandCount(context, DBCommandHelper.IN, MTTextMessage.RETRY, MTTextMessage.CMD);
        int sync = DBCommandHelper.getCommandCount(context, DBCommandHelper.OUT, DBCommandHelper.BORN, null);

        intent.putExtra(Intents.SENT_EXTRA, sent);
        intent.putExtra(Intents.CAPACITY_EXTRA, capacity);
        intent.putExtra(Intents.OUTGOING_EXTRA, outgoing);
        intent.putExtra(Intents.INCOMING_EXTRA, incoming);
        intent.putExtra(Intents.RETRY_EXTRA, retry);
        intent.putExtra(Intents.SYNC_EXTRA, sync);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        intent.putExtra(Intents.CONNECTION_UP_EXTRA, prefs.getBoolean(SettingsActivity.CONNECTION_UP, true));
        intent.putExtra(Intents.LAST_SMS_SENT, prefs.getLong(SettingsActivity.LAST_SMS_SENT, 0));
        intent.putExtra(Intents.LAST_SMS_RECEIVED, prefs.getLong(SettingsActivity.LAST_SMS_RECEIVED, 0));
        intent.putExtra(Intents.IS_PAUSED, prefs.getBoolean(SettingsActivity.IS_PAUSED, false));

        context.sendBroadcast(intent);
    }

    private class DownloadPackFile extends AsyncTask<Integer, Integer, Long> {

        @Override
        protected Long doInBackground(final Integer... packs) {
            String packToInstall = packs[0].toString();

            String endpoint = RapidPro.get().getServerURL(getApplicationContext());
            String url = endpoint + "/android/?v=" + MESSAGE_PACK_VERSION_SUPPORTED + "&pack=" + packToInstall;

            String fileName = "Pack" + packToInstall + ".apk";
            final File file = new File(getExternalFilesDir("Download").getAbsolutePath(), fileName);

            if(file.exists()) {
                file.delete();
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setDescription(fileName);
            request.setTitle(fileName);
            request.setDestinationUri(Uri.fromFile(file));
            request.setVisibleInDownloadsUi(false);

            final DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            final Long enqueuedId = manager.enqueue(request);

            final DownloadManager.Query managerQuery = new DownloadManager.Query();
            managerQuery.setFilterById(enqueuedId);

            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {

                        Cursor cursor = manager.query(managerQuery);
                        if (cursor.moveToFirst()) {
                            int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                            if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    Uri apkUri = FileProvider.getUriForFile(getApplicationContext(), "io.rapidpro.androidchannel.provider", file);
                                    Intent installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                                    installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                                    installIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    startActivity(installIntent);

                                } else {
                                    Intent installIntent = new Intent(Intent.ACTION_VIEW);
                                    installIntent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                                    installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(installIntent);
                                }
                            }
                        }
                    }
                }
            };
            registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            return enqueuedId;
        }

    }

    public String getServerURL(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String endpoint = prefs.getString(SettingsActivity.SERVER, SyncHelper.ENDPOINT);
        String ip = prefs.getString(SettingsActivity.IP_ADDRESS, null);

        // if our endpoint is an ip, add :8000 to it
        if (endpoint.startsWith("ip")) {
            endpoint = "http://" + ip;
            if (!ip.contains(":")) {
                endpoint += ":8000";
            }
        }

        return endpoint;

    }

    public void installPack(final Context context){
        List<String> packs = getInstalledPacks();

        int packToInstall = 0;
        for (int i=1; i<=10; i++) {
            if (!packs.contains("io.rapidpro.androidchannel.pack" + i)) {
                packToInstall = i;
                break;
            }
        }

        if (packToInstall > 0) {
            new DownloadPackFile().execute(packToInstall);
        }
    }

    public String getUUID(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String uuid = prefs.getString(SettingsActivity.UUID, null);

        if (uuid == null){
            uuid = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(SettingsActivity.UUID, uuid);
            editor.apply();
        }

        return uuid;
    }

    public String refreshAppVersion(){
        final PackageManager pm = getPackageManager();
        PackageInfo pinfo = null;
        String appVersion = null;
        try {
            pinfo = pm.getPackageInfo(getPackageName(), 0);
            appVersion = pinfo.versionName;
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putString(SettingsActivity.APP_VERSION, appVersion);
            editor.apply();

        } catch (PackageManager.NameNotFoundException e) {
            RapidPro.LOG.e("Error getting package version name.", e);
        }
        return appVersion;
    }

    public String getAppVersion() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String appVersion = prefs.getString(SettingsActivity.APP_VERSION, null);

        if (appVersion == null){
            appVersion = refreshAppVersion();
        }

        return appVersion;
    }

    public void printDebug() {
        // some debug metrics
        int allotted = ((RapidPro)getApplicationContext()).getInstalledPacks().size() * RapidPro.MESSAGE_THROTTLE;
        int sent = RapidPro.get().getTotalSentInWindow();
        int born = DBCommandHelper.getPendingCommands(this, DBCommandHelper.IN, DBCommandHelper.BORN, -1, MTTextMessage.CMD, false).size();
        int pending = DBCommandHelper.getPendingCommands(this, DBCommandHelper.IN, MTTextMessage.PENDING, -1, MTTextMessage.CMD, false).size();
        int retry = DBCommandHelper.getPendingCommands(this, DBCommandHelper.IN, MTTextMessage.RETRY, -1, MTTextMessage.CMD, false).size();

        RapidPro.LOG.d("\n\n============================================================================");
        RapidPro.LOG.d(sent + " of " + allotted + " messages in last 30 minutes.");
        RapidPro.LOG.d("  Born    : " + born);
        RapidPro.LOG.d("  Pending : " + pending);
        RapidPro.LOG.d("  Retry   : " + retry);
        RapidPro.LOG.d("\n");

        for (String pack : RapidPro.get().getInstalledPacks()) {
            RapidPro.LOG.d("   > " + RapidPro.get().getSendsForPack(pack).size() + ": "  + pack);
        }
        RapidPro.LOG.d("\n\n");
    }

}
