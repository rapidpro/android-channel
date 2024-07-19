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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

public class BaseActivity extends FragmentActivity {

    public void onCreate(Bundle bundle){
        super.onCreate(bundle);

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            RapidPro.LOG.w("Fetching FCM registration token failed " + task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String refreshedToken = task.getResult();

                        if (refreshedToken != null) {
                            SettingsActivity.setFCM(getApplicationContext(), refreshedToken);
                            RapidPro.get().sync(true);
                        }

                    }
                });

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        Toast.makeText(getApplicationContext(), "No menu available", Toast.LENGTH_SHORT).show();
        return false;
    }


    public void onShowSettings(View v){
        startActivity(new Intent(Intents.SHOW_SETTINGS));
    }

    public void onSync(View v){
        RapidPro.get().sync(true);
    }
}
