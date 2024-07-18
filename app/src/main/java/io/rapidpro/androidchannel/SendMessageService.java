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

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;

public class SendMessageService extends IntentService {

    public static final String TAG = SendMessageService.class.getSimpleName();

    private static final String SMS_SENT_REPORT_ACTION = "io.rapidpro.androidchannel.SMS_SENT_REPORT";
    private static final String SMS_SENT_REPORT_TOKEN_EXTRA = "token";

    private static final String SMS_FAILED_REPORT_ACTION = "io.rapidpro.androidchannel.SMS_FAILED_REPORT";
    private static final String SMS_FAILED_REPORT_TOKEN_EXTRA = "token";

    private static final String SMS_DELIVERED_REPORT_ACTION = "io.rapidpro.androidchannel.SMS_DELIVERED_REPORT";
    private static final String SMS_DELIVERED_REPORT_TOKEN_EXTRA = "token";

    public SendMessageService() {
        super(SendMessageService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals("io.rapidpro.androidchannel.SendMessage")) {
            String address = intent.getStringExtra("address");

            ArrayList<String> message = intent.getStringArrayListExtra("message");
            String token = intent.getStringExtra("token");
            sendSms(getBaseContext(), address, message, token);
        }
    }

    public void sendSms(Context context, String address, ArrayList<String> message, String token) {
        if (message != null && address != null && token != null) {

            SmsManager smsManager = SmsManager.getDefault();

            // make sure we've got a plus for international format
            if (!address.startsWith("+") && address.length() > 10) {
                address = "+" + address;
            }

            final Intent intent = new Intent(SMS_SENT_REPORT_ACTION);
            intent.setData(Uri.fromParts("sms", token, ""));
            intent.putExtra(SMS_SENT_REPORT_TOKEN_EXTRA, token);
            final PendingIntent queueIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            final ArrayList<PendingIntent> queueingIntents = new ArrayList<PendingIntent>();
            for (int i = 0; i < message.size(); i++) {
                queueingIntents.add(queueIntent);
            }

            final Intent intent1 = new Intent(SMS_DELIVERED_REPORT_ACTION);
            intent1.setData(Uri.fromParts("sms", token, ""));
            intent1.putExtra(SMS_DELIVERED_REPORT_TOKEN_EXTRA, token);
            final PendingIntent deliveryIntent = PendingIntent.getBroadcast(context, 0, intent1, PendingIntent.FLAG_IMMUTABLE);
            final ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>();
            for (int i = 0; i < message.size(); i++){
                deliveryIntents.add(deliveryIntent);
            }

            Log.d(TAG, "Sending [" + intent.getData() + "] " + address + " - " + message.size());
            try{
                smsManager.sendMultipartTextMessage(address, null, message, queueingIntents, deliveryIntents);
            } catch (Throwable t){
                // Mark that the message failed to send
                final Intent failedIntent = new Intent(SMS_FAILED_REPORT_ACTION);
                failedIntent.setData(Uri.fromParts("sms", token, ""));
                failedIntent.putExtra(SMS_FAILED_REPORT_TOKEN_EXTRA, token);

                // send a broadcast out that this failed
                sendBroadcast(failedIntent);

                Log.e(TAG, "Error trying to send message, ignoring", t);
            }
        }
    }
}
