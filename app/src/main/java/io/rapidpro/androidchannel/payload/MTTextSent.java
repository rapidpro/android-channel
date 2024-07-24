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
 * Sent to the server when a command was successfully m_sent to a recipient.
 */
public class MTTextSent extends QueueingCommand {
    public static final String CMD = "mt_sent";

    private long m_messageId;
    private long m_sent;
    private String m_phone;

    public MTTextSent(long serverId, String phone){
        super(CMD);
        m_messageId = serverId;
        m_sent = System.currentTimeMillis();
        m_phone = phone;
    }

    public MTTextSent(JSON json){
        super(CMD);
        this.m_messageId = json.getLong(MESSAGE_ID);
        this.m_sent = json.getLong(SENT);
    }

    /**
     * The server acked this command, remove it from our list of commands.
     *
     * @param context
     * @param cmd
     */
    public void ack(Context context, Ack cmd){
        DBCommandHelper.markCommandComplete(context, getCommandId());
        DBCommandHelper.updateCommandStateWithServerId(context, MTTextMessage.CMD, m_messageId, MTTextMessage.SENT_SYNCED, null);
    }

    @Override
    public JSON asJSON() {
        JSON json = new JSON();
        json.put(COMMAND, CMD);
        json.put(MESSAGE_ID, m_messageId);
        json.put(SENT, m_sent);
        return json;
    }

    @Override
    public int getDirection() {
        return DBCommandHelper.OUT;
    }

    @Override
    public String getTitle() {
        return "Message Sent";
    }

    @Override
    public String getBody() {
        return "Message successfully sent to " + m_phone;
    }

    @Override
    public long getServerId() {
        return DBCommandHelper.NONE;
    }

    @Override
    public int isHidden() {
        return DBCommandHelper.HIDDEN;
    }
}
