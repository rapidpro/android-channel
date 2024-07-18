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

/**
 * A command that is queued for later execution.
 *
 * These commands must be able to convert themselves to JSON and will
 * be stored in databases.
 */
public abstract class QueueingCommand extends Command {

    public QueueingCommand(String cmd){
        super(cmd);
    }

    /**
     * This is the direction of the message from the perspective of the
     * relayer as it is reported to the server.
     *
     * e.g. an Outgoing message is going from the relayer to the server
     * and an incoming message is coming from the server and going to the
     * relayer.
     */
    public abstract int getDirection();
    public abstract String getTitle();
    public abstract String getBody();
    public abstract long getServerId();
    public abstract int isHidden();
}
