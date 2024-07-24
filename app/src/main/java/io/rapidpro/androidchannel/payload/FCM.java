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

import io.rapidpro.androidchannel.json.JSON;

public class FCM extends Command {
    public static final String CMD = "fcm";
    public static final String FCM_ID = "fcm_id";
    public static final String UUID = "uuid";

    private String m_fcmId;
    private String m_uuid;

    public FCM(String fcmId, String uuid){
        super(CMD);
        m_fcmId = fcmId;
        m_uuid = uuid;
    }

    public JSON asJSON(){
        JSON json = new JSON();
        json.put(COMMAND, CMD);
        json.put(FCM_ID, m_fcmId);
        json.put(UUID, m_uuid);
        return json;
    }

    @Override
    public void ack(Context context, Ack ack){
        // no-op for this command
    }
}
