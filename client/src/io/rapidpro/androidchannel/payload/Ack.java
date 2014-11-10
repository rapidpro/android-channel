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
import io.rapidpro.androidchannel.RapidPro;
import io.rapidpro.androidchannel.json.JSON;

public class Ack extends Command {

    public static final String CMD = "ack";

    private JSON m_extra;

    public Ack() {
        super(CMD);
    }

    public Ack(JSON json) {
        super(CMD);
        setPayloadId(json.getInt(PAYLOAD_ID));

        if (json.has(EXTRA)){
            m_extra = json.getJSON(EXTRA);
        }
    }

    public JSON getAckExtra(){ return m_extra; }

    public void execute(Context context, SyncPayload sent){
        Command sentCommand = sent.getCommand(getPayloadId());

        if (sentCommand != null){
            sentCommand.ack(context, this);
        } else {
            RapidPro.LOG.d("Couldn't find m_sent command for ack id: " + getPayloadId());
        }
    }

    @Override
    public JSON asJSON() {
        throw new RuntimeException("Ack commands are not seriazable.");
    }
}
