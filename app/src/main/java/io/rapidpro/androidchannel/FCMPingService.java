package io.rapidpro.androidchannel;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;


public class FCMPingService extends WakefulIntentService {

    public FCMPingService() {
        super(FCMPingService.class.getSimpleName());
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        if (refreshedToken != null){
            FirebaseMessaging.getInstance().send(
                    new RemoteMessage.Builder(refreshedToken + "@gcm.googleapis.com")
                            .setMessageId("" + System.currentTimeMillis())
                            .addData("msg", "ping")
                            .addData("ts", "" + System.currentTimeMillis())
                            .build());
        }

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putLong(RapidProAlarmListener.LAST_FCM_TIME, System.currentTimeMillis());
        editor.commit();

    }
}
