package io.rapidpro.androidchannel;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.JobIntentService;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;


public class FCMPingService extends JobIntentService {

    /**
     * Unique job ID for this service.
     */
    static final int JOB_ID = 1000;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, FCMPingService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(Intent intent) {
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
        editor.putLong(RapidPro.LAST_FCM_TIME, System.currentTimeMillis());
        editor.commit();

    }
}
