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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.telephony.SmsManager;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import io.rapidpro.androidchannel.json.JSON;

public final class SMSModem extends BroadcastReceiver {

    private static final String SMS_SENT_REPORT_ACTION = "io.rapidpro.androidchannel.SMS_SENT_REPORT";
	private static final String SMS_SENT_REPORT_TOKEN_EXTRA = "token";

    private static final String SMS_FAILED_REPORT_ACTION = "io.rapidpro.androidchannel.SMS_FAILED_REPORT";
    private static final String SMS_FAILED_REPORT_TOKEN_EXTRA = "token";

    private static final String SMS_DELIVERED_REPORT_ACTION = "io.rapidpro.androidchannel.SMS_DELIVERED_REPORT";
    private static final String SMS_DELIVERED_REPORT_TOKEN_EXTRA = "token";

    private static final String SENT_KEY = "SENT_KEY";
    private static final String DELIVERED_KEY = "DELIVERED_KEY";

    private static final String KEYS = "KEYS";
    private static final String VALUES = "VALUES";

	private final Context context;
	private final SmsManager smsManager;
	private final SmsModemListener listener;

    private Map<String, Long> m_pendingSent = new Hashtable<>();
    private Map<String, Long> m_pendingDelivered = new Hashtable<>();

	public interface SmsModemListener {
        void onSMSSent(Context context, String token);
        void onSMSDelivered(Context context, String token);
        void onSMSSendError(Context context, String token, String errorDetails);
        void onSMSSendFailed(Context context, String token);

	}

	public SMSModem(Context c, SmsModemListener l) {
		context = c;
		listener = l;
		smsManager = SmsManager.getDefault();

        final IntentFilter receivedFilter = new IntentFilter();
        receivedFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        context.registerReceiver(this, receivedFilter);

		final IntentFilter deliveryFilter = new IntentFilter();
        deliveryFilter.addAction(SMS_SENT_REPORT_ACTION);
        deliveryFilter.addAction(SMS_DELIVERED_REPORT_ACTION);
        deliveryFilter.addAction(SMS_FAILED_REPORT_ACTION);
        deliveryFilter.addDataScheme("sms");
        context.registerReceiver(this, deliveryFilter);

        final IntentFilter shuttingDownFilter = new IntentFilter();
        shuttingDownFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        shuttingDownFilter.addAction("android.intent.action.QUICKBOOT_POWEROFF");
        context.registerReceiver(this, shuttingDownFilter);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        String pendingSentString = prefs.getString(SENT_KEY, null);
        String pendingDeliveredString = prefs.getString(DELIVERED_KEY, null);
        if (pendingSentString != null) {
            m_pendingSent = deserializer(pendingSentString);
            editor.remove(SENT_KEY);
            RapidPro.LOG.d(String.format("Getting pendingSent from SENT_KEY %s in SharedPreferences", pendingSentString));
        }

        if (pendingDeliveredString != null){
            m_pendingDelivered = deserializer(pendingDeliveredString);
            editor.remove(DELIVERED_KEY);
            RapidPro.LOG.d(String.format("Getting pendingDelivered from DELIVERED_KEY %s in SharedPreferences", pendingDeliveredString));
        }
        editor.apply();
	}

    public void sendSms(Context c, String address, String message, String token, String pack) {
        RapidPro app = ((RapidPro)c.getApplicationContext());

        synchronized (app) {

            if (message != null && address != null && token != null) {

                final ArrayList<String> parts = smsManager.divideMessage(message);

                Intent sendMessageIntent = new Intent(c, SendMessageService.class);
                sendMessageIntent.setAction("io.rapidpro.androidchannel.SendMessage");
                sendMessageIntent.addCategory(pack);
                sendMessageIntent.putExtra("address", address);
                sendMessageIntent.putExtra("message", parts);
                sendMessageIntent.putExtra("token", token);

                m_pendingSent.put(token, (long) parts.size());

                // android only returns only one delivery reports for sendMultipleTextMessage
                m_pendingDelivered.put(token, 1L);
                c.startService(sendMessageIntent);
            }
        }
	}

	public void clear() {
		context.unregisterReceiver(this);
	}

	@Override
    public void onReceive(Context c, Intent intent) {
        final String action = intent.getAction();
        RapidPro.LOG.d("SMSModem got action: " + action + intent);
        if (action.equalsIgnoreCase(SMS_DELIVERED_REPORT_ACTION)) {
			final int resultCode = getResultCode();
            final String token = intent.getStringExtra(SMS_DELIVERED_REPORT_TOKEN_EXTRA);
            RapidPro.LOG.d("Deliver report, result code '" + resultCode	+ "', token '" + token + "' URI: " + intent.getData());
            if (resultCode == Activity.RESULT_OK){
                if (m_pendingDelivered.containsKey(token)) {
                    m_pendingDelivered.put(token, m_pendingDelivered.get(token) - 1);
                    if (m_pendingDelivered.get(token) == 0) {
                        m_pendingDelivered.remove(token);

                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(c).edit();

                        editor.putString(SENT_KEY, serializer(m_pendingSent));
                        editor.putString(DELIVERED_KEY, serializer(m_pendingDelivered));
                        editor.apply();

                        listener.onSMSDelivered(c, token);
                    }
                }
            }
        } else if (action.equalsIgnoreCase(SMS_SENT_REPORT_ACTION)) {
            final int resultCode = getResultCode();
            final String token = intent.getStringExtra(SMS_SENT_REPORT_TOKEN_EXTRA);
            RapidPro.LOG.d("Sent to queue report, result code '" + resultCode	+ "', token '" + token + "' URI: " + intent.getData());
            if (resultCode == Activity.RESULT_OK){
                if (m_pendingSent.containsKey(token)) {
                    m_pendingSent.put(token, m_pendingSent.get(token) - 1);
                    if (m_pendingSent.get(token) == 0) {
                        m_pendingSent.remove(token);

                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(c).edit();

                        editor.putString(SENT_KEY, serializer(m_pendingSent));
                        editor.putString(DELIVERED_KEY, serializer(m_pendingDelivered));
                        editor.apply();

                        listener.onSMSSent(c, token);
                    }
                }
            } else {
                if (m_pendingSent.containsKey(token)) {
                    m_pendingSent.remove(token);
                    m_pendingDelivered.remove(token);

                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(c).edit();

                    editor.putString(SENT_KEY, serializer(m_pendingSent));
                    editor.putString(DELIVERED_KEY, serializer(m_pendingDelivered));
                    editor.apply();

                    listener.onSMSSendError(c, token, extractError(resultCode, intent));
                }
            }
        } else if (action.equalsIgnoreCase(SMS_FAILED_REPORT_ACTION)) {
            final int resultCode = getResultCode();
            final String token = intent.getStringExtra(SMS_FAILED_REPORT_TOKEN_EXTRA);
            RapidPro.LOG.d("Sent to queue report, result code '" + resultCode	+ "', token '" + token + "' URI: " + intent.getData());

            if (m_pendingSent.containsKey(token)) {
                m_pendingSent.remove(token);
                m_pendingDelivered.remove(token);

                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(c).edit();
                editor.putString(SENT_KEY, serializer(m_pendingSent));
                editor.putString(DELIVERED_KEY, serializer(m_pendingDelivered));
                editor.apply();

                listener.onSMSSendFailed(c, token);
            }
        } else if (action.equals("android.intent.action.ACTION_SHUTDOWN") ||
                action.equals("android.intent.action.QUICKBOOT_POWEROFF")) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(c).edit();

            editor.putString(SENT_KEY, serializer(m_pendingSent));
            editor.putString(DELIVERED_KEY, serializer(m_pendingDelivered));
            RapidPro.LOG.d(String.format("Added the pendingSent %s and pendingDelivered %s to SharedPreferences", serializer(m_pendingSent), serializer(m_pendingDelivered)));
            editor.apply();

        }
	}

    // function to serialize keys and values of a HashMap into a string
    private String serializer(Map<String, Long> map) {
        JSON json = new JSON();

        String[] keys = new String[map.size()];
        String[] values = new String[map.size()];
        int i=0;

        synchronized(map){
            for (String key: map.keySet()){
                keys[i] = key;
                values[i] = map.get(key).toString();
                i++;
            }
        }
        json.put(KEYS, keys);
        json.put(VALUES, values);

        return json.toString();
    }

    // function to convert back a seriarized string into a HashMap
    private HashMap<String, Long> deserializer(String map_string) {
        HashMap<String, Long> output_map = new HashMap<>();

        JSON json = new JSON(map_string);

        String[] keys = json.getStringArray(KEYS);
        String[] values = json.getStringArray(VALUES);


        for (int i=0; i < keys.length; i++){
            output_map.put(keys[i], Long.parseLong(values[i]));
        }
        return output_map;
    }

	private String extractError(int resultCode, Intent i) {
		switch (resultCode) {
		case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
			if (i.hasExtra("errorCode")) {
				return String.valueOf(i.getIntExtra("errorCode",-1));
			} else {
				return "Unknown error. No 'errorCode' field.";
			}
		case SmsManager.RESULT_ERROR_NO_SERVICE:
			return "No service";
		case SmsManager.RESULT_ERROR_RADIO_OFF:
			return "Radio off";
		case SmsManager.RESULT_ERROR_NULL_PDU:
			return "PDU null";
		default:
			return "really unknown error";
		}
	}
}
