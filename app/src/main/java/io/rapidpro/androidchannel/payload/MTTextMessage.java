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
import android.telephony.SmsManager;
import io.rapidpro.androidchannel.SMSModem;
import io.rapidpro.androidchannel.RapidPro;
import io.rapidpro.androidchannel.data.DBCommandHelper;
import io.rapidpro.androidchannel.json.JSON;

public class MTTextMessage extends QueueingCommand {

    public static String CMD = "mt_sms";

    public static final int PENDING = 1;
    public static final int RETRY = 2;
    public static final int SENT = 3;
    public static final int SENT_SYNCED = 4;
    public static final int DELIVERED = 5;
    public static final int FAILED = 6;
    public static final int DELIVERED_SYNCED = DBCommandHelper.COMPLETE;
    public static final int FAILED_SYNCED = DBCommandHelper.COMPLETE + 1;
    public static final int ERROR = 7;

    public static final int MAX_SEND_TRIES = 10;

    private int m_serverId;
    private String m_phone;
    private String m_message;

    public MTTextMessage(String phone, String message, int serverId){
        super(CMD);
        m_phone = phone;
        m_message = message;
        m_serverId = serverId;
    }

    public MTTextMessage(JSON json){
        super(CMD);
        m_phone = json.getString(PHONE);
        m_message = json.getString(MESSAGE);
        m_serverId = json.getInt(MESSAGE_ID);
    }

    @Override
    public JSON asJSON() {
        JSON json = new JSON();
        json.put(COMMAND, CMD);
        json.put(PHONE, m_phone);
        json.put(MESSAGE, m_message);
        json.put(MESSAGE_ID, m_serverId);
        return json;
    }

    public String getPhone(){ return m_phone; }

    public void execute(Context context, SyncPayload payload){
        SMSModem modem = RapidPro.get().getModem();

        try{
            // figure out how many text messages it will take to send this
            SmsManager smsManager = SmsManager.getDefault();
            int numMessages = smsManager.divideMessage(m_message).size();

            String pack = RapidPro.get().getNextPack(numMessages);
            if (pack == null) {
                RapidPro.LOG.d("No packs available to service message, ignoring command for now");
                return;
            }

            RapidPro.get().addSendForPack(pack, numMessages);

            RapidPro.LOG.d("\n\n\n\nSending [" + m_serverId + "] - " + m_phone + " - "+ m_message + " (" + pack + ")  attempt: " + getExtra() + "\n\n");

            // first mark this message as handed off to android
            DBCommandHelper.updateCommandStateWithServerId(context, CMD, m_serverId, PENDING, null);

            // then actually hand it off
            modem.sendSms(context, m_phone, m_message, "" + m_serverId, pack);

            Thread.sleep(RapidPro.MESSAGE_RATE_LIMITER);
        } catch (Throwable t){
            RapidPro.LOG.e("Error [" + m_serverId + "] - " + m_phone + " - "+ m_message, t);
        }
    }

    @Override
    public int getDirection() {
        return DBCommandHelper.IN;
    }

    @Override
    public String getTitle() {
        return "To " + m_phone;
    }

    @Override
    public String getBody() {
        return m_message;
    }

    @Override
    public long getServerId() {
        return m_serverId;
    }

    @Override
    public int isHidden() {
        return DBCommandHelper.VISIBLE;
    }
}
