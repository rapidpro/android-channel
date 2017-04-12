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

public interface Intents {
    public static final String START_SYNC = "io.rapidpro.androidchannel.StartSync";

    public static final String SHOW_SETTINGS = "io.rapidpro.androidchannel.ShowSettings";
    public static final String RUN_LOCAL_COMMANDS = "io.rapidpro.androidchannel.RunLocalCommands";
    public static final String PING_GCM = "io.rapidpro.androidchannel.PingGCM";

    public static final String UPDATE_COUNTS = "io.rapidpro.androidchannel.UpdateCounts";
    public static final String UPDATE_STATUS = "io.rapidpro.androidchannel.UpdateStatus";
    public static final String UPDATE_RELAYER_STATE = "io.rapidpro.androidchannel.UpdateRelayerState";
    public static final String CLAIM_CODE_EXTRA = "claimCode";
    public static final String STATUS_EXTRA = "status";
    public static final String CLAIMED_EXTRA = "claimed";

    public static final String SHOW_MESSAGES = "io.rapidpro.androidchannel.ShowMessages";
    public static final String STATUS_ID_EXTRA = "statusId";

    public static final String SENT_EXTRA = "sent";
    public static final String CAPACITY_EXTRA = "capacity";
    public static final String OUTGOING_EXTRA = "outgoing";
    public static final String INCOMING_EXTRA = "incoming";
    public static final String RETRY_EXTRA = "retry";
    public static final String SYNC_EXTRA = "sync";
    public static final String FORCE_EXTRA = "force";
    public static final String TICKLE_AIRPLANE = "airplane";
    public static final String CONNECTION_UP_EXTRA = "connectionUp";
    public static final String LAST_SMS_SENT = "lastSmsSent";
    public static final String LAST_SMS_RECEIVED = "lastSmsReceived";
    public static final String SYNC_TIME = "syncTime";
    public static final String IS_PAUSED = "rapidproApplicationPaused";
}
