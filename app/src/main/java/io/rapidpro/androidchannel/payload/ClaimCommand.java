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

package io.rapidpro.androidchannel.payload;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import io.rapidpro.androidchannel.Intents;
import io.rapidpro.androidchannel.SettingsActivity;
import io.rapidpro.androidchannel.json.JSON;

public class ClaimCommand extends Command {
    public static final String CMD = "claim";
    public static final String ORG_ID = "org_id";

    private int m_orgId;

    public ClaimCommand(JSON json){
        super(CMD);
        m_orgId = json.getInt(ORG_ID);
    }

    @Override
    public JSON asJSON() {
        JSON json = new JSON();
        json.put(COMMAND, CMD);
        json.put(ORG_ID, m_orgId);
        return json;
    }

    @Override
    public void execute(Context context, SyncPayload sent){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(SettingsActivity.RELAYER_ORG, m_orgId);
        editor.remove(SettingsActivity.RESET);
        editor.apply();

        // update our home activity
        Intent intent = new Intent(Intents.UPDATE_RELAYER_STATE);
        context.sendBroadcast(intent);
    }
}
