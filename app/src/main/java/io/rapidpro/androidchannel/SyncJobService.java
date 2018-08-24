package io.rapidpro.androidchannel;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

import static io.rapidpro.androidchannel.SyncHelper.POLLING_INTERVAL;

public class SyncJobService extends JobService {

    public static final int JOB_ID = 1005;

    @Override
    public boolean onStartJob(final JobParameters params) {
        new Thread() {
            public void run() {
                try {
                    RapidPro.LOG.d("Starting scheduled sync");
                    SyncHelper syncHelper = new SyncHelper(SyncJobService.this, System.currentTimeMillis(), true);
                    syncHelper.sync();
                } finally {
                    long interval = POLLING_INTERVAL * 1000;
                    JobInfo syncJob = new JobInfo.Builder(SyncJobService.JOB_ID, new ComponentName(getPackageName(), SyncJobService.class.getName()))
                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                            .setMinimumLatency(interval).setOverrideDeadline(interval).build();

                    JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
                    jobScheduler.schedule(syncJob);
                    jobFinished(params, false);
                }
            }
        }.start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        RapidPro.LOG.d("Stopping scheduled sync");
        return false;
    }
}
