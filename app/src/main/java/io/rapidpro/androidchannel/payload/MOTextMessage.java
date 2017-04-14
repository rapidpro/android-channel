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

import java.util.Date;

public class MOTextMessage extends QueueingCommand {
    public static final String CMD = "mo_sms";

    private String m_msg;
    private String m_phone;
    private Date m_received;

    public MOTextMessage(String phone, String msg, Date received){
        super(CMD);
        m_phone = phone;
        m_msg = msg;
        m_received = received;
    }

    public MOTextMessage(JSON json) {
        super(CMD);
        m_msg = json.getString(MESSAGE);
        m_phone = json.getString(PHONE);
        m_received = new Date(json.getLong(RECEIVED));
    }

    public void ack(Context context, Ack ack){
        DBCommandHelper.markCommandComplete(context, getCommandId());
    }

    public JSON asJSON() {
        JSON json = new JSON();
        json.put(COMMAND, CMD);
        json.put(PHONE, m_phone);
        json.put(RECEIVED, m_received.getTime());
        json.put(MESSAGE,  m_msg);
        return json;
    }

    @Override
    public int getDirection() {
        return DBCommandHelper.OUT;
    }

    @Override
    public String getTitle() {
        return "From " + m_phone;
    }

    @Override
    public String getBody() {
        return m_msg;
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
