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

package io.rapidpro.androidchannel.data;

import android.content.Context;
import android.database.Cursor;
import io.rapidpro.androidchannel.json.JSON;
import io.rapidpro.androidchannel.payload.Command;

/**
 * Our DB representation of a command.
 *
 * This is mostly just a struct for storing the command parameters.
 *
 */
public class DBCommand {

    /** db id for this command command */
    private long m_id;

    /** the command keyword */
    private String m_cmd;

    /** our blob of JSON that represents this command */
    private String m_blob;

    /**
     * Explicit constructor, usually called by the DB layer
     *
     * @param id
     * @param cmd
     * @param blob
     */
    public DBCommand(long id, String cmd, String blob){
        m_id = id;
        m_cmd = cmd;
        m_blob = blob;
    }

    public long getId(){ return m_id; }
    public String getCommand(){ return m_cmd; }
    public String getBlob(){ return m_blob; }
}
