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

import java.util.ArrayList;
import java.util.List;

import io.rapidpro.androidchannel.RapidPro;
import io.rapidpro.androidchannel.data.DBCommandHelper;
import io.rapidpro.androidchannel.json.JSON;

public class MTBroadcast extends Command {
    public static final String CMD = "mt_bcast";

    public static final String ID = "id";
    public static final String TO = "to";
    public static final String PHONE = "phone";
    public static final String MSG = "msg";

    public int m_id;
    public String m_msg;
    public List<To> m_tos = new ArrayList<To>();

    public MTBroadcast(JSON json){
        super(CMD);
        m_msg = json.getString(MSG);

        for(JSON to: json.getJSONList(TO)){
            m_tos.add(new To(to));
        }
    }

    /**
     * Adds an outgoing command to our database
     **/
    @Override
    public void execute(Context context, SyncPayload sent){
        for(To to: m_tos){
            MTTextMessage msg = (MTTextMessage) DBCommandHelper.withServerId(context, MTTextMessage.CMD, to.getId());

            // only add the command if we don't already know about it
            if (msg == null){
                msg = new MTTextMessage(to.getPhone(), m_msg, to.getId());
                DBCommandHelper.queueCommand(context, msg);
            } else {
                // if this message has already been sent, add a command to notify the server
                int state = DBCommandHelper.getCommandState(context, MTTextMessage.CMD, to.getId());

                // RapidPro.LOG.d("Stale message: " + state + " for " + msg.getPhone() + " id: " + to.getId());
                if (state != MTTextMessage.FAILED) {
                    // we consider this message has been sent, but the server doesn't know it, resend our sync for send
                    if (state >= MTTextMessage.SENT && state <= MTTextMessage.DELIVERED_SYNCED) {
                        RapidPro.LOG.d("Resending SENT notification for " + msg.getPhone() + " id: " + to.getId());
                        DBCommandHelper.queueCommand(context, new MTTextSent(to.getId(), msg.getPhone()));
                    }

                    // we consider this message has been delivered, resend our sync for delivered
                    if (state >= MTTextMessage.DELIVERED && state <= MTTextMessage.DELIVERED_SYNCED) {
                        RapidPro.LOG.d("Resending DELIVERED notification for " + msg.getPhone() + " id: " + to.getId());
                        DBCommandHelper.queueCommand(context, new MTTextDelivered(to.getId(), msg.getPhone()));
                    }
                }

                // Resend from server
                if (state == MTTextMessage.FAILED || state == MTTextMessage.FAILED_SYNCED) {
                    RapidPro.LOG.d("Server request to resend for " + msg.getPhone() + " id: " + to.getId());
                    DBCommandHelper.updateCommandStateWithServerId(context, MTTextMessage.CMD, to.getId(), MTTextMessage.RETRY, "" + 0);
                    msg = (MTTextMessage) DBCommandHelper.withServerId(context, MTTextMessage.CMD, to.getId());
                    DBCommandHelper.queueCommand(context, msg);
                }
            }
        }
    }

    static class To {
        long m_id;
        String m_phone;

        public To(JSON json){
            m_id = json.getLong(ID);
            m_phone = json.getString(PHONE);
        }

        public long getId(){ return m_id; }
        public String getPhone(){ return m_phone; }
    }

    public JSON asJSON(){
        throw new RuntimeException("MTBroadcast messages should not be serialized");
    }
}
