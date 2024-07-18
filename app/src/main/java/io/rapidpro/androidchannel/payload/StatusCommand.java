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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.List;

import io.rapidpro.androidchannel.RapidPro;
import io.rapidpro.androidchannel.SettingsActivity;
import io.rapidpro.androidchannel.data.DBCommandHelper;
import io.rapidpro.androidchannel.json.JSON;

public class StatusCommand extends Command {
    public static final String CMD = "status";

    private String m_powerSource;
    private int m_powerLevel;
    private String m_powerStatus;

    private String m_networkType;
    private String m_countryCode;

    private List<Command> m_messagePendingCommand;
    private List<Command> m_messageRetryCommand;
    private List<Long> m_messagePendingIds = new ArrayList<Long>();
    private List<Long> m_messageRetryIds = new ArrayList<Long>();

    private int m_relayerOrg;

    private String m_device;
    private String m_os;
    private String m_appVersion;

    public static final String POWER_SOURCE = "p_src";
    public static final String POWER_LEVEL = "p_lvl";
    public static final String POWER_STATUS = "p_sts";

    public static final String NETWORK_TYPE = "net";
    public static final String RELAYER_ORG = "org_id";

    public static final String MESSAGE_PENDING = "pending";
    public static final String MESSAGE_RETRY = "retry";

    public static final String COUNTRY_CODE = "cc";

    // power source strings
    public static final String SOURCE_AC = "AC";
    public static final String SOURCE_USB = "USB";
    public static final String SOURCE_WIRELESS = "WIR";
    public static final String SOURCE_BATTERY = "BAT";

    // power status strings
    public static final String STATUS_UNKNOWN = "UNK";
    public static final String STATUS_CHARGING = "CHA";
    public static final String STATUS_DISCHARGING = "DIS";
    public static final String STATUS_NOT_CHARGING = "NOT";
    public static final String STATUS_FULL = "FUL";

    public static final String DEVICE = "dev";
    public static final String OS = "os";
    public static final String APP_VERSION = "app_version";

    public StatusCommand(Context context) {
        super(CMD);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int powerSourceId = preferences.getInt("powerSource", -1);
        switch (powerSourceId){
            case BatteryManager.BATTERY_PLUGGED_AC:
                m_powerSource = SOURCE_AC;
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                m_powerSource = SOURCE_USB;
                break;
            case 4: // this case because BATTERY_PLUGGED_WIRELESS is supported from API 17
                m_powerSource = SOURCE_WIRELESS;
                break;
            default: // this is the case of
                m_powerSource = SOURCE_BATTERY;
                break;
        }

        int powerStatusId = preferences.getInt("powerStatus", -1);
        switch (powerStatusId){
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
                m_powerStatus = STATUS_UNKNOWN;
                break;
            case BatteryManager.BATTERY_STATUS_CHARGING:
                m_powerStatus = STATUS_CHARGING;
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                m_powerStatus = STATUS_NOT_CHARGING;
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                m_powerStatus = STATUS_FULL;
                break;
            default:
                m_powerStatus = STATUS_DISCHARGING;
                break;
        }

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        m_countryCode = tm.getSimCountryIso();
        m_countryCode = (m_countryCode == null) ? "" : m_countryCode.toUpperCase();

        m_powerLevel = preferences.getInt("powerLevel", -1);
        m_networkType = preferences.getString("networkType", "");

        m_messagePendingCommand = DBCommandHelper.getPendingCommands(context, DBCommandHelper.IN, DBCommandHelper.BORN, -1, MTTextMessage.CMD, false);
        m_messagePendingCommand.addAll(DBCommandHelper.getPendingCommands(context, DBCommandHelper.IN, MTTextMessage.PENDING, -1, MTTextMessage.CMD, false));

        m_messageRetryCommand = DBCommandHelper.getPendingCommands(context, DBCommandHelper.IN, MTTextMessage.RETRY, -1, MTTextMessage.CMD, false);

        if (m_messagePendingCommand.size() != 0){
            RapidPro.LOG.d(String.format("m_messagePending has %d elements", m_messagePendingCommand.size()));
            for(Command cmd :m_messagePendingCommand){
                MTTextMessage pendingMessage = (MTTextMessage) cmd;
                m_messagePendingIds.add(pendingMessage.getServerId());
            }
        }

        if (m_messageRetryCommand.size() != 0){
            RapidPro.LOG.d(String.format("m_messagePending has %d elements", m_messageRetryCommand.size()));
            for (Command cmd :m_messageRetryCommand){
                MTTextMessage retryMessage = (MTTextMessage) cmd;
                m_messageRetryIds.add(retryMessage.getServerId());
            }
        }

        m_relayerOrg = preferences.getInt(SettingsActivity.RELAYER_ORG, -1);
        m_device = Build.MODEL;
        m_os = Build.VERSION.RELEASE;

        m_appVersion = RapidPro.get().getAppVersion();

    }

    @Override
    public void ack(Context context, Ack ack){
        // noop
    }


    @Override
    public JSON asJSON() {
        JSON json = new JSON();
        json.put(COMMAND, CMD);
        json.put(POWER_SOURCE, m_powerSource);
        json.put(POWER_LEVEL, m_powerLevel);
        json.put(POWER_STATUS, m_powerStatus);
        json.put(NETWORK_TYPE, m_networkType);
        json.put(MESSAGE_PENDING, m_messagePendingIds.toArray(new Long[m_messagePendingIds.size()]));
        json.put(MESSAGE_RETRY, m_messageRetryIds.toArray(new Long[m_messageRetryIds.size()]));
        json.put(RELAYER_ORG, m_relayerOrg);
        json.put(COUNTRY_CODE, m_countryCode);

        json.put(DEVICE, m_device);
        json.put(OS, m_os);
        json.put(APP_VERSION, m_appVersion);

        return json;
    }
}
