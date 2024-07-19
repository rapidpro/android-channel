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

import androidx.preference.PreferenceManager;

import io.rapidpro.androidchannel.Intents;
import io.rapidpro.androidchannel.SettingsActivity;
import io.rapidpro.androidchannel.data.DBCommandHelper;
import io.rapidpro.androidchannel.json.JSON;

public class RegistrationCommand extends QueueingCommand {

    public static final String CMD = "reg";
    public static final String SECRET = "relayer_secret";
    public static final String CLAIM_CODE = "relayer_claim_code";
    public static final String RELAYER_ID = "relayer_id";

    private int m_relayerId;
    private String m_relayerSecret;
    private String m_relayerClaimCode;

    public RegistrationCommand(JSON json){
        super(CMD);
        m_relayerId = json.getInt(RELAYER_ID);
        m_relayerSecret = json.getString(SECRET);
        m_relayerClaimCode = json.getString(CLAIM_CODE);
    }

    @Override
    public JSON asJSON() {
        JSON json = new JSON();
        json.put(COMMAND, CMD);
        json.put(RELAYER_ID, m_relayerId);
        json.put(SECRET, m_relayerSecret);
        json.put(CLAIM_CODE, m_relayerClaimCode);
        return json;
    }

    @Override
    public int getDirection() {
        return DBCommandHelper.IN;
    }

    @Override
    public String getTitle() {
        return "Phone Registration";
    }

    @Override
    public String getBody() {
        if (m_relayerClaimCode != null){
            return "Device successfully registered with server.  " +
                    m_relayerClaimCode.substring(0,3) + " " + m_relayerClaimCode.substring(3,6) + " " + m_relayerClaimCode.substring(6,9);
        } else {
            return "Device Registered with server.";
        }
    }

    @Override
    public long getServerId() {
        return DBCommandHelper.NONE;
    }

    @Override
    public int isHidden() {
        return DBCommandHelper.VISIBLE;
    }

    @Override
    public void execute(Context context, SyncPayload sent){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(SettingsActivity.RELAYER_SECRET, m_relayerSecret);
        editor.putString(SettingsActivity.RELAYER_ID, "" + m_relayerId);
        editor.putString(SettingsActivity.RELAYER_CLAIM_CODE, m_relayerClaimCode);
        editor.remove(SettingsActivity.RELAYER_ORG);
        editor.remove(SettingsActivity.RESET);

        editor.apply();

        // notify that our state has changed
        Intent intent = new Intent(Intents.UPDATE_RELAYER_STATE);
        intent.putExtra(Intents.CLAIM_CODE_EXTRA, m_relayerClaimCode);
        context.sendBroadcast(intent);

        DBCommandHelper.queueCommand(context, this, DBCommandHelper.COMPLETE);

        // clear previous registrations
        DBCommandHelper.trimCommands(context, DBCommandHelper.IN, DBCommandHelper.COMPLETE, CMD, 1);
    }
}
