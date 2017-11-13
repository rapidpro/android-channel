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

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;

import io.rapidpro.androidchannel.RapidPro;
import io.rapidpro.androidchannel.json.JSON;

public class SyncPayload {
    public static final String CMDS = "cmds";

    public static final String ERROR = "error";
    public static final String ERROR_ID = "error_id";

    public static final int NO_ERROR = -1;
    public static final int INVALID_SECRET = 1;
    public static final int OLD_REQUEST = 3;

    public SyncPayload(){}

    public SyncPayload(JSON json){
        error = json.getString(ERROR, null);
        errorId = json.getInt(ERROR_ID, -1);

        if (json.has(CMDS)){
            for (JSON cmdJson: json.getJSONList(CMDS)){
                try{
                    Command cmd = Command.fromJSON(cmdJson);
                    commands.add(cmd);

                    if (cmdJson.has(Command.PAYLOAD_ID)){
                        cmd.setPayloadId((cmdJson.getInt(Command.PAYLOAD_ID)));
                    }
                } catch (Throwable t){
                    RapidPro.LOG.e("Error with command: " + cmdJson, t);
                }
            }
        }
    }

    public void addCommand(Command command){
        commands.add(command);
        int payloadId = commands.size();

        // set our payload id
        command.setPayloadId(payloadId);
        commandsByPayload.put(payloadId, command);
    }

    public Command getCommand(int payloadId){
        return commandsByPayload.get(payloadId);
    }

    public JSON asJSON(){
        JSON json = new JSON();

        JSONArray cmds = new JSONArray();
        for (Command cmd: commands){
            JSON cmdJSON = cmd.asJSON();

            // set the payload id in our json
            cmdJSON.put(Command.PAYLOAD_ID, cmd.getPayloadId());

            // and add the json to our list of commands
            cmds.put(cmdJSON.getObject());
        }

        json.put(CMDS, cmds);
        return json;
    }

    public ArrayList<Command> commands = new ArrayList<Command>();
    public HashMap<Integer, Command> commandsByPayload = new HashMap<Integer, Command>();

    public String error;
    public int errorId;
}
