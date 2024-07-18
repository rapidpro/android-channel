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
import android.database.Cursor;
import io.rapidpro.androidchannel.data.DBCommandHelper;
import io.rapidpro.androidchannel.json.JSON;

public abstract class Command {

    public static final String COMMAND = "cmd";
    public static final String PAYLOAD_ID = "p_id";

    public static final String PHONE = "phone";
    public static final String MESSAGE_ID = "msg_id";
    public static final String MESSAGE = "msg";

    public static final String LOCAL_MESSAGE_ID = "loc_id";

    public static final String DATE = "ts";

    public static final String RECEIVED = "ts";
    public static final String SENT = "ts";
    public static final String DELIVERED = "ts";
    public static final String FAILED = "ts";

    public static final String EXTRA = "extra";

    /** our command name */
    private String m_command;

    /** our id in our command database */
    private long m_commandId;

    /** the id in the payload */
    private int m_payloadId;

    /** when this command was created */
    private long m_created;

    /** when the command was last modified */
    private long m_modified;

    /** The extra on this command */
    private String m_extra;

    public Command(String command){
        m_command = command;
        m_created = System.currentTimeMillis();
        m_modified = System.currentTimeMillis();
    }

    public String getCommand(){ return m_command; }

    public int getPayloadId(){ return m_payloadId; }
    public void setPayloadId(int id){ m_payloadId = id; }

    public long getCommandId(){ return m_commandId; }
    public void setCommandId(long id){ m_commandId = id; }

    public void setDates(long created, long modified) {
        m_created = created;
        m_modified = modified;
    }

    public long getCreated() {
        return m_created;
    }

    public long getModified() {
        return m_modified;
    }

    public void setExtra(String extra){
        m_extra = extra;
    }

    public String getExtra(){
        return m_extra;
    }

    /**
     * Executes this command.
     *
     * @param context Our local context
     * @param sent The payload that was m_sent to the server
     */
    public void execute(Context context, SyncPayload sent){
        throw new RuntimeException("Execute not supported on this command type: " + getCommand());
    }

    /**
     * Called when a command has been acknowledged by the server.
     *
     * @param ack The Ack as m_sent by the server
     */
    public void ack(Context context, Ack ack){
        throw new RuntimeException("Ack not supported on this command type: " + getCommand());
    }

    /**
     * Factory for commands given a JSON blob.  Throws an exception
     * if the passed in command is not recognized as something we know.
     *
     * @param json
     * @return
     */
    public static Command fromJSON(JSON json){
        String type = json.getString(COMMAND);
        Command cmd;

        switch (type) {
            case MTBroadcast.CMD:
                cmd = new MTBroadcast(json);
                break;
            case RegistrationCommand.CMD:
                cmd = new RegistrationCommand(json);
                break;
            case MOTextMessage.CMD:
                cmd = new MOTextMessage(json);
                break;
            case Ack.CMD:
                cmd = new Ack(json);
                break;
            case MTTextSent.CMD:
                cmd = new MTTextSent(json);
                break;
            case ReleaseCommand.CMD:
                cmd = new ReleaseCommand(json);
                break;
            case MTTextFailed.CMD:
                cmd = new MTTextFailed(json);
                break;
            case ClaimCommand.CMD:
                cmd = new ClaimCommand(json);
                break;
            case ResetCommand.CMD:
                cmd = new ResetCommand(json);
                break;
            default:
                throw new RuntimeException("Unknown command: " + json.toString());
        }

        return cmd;
    }

    /**
     * Factory for commands given a JSON blob.  Throws an exception
     * if the passed in command is not recognized as something we know.
     *
     * @param cursor The cursor to build our command off of
     * @return
     */
    public static Command fromCursor(Cursor cursor){
        JSON json = new JSON(cursor.getString(DBCommandHelper.BLOB_IDX));
        String type = cursor.getString(DBCommandHelper.CMD_IDX);
        Command cmd;

        if (type.equals(MTBroadcast.CMD)){
            cmd = new MTBroadcast(json);
        } else if (type.equals(RegistrationCommand.CMD)){
            cmd = new RegistrationCommand(json);
        } else if (type.equals(MOTextMessage.CMD)) {
            cmd = new MOTextMessage(json);
        } else if (type.equals(Ack.CMD)) {
            cmd = new Ack(json);
        } else if (type.equals(MTTextMessage.CMD)){
            cmd = new MTTextMessage(json);
        } else if (type.equals(MTTextSent.CMD)){
            cmd = new MTTextSent(json);
        } else if (type.equals(MTTextDelivered.CMD)){
            cmd = new MTTextDelivered(json);
        } else if (type.equals(MTTextFailed.CMD)){
            cmd = new MTTextFailed(json);
        } else if (type.equals(CallLog.CMD)){
            cmd = new CallLog(json);
        } else if (type.equals(ResetCommand.CMD)){
            cmd = new ResetCommand(json);
        } else {
            throw new RuntimeException("Unknown command: " + json.toString());
        }

        // set our db id for this command
        cmd.setCommandId(cursor.getInt(DBCommandHelper.ID_IDX));
        cmd.setDates(cursor.getLong(DBCommandHelper.CREATED_IDX), cursor.getLong(DBCommandHelper.MODIFIED_IDX));
        cmd.setExtra(cursor.getString(DBCommandHelper.EXTRA_IDX));
        return cmd;
    }

    public abstract JSON asJSON();

    public String toString(){
        return getCommand();
    }
}
