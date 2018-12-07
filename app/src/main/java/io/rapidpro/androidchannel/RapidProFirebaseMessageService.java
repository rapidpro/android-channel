package io.rapidpro.androidchannel;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;


public class RapidProFirebaseMessageService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        RapidPro.LOG.d("\n===FCM OnMessageReceived: " + remoteMessage.toString());

        if (remoteMessage.getData().size() > 0) {

            Map<String, String> messageData = remoteMessage.getData();
            String msg = messageData.get("msg");

            RapidPro.LOG.d("\n===Message: " + msg);

            if (msg.equals("ping")) {
                RapidPro.LOG.d("\n===Ping received, taking no action.");
            } else if (msg.equals("sync")) {
                RapidPro.get().sync(true);
            }
        }
    }

    @Override
    public void onSendError(String s, Exception e) {
        RapidPro.LOG.d("\n===Error: for msg(" + s + ") " + e);
    }


    @Override
    public void onNewToken(String token) {
        RapidPro.LOG.d("\n===Registered: " +token);
        SettingsActivity.setFCM(getApplicationContext(),token);
    }

}