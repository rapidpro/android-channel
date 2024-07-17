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

package io.rapidpro.androidchannel;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import io.rapidpro.androidchannel.data.DBCommandHelper;
import io.rapidpro.androidchannel.payload.*;

import java.util.Date;

public class SMSListener implements SMSModem.SmsModemListener {

    @Override
    public void onSMSSent(Context context, String token) {
        // Stop if RapidPro is paused
        if (RapidPro.get().isPaused()) return;

        RapidPro.LOG.d("SMS Sent: " + token);
        long msgId = Long.parseLong(token);

        DBCommandHelper.updateCommandStateWithServerId(context, MTTextMessage.CMD, msgId, MTTextMessage.SENT, null);

        // update our timestamp of when the last successful SMS was sent, we use this to determine if we
        // are having trouble sending messages
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(SettingsActivity.LAST_SMS_SENT, System.currentTimeMillis());
        editor.commit();

        MTTextMessage msg = (MTTextMessage) DBCommandHelper.withServerId(context, MTTextMessage.CMD, msgId);
        if (msg != null){
            DBCommandHelper.queueCommand(context, new MTTextSent(msgId, msg.getPhone()));

            RapidPro.get().refreshHome();
            RapidPro.broadcastUpdatedCounts(context);
            RapidPro.get().sync();
        }
    }

    @Override
    public void onSMSDelivered(Context context, String token) {
        RapidPro.LOG.d("SMS Delivered: " + token);
        long msgId = Long.parseLong(token);

        DBCommandHelper.updateCommandStateWithServerId(context, MTTextMessage.CMD, msgId, MTTextMessage.DELIVERED, null);

        MTTextMessage msg = (MTTextMessage) DBCommandHelper.withServerId(context, MTTextMessage.CMD, msgId);
        if (msg != null){
            DBCommandHelper.queueCommand(context, new MTTextDelivered(msgId, msg.getPhone()));

            RapidPro.get().refreshHome();
            RapidPro.broadcastUpdatedCounts(context);
            RapidPro.get().sync();
        }
    }

    @Override
    public void onSMSSendError(Context context, String token, String errorDetails) {
        long msgId = Long.parseLong(token);

        MTTextMessage msg = (MTTextMessage) DBCommandHelper.withServerId(context, MTTextMessage.CMD, msgId);
        if (msg != null){
            String extra = msg.getExtra();
            int tries = 0;
            try{ tries = Integer.parseInt(extra); } catch (Throwable t){}
            tries++;

            if (tries < MTTextMessage.MAX_SEND_TRIES){
                DBCommandHelper.updateCommandStateWithServerId(context, MTTextMessage.CMD, msgId, MTTextMessage.RETRY, "" + tries);
                RapidPro.LOG.d("SMS Send Error, try: " + tries + " Id: " + token + " details: " + errorDetails);
            } else {
                DBCommandHelper.updateCommandStateWithServerId(context, MTTextMessage.CMD, msgId, MTTextMessage.FAILED, null);
                RapidPro.LOG.d("Failure after " + tries + " tries.");
                DBCommandHelper.queueCommand(context, new MTTextFailed(msgId, msg.getPhone()));
                RapidPro.get().refreshHome();
                RapidPro.get().sync();
            }
        }

        RapidPro.broadcastUpdatedCounts(context);
    }

    @Override
    public void onSMSSendFailed(Context context, String token){
        // go through same path as send errors, we will continue trying up to 10 times
        onSMSSendError(context, token, "SMS Send Failed");
    }
}
