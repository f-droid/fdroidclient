package org.fdroid.fdroid;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;

/**
 * Interface between the new {@link android.app.job.JobScheduler} API and
 * our old {@link UpdateService}, which is based on {@link android.app.IntentService}.
 * This does not do things the way it should, e.g. stopping the job on
 * {@link #onStopJob(JobParameters)} and properly reporting
 * {@link #jobFinished(JobParameters, boolean)}, but this at least provides
 * the nice early triggering when there is good power/wifi available.
 *
 * @see <a href="https://developer.android.com/about/versions/android-5.0.html#Power">Project Volta: Scheduling jobs</a>
 */
@TargetApi(21)
public class UpdateJobService extends JobService {
    @Override
    public boolean onStartJob(final JobParameters params) {
        new Thread() {
            @Override
            public void run() {
                // faking the actually run time
                try {
                    startService(new Intent(UpdateJobService.this, UpdateService.class));
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // ignored
                } finally {
                    jobFinished(params, false);
                }
            }
        }.start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
