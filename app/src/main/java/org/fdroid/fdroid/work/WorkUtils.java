package org.fdroid.fdroid.work;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;

import java.util.concurrent.TimeUnit;

public class WorkUtils {
    private static final String TAG = WorkUtils.class.getSimpleName();

    private WorkUtils() { }

    /**
     * Schedule or cancel a work request to update the app index, according to the
     * current preferences. Should be called a) at boot, b) if the preference
     * is changed, or c) on startup, in case we get upgraded.
     */
    public static void scheduleCleanCache(@NonNull final Context context) {
        final WorkManager workManager = WorkManager.getInstance(context);
        final long keepTime = Preferences.get().getKeepCacheTime();
        long interval = TimeUnit.DAYS.toMillis(1);
        if (keepTime < interval) {
            interval = keepTime;
        }

        final Constraints.Builder constraintsBuilder = new Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresBatteryNotLow(true);
        if (Build.VERSION.SDK_INT >= 23) {
            constraintsBuilder.setRequiresDeviceIdle(true);
        }
        final PeriodicWorkRequest cleanCache =
                new PeriodicWorkRequest.Builder(CleanCacheWorker.class, interval, TimeUnit.MILLISECONDS)
                        .setConstraints(constraintsBuilder.build())
                        .build();
        workManager.enqueueUniquePeriodicWork("clean_cache",
                ExistingPeriodicWorkPolicy.REPLACE, cleanCache);
        Utils.debugLog(TAG, "Scheduled periodic work for cleaning the cache.");
    }
}
