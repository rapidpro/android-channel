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
import io.rapidpro.androidchannel.data.DBCommandHelper;
import io.rapidpro.androidchannel.json.JSON;

/**
 * Represents call activity on the handset, this can be either an outgoing call
 * or an incoming call.
 */
public class CallLog extends QueueingCommand {

    public static final String CMD = "call";
    public static final String CALL_TYPE = "type";
    public static final String DURATION = "dur";

    private String m_phone;
    private String m_type;
    private long m_time;
    private int m_duration;

    public static final String UNKNOWN = "unk";
    public static final String INCOMING = "mo_call";
    public static final String OUTGOING = "mt_call";
    public static final String MISSED = "mo_miss";

    public CallLog(String type, String phone, long time, int duration) {
        super(CMD);
        m_type = type;
        m_phone = phone;
        m_time = time;
        m_duration = duration;
    }

    public CallLog(JSON json){
        super(CMD);
        m_phone = json.getString(PHONE);
        m_type = json.getString(CALL_TYPE);
        m_time = json.getLong(DATE);
        m_duration = json.getInt(DURATION);
    }

    public void ack(Context context, Ack cmd){
        DBCommandHelper.markCommandComplete(context, getCommandId());
    }

    @Override
    public JSON asJSON() {
        JSON json = new JSON();
        json.put(COMMAND, CMD);
        json.put(PHONE, m_phone);
        json.put(CALL_TYPE, m_type);
        json.put(DATE, m_time);
        json.put(DURATION, m_duration);
        return json;
    }

    @Override
    public int getDirection() {
        return DBCommandHelper.OUT;
    }

    @Override
    public String getTitle() {
        if (m_type.equals(INCOMING) || m_type.equals(MISSED)){
            return "From " + m_phone;
        } else {
            return "To " + m_phone;
        }
    }

    @Override
    public String getBody() {
        if (m_type.equals(INCOMING)){
            return "Incoming call from " + m_phone + " lasting " + m_duration + " seconds";
        } else if (m_type.equals(OUTGOING)){
            return "Outgoing call to " + m_phone + " lasting " + m_duration + " seconds";
        } else if (m_type.equals(MISSED)){
            return "Missed call from " + m_phone;
        } else {
            return "Call " + m_phone;
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
}
