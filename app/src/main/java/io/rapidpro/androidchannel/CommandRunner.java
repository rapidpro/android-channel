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

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import io.rapidpro.androidchannel.data.DBCommandHelper;
import io.rapidpro.androidchannel.payload.Command;
import io.rapidpro.androidchannel.payload.MTTextMessage;

import java.util.List;

public class CommandRunner extends WakefulIntentService {

    public CommandRunner() {
        super(CommandRunner.class.getSimpleName());
    }

    @Override
    protected void doWakefulWork(Intent intent) {

        // prune any of our pending messages we should give up on
        int pruned = DBCommandHelper.prunePendingMessages(this);
        RapidPro.LOG.d("Old pending messages updated to sent: " + pruned);

        int commandsRun = 0;
        int allotted = RapidPro.get().getSendCapacity();
        int sent = RapidPro.get().getTotalSentInWindow();
        int remaining = allotted - sent;
        if (remaining > 0) {
            // first any outgoing sends
            List<Command> sends = DBCommandHelper.getPendingCommands(this, DBCommandHelper.IN, DBCommandHelper.BORN, remaining, MTTextMessage.CMD, false);
            commandsRun += sends.size();
            executeCommands(sends);
        }

        // then all other commands
        List<Command> other_cmds = DBCommandHelper.getPendingCommands(this, DBCommandHelper.IN, DBCommandHelper.BORN, 50, MTTextMessage.CMD, true);
        commandsRun += other_cmds.size();
        executeCommands(other_cmds);

        // finally, look for retries
        sent = RapidPro.get().getTotalSentInWindow();
        remaining = allotted - sent;
        if (remaining > 0){
            List<Command> retries = DBCommandHelper.getRetryMessages(this, remaining);
            commandsRun += retries.size();
            executeCommands(retries);
        }

        if (commandsRun > 0){
            // trim our database
            DBCommandHelper.trimCommands(this);
            RapidPro.broadcastUpdatedCounts(this);
        }

        RapidPro.get().printDebug();

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).edit();
        editor.putLong(RapidProAlarmListener.LAST_RUN_COMMANDS_TIME, System.currentTimeMillis());
        editor.commit();
    }

    private void executeCommands(List<Command> cmds){
        for (Command cmd : cmds){
            try{
                cmd.execute(this, null);
            } catch (Throwable t){
                RapidPro.LOG.e("Error thrown executing command.", t);
            }
        }
    }
}
