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
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import io.rapidpro.androidchannel.ui.UpdatingTextView;
import io.rapidpro.androidchannel.util.DateUtil;

public class HomeActivity extends BaseActivity implements Intents {

    private static final String FRAGMENT_MESSAGE_LIST = "fragmentMessageList";
    private static final String FRAGMENT_UNCLAIMED = "fragmentUnclaimed";
    private static final String FRAGMENT_DASHBOARD = "fragmentDashboard";
    private static final String FRAGMENT_UNREGISTERED = "fragmentUnregistered";
    private static final String FRAGMENT_NO_FCM = "fragmentNoFCM";
    private static final String FRAGMENT_RESETTING = "fragmentResetting";

    public static final String SHOW_ADVANCED_SETTINGS = "showAdvancedSettings";

    // initialize m_touch_count to 8 to force exactly ten touch from the user
    private int m_debugTapsRemaining = 8;
    private long m_lastTouch = 0l;

    // private TextView m_secret;
    private TextView m_status;

    private static HomeActivity s_this;
    private DashboardReceiver m_receiver;

    private LinearLayout m_statusBar;

    private View m_lastUpdated;
    private View m_lastUpdate;

    private View m_logo;
    private View m_settings;

    private TextView m_outgoingCount;
    private TextView m_retryCount;
    private TextView m_syncCount;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.home);

        m_lastUpdated = findViewById(R.id.last_updated);
        m_lastUpdate = findViewById(R.id.last_update);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        Boolean showAdvancedSettings = prefs.getBoolean(SHOW_ADVANCED_SETTINGS, false);


        if (!showAdvancedSettings) {
            m_logo = findViewById(R.id.logo);

            m_logo.setOnTouchListener(new View.OnTouchListener(){
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    int action = MotionEventCompat.getActionMasked(motionEvent);

                    switch(action) {
                        case (MotionEvent.ACTION_DOWN) :
                            long now = System.currentTimeMillis();
                            if (now - m_lastTouch < 2000){
                                if (m_debugTapsRemaining >= 1) {
                                    m_debugTapsRemaining--;
                                } else {
                                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                                    editor.putBoolean(SHOW_ADVANCED_SETTINGS, true);
                                    editor.commit();
                                    Toast.makeText(getApplicationContext(), "Advanced Settings Activated", Toast.LENGTH_SHORT).show();
                                }
                            }  else {
                                // reset m_touch_count to 8 to force exactly 10 touch from the user
                                m_debugTapsRemaining = 8;
                            }
                            m_lastTouch = now;

                            return false;

                        default :
                            return false;
                    }
                }
            });
        }

        m_settings = findViewById(R.id.settings_icon);
        m_settings.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = MotionEventCompat.getActionMasked(motionEvent);

                switch(action) {
                    case (MotionEvent.ACTION_DOWN) :
                        m_settings.setBackgroundColor(Color.argb(32, 255, 255, 255));
                        return false;
                    case (MotionEvent.ACTION_UP):
                        m_settings.setBackgroundColor(Color.TRANSPARENT);
                        return false;
                    default:
                        return false;

                }

            }
        });

        m_status = (TextView)findViewById(R.id.status);
        m_statusBar = (LinearLayout)findViewById(R.id.status_bar);

        s_this = this;

        m_outgoingCount = (TextView) findViewById(R.id.outgoing_count);
        m_retryCount = (TextView) findViewById(R.id.retry_count);
        m_syncCount = (TextView) findViewById(R.id.sync_count);

        // initialize our dashboard
        updateClaimCode(this);
    }

    public void onResume() {
        super.onResume();

        // listen for dashboard update events
        m_receiver = new DashboardReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.UPDATE_STATUS);
        filter.addAction(Intents.UPDATE_COUNTS);
        filter.addAction(Intents.UPDATE_RELAYER_STATE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(m_receiver, filter);

        RapidPro.broadcastUpdatedCounts(this);

        updateClaimCode(this);

        // if we aren't claimed, force a sync
        if (!RapidPro.get().isClaimed()){
            RapidPro.get().sync(true);
        }
    }

    public void onPause() {
        super.onPause();
        unregisterReceiver(m_receiver);
    }

    public void setUpdatedVisibility(int visibility){
        m_lastUpdate.setVisibility(visibility);
        m_lastUpdated.setVisibility(visibility);
    }

    public void showResetting(){
        setUpdatedVisibility(View.GONE);
        showFragment(new ResettingFragment(), FRAGMENT_RESETTING, null, true);
        m_statusBar.setVisibility(View.GONE);
    }

    public void showNoFCM(){
        setUpdatedVisibility(View.GONE);
        showFragment(new NoFcmFragment(), FRAGMENT_NO_FCM, null, true);
        m_statusBar.setVisibility(View.GONE);
    }

    public void showUnclaimed() {
        setUpdatedVisibility(View.GONE);
        showFragment(new UnclaimedFragment(), FRAGMENT_UNCLAIMED, null, true);
        m_statusBar.setVisibility(View.GONE);
    }

    public void showDashboard() {
        showFragment(new DashboardFragment(), FRAGMENT_DASHBOARD, null, true);
        setUpdatedVisibility(View.VISIBLE);
        m_statusBar.setVisibility(View.VISIBLE);
    }

    public void showUnregistered() {
        setUpdatedVisibility(View.GONE);
        showFragment(new UnregisteredFragment(), FRAGMENT_UNREGISTERED, null, true);
        m_statusBar.setVisibility(View.GONE);
    }

    public void showList(){
        if (!isFragmentVisible(FRAGMENT_MESSAGE_LIST)){
            Bundle args = new Bundle();
            showFragment(new MessageListFragment(), FRAGMENT_MESSAGE_LIST, args, false);
        }
    }

    public void updateLastSync(UpdatingTextView view) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (RapidPro.get().isClaimed()) {
            long lastUpdate = prefs.getLong(RapidProAlarmListener.LAST_SYNC_TIME, 0);
            if (lastUpdate == 0) {
                view.setText("Waiting..");
            } else {
                view.setText(DateUtil.getFuzzyTime(lastUpdate));
            }
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    public void onInstallPack(View v){
        RapidPro.get().installPack(this);
    }

    public void pauseRapidProApp(View v) {
        RapidPro.get().pause();
    }

    public void resumeRapidProApp(View v) {
        RapidPro.get().resume();
    }

    public void onDestroy(){
        super.onDestroy();
    }

    public void onStatusClick(View view) {
        showList();
    }

    private void showFragment(Fragment fragment, String tag,  Bundle args, boolean clearBackStack) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();

        if (clearBackStack) {
            manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            ft.addToBackStack(null);
        }

        ft.setCustomAnimations(0, 0);

        if (args != null) {
            fragment.setArguments(args);
        }

        ft.replace(R.id.content_view, fragment, tag).commit();
    }

    public void updateStatusBar(Intent intent){
        m_outgoingCount.setText("" + intent.getIntExtra(OUTGOING_EXTRA, 0));
        m_retryCount.setText("" + intent.getIntExtra(RETRY_EXTRA, 0));
        m_syncCount.setText("" + intent.getIntExtra(SYNC_EXTRA, 0));
    }


    public void updateClaimCode(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String secret = prefs.getString(SettingsActivity.RELAYER_CLAIM_CODE, null);

        Intent intent = new Intent(UPDATE_RELAYER_STATE);
        intent.putExtra(CLAIM_CODE_EXTRA, secret);
        context.sendBroadcast(intent);
    }

    private boolean isFragmentVisible(String tag) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        return fragment != null && fragment.isVisible();
    }

    class DashboardReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intents.UPDATE_COUNTS)){
                updateStatusBar(intent);
            }

            if (intent.getAction().equals(Intents.UPDATE_STATUS)){
                if (intent.hasExtra(STATUS_EXTRA)){
                    String status = intent.getStringExtra(STATUS_EXTRA);
                    m_status.setText(status.toUpperCase());
                }
            }

            if (intent.getAction().equals(Intents.UPDATE_RELAYER_STATE)){
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                RapidPro.get().updateNotification();

                // we are resetting, show that progress
                if (RapidPro.get().isResetting()){
                    if (!isFragmentVisible(FRAGMENT_RESETTING)) {
                        showResetting();
                    }
                }
                // we don't have a FCM id, oh noes, show that progress
                if (!RapidPro.get().hasFCM()){
                    if (!isFragmentVisible(FRAGMENT_NO_FCM)) {
                        showNoFCM();
                    }
                }
                // if we don't have a secret, time to register
                else if (!RapidPro.get().isRegistered()){
                    if (!isFragmentVisible(FRAGMENT_UNREGISTERED)) {
                        showUnregistered();
                        RapidPro.get().sync();
                    }
                }
                // we don't have an org, but we have a secret, show that state
                else if (!RapidPro.get().isClaimed()){
                    if (!isFragmentVisible(FRAGMENT_UNCLAIMED)){
                        showUnclaimed();
                    }
                }
                else {
                    if (!isFragmentVisible(FRAGMENT_DASHBOARD)){
                        showDashboard();
                    }
                }
            }
        }
    }
}
