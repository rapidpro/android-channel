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
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;

public class BaseActivity extends FragmentActivity {

    public void onCreate(Bundle bundle){
        super.onCreate(bundle);

        GCMRegistrar.checkDevice(this);
        GCMRegistrar.checkManifest(this);

        String regId = GCMRegistrar.getRegistrationId(this);
        if (regId.equals("")) {
            GCMRegistrar.register(this, Config.GCM_APP_ID);
        } else {
            regId = GCMRegistrar.getRegistrationId(this);
            SettingsActivity.setGCM(this, regId);
            RapidPro.get().sync(true);
        }
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
