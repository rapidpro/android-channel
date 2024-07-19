package io.rapidpro.androidchannel;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.preference.PreferenceManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(FCMPingService.class.getSimpleName(), "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String refreshedToken = task.getResult();
                        if (refreshedToken != null){
                            FirebaseMessaging.getInstance().send(
                                    new RemoteMessage.Builder(refreshedToken + "@gcm.googleapis.com")
                                            .setMessageId("" + System.currentTimeMillis())
                                            .addData("msg", "ping")
                                            .addData("ts", "" + System.currentTimeMillis())
                                            .build());
                        }

                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                        editor.putLong(RapidPro.LAST_FCM_TIME, System.currentTimeMillis());
                        editor.apply();

                    }
                });
    }
}
