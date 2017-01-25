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
import io.rapidpro.androidchannel.RapidPro;
import io.rapidpro.androidchannel.data.DBCommandHelper;
import io.rapidpro.androidchannel.json.JSON;

/**
 * Releases this relayer from the clutches of the current account
 */
public class ReleaseCommand extends Command {

    public static final String CMD = "rel";
    public static final String RELAYER_ID = "relayer_id";
    private int m_relayerId;

    public ReleaseCommand(JSON json){
        super(CMD);
        m_relayerId = json.getInt(RELAYER_ID);
    }

    @Override
    public JSON asJSON() {
        JSON json = new JSON();
        json.put(COMMAND, CMD);
        json.put(RELAYER_ID, m_relayerId);
        return json;
    }

    @Override
    public void execute(Context context, SyncPayload sent){
        RapidPro.get().release(context);
    }
}
