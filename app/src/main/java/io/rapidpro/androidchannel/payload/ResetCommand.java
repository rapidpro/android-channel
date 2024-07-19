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
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import io.rapidpro.androidchannel.RapidPro;
import io.rapidpro.androidchannel.SettingsActivity;
import io.rapidpro.androidchannel.data.DBCommandHelper;
import io.rapidpro.androidchannel.json.JSON;

public class ResetCommand extends QueueingCommand {

    public static final String CMD = "reset";

    public ResetCommand(JSON json){
        super(CMD);
    }

    public ResetCommand(){
        super(CMD);
    }

    @Override
    public JSON asJSON() {
        JSON json = new JSON();
        json.put(COMMAND, CMD);
        return json;
    }

    @Override
    public int getDirection() {
        return DBCommandHelper.OUT;
    }

    @Override
    public String getTitle() {
        return "Resetting Device";
    }

    @Override
    public String getBody() {
        return "Removing device from RapidPro Server";
    }

    @Override
    public long getServerId() {
        return DBCommandHelper.NONE;
    }

    @Override
    public int isHidden() {
        return DBCommandHelper.VISIBLE;
    }

    /** Server says we've been reset, release everything */
    public void ack(Context context, Ack ack){
        RapidPro.get().release(context);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove(SettingsActivity.RESET);
        editor.apply();
    }
}
