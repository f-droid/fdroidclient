package org.fdroid.fdroid;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;

/**
 * Interface between the new {@link android.app.job.JobScheduler} API and
 * {@link UpdateService}, which is based on {@link android.support.v4.app.JobIntentService}.
 *
 * @see <a href="https://developer.android.com/about/versions/android-5.0.html#Power">Project Volta: Scheduling jobs</a>
 */
@TargetApi(21)
public class UpdateJobService extends JobService {
    @Override
    public boolean onStartJob(final JobParameters params) {
        UpdateService.updateNow(this);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        UpdateService.stopNow(this);
        return true;
    }
}
