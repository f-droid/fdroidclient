package org.fdroid.fdroid;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;

/**
 * Shim to run {@link CleanCacheService} with {@link android.app.job.JobScheduler}
 */
@TargetApi(21)
public class CleanCacheJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        CleanCacheService.start(this);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }
}
