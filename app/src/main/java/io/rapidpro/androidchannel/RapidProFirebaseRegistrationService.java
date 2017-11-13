package io.rapidpro.androidchannel;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class RapidProFirebaseRegistrationService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        RapidPro.LOG.d("\n===Registered: " + refreshedToken);
        SettingsActivity.setFCM(getApplicationContext(), refreshedToken);
    }

}
