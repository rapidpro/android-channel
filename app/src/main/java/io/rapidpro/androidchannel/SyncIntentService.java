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

import android.content.Context;
import android.content.Intent;

import androidx.core.app.JobIntentService;

/**
 * Syncs our messages with the server.
 */
public class SyncIntentService extends JobIntentService {

    /**
     * Unique job ID for this service.
     */
    public static final int JOB_ID = 1001;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, SyncIntentService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(Intent intent) {

        boolean force = intent.getBooleanExtra(Intents.FORCE_EXTRA, false) || !RapidPro.get().isClaimed();
        long syncTime = intent.getLongExtra(Intents.SYNC_TIME, 0);

        SyncHelper syncHelper = new SyncHelper(this, syncTime, force);
        syncHelper.sync();
    }
}
