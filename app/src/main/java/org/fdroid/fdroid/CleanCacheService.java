package org.fdroid.fdroid;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.installer.ApkCache;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Handles cleaning up caches files that are not going to be used, and do not
 * block the operation of the app itself.  For things that must happen before
 * F-Droid starts normal operation, that should go into
 * {@link FDroidApp#onCreate()}.
 * <p>
 * These files should only be deleted when they are at least an hour-ish old,
 * in case they are actively in use while {@code CleanCacheService} is running.
 * {@link #clearOldFiles(File, long)} checks the file age using access time from
 * {@link android.system.StructStat#st_atime} on {@link android.os.Build.VERSION_CODES#LOLLIPOP}
 * and newer.  On older Android, last modified time from {@link File#lastModified()}
 * is used.
 */
public class CleanCacheService extends JobIntentService {
    public static final String TAG = "CleanCacheService";

    private static final int JOB_ID = 0x982374;

    /**
     * Schedule or cancel this service to update the app index, according to the
     * current preferences. Should be called a) at boot, b) if the preference
     * is changed, or c) on startup, in case we get upgraded.
     */
    public static void schedule(Context context) {
        long keepTime = Preferences.get().getKeepCacheTime();
        long interval = TimeUnit.DAYS.toMillis(1);
        if (keepTime < interval) {
            interval = keepTime;
        }

        if (Build.VERSION.SDK_INT < 21) {
            Intent intent = new Intent(context, CleanCacheService.class);
            PendingIntent pending = PendingIntent.getService(context, 0, intent, 0);

            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarm.cancel(pending);
            alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + 5000, interval, pending);
        } else {
            Utils.debugLog(TAG, "Using android-21 JobScheduler for updates");
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            ComponentName componentName = new ComponentName(context, CleanCacheJobService.class);
            JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, componentName)
                    .setRequiresDeviceIdle(true)
                    .setRequiresCharging(true)
                    .setPeriodic(interval);
            if (Build.VERSION.SDK_INT >= 26) {
                builder.setRequiresBatteryNotLow(true);
            }
            jobScheduler.schedule(builder.build());

        }
    }

    public static void start(Context context) {
        enqueueWork(context, CleanCacheService.class, JOB_ID, new Intent(context, CleanCacheService.class));
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
        deleteExpiredApksFromCache();
        deleteStrayIndexFiles();
        deleteOldInstallerFiles();
        deleteOldIcons();
    }

    /**
     * All downloaded APKs will be cached for a certain amount of time, which is
     * specified by the user in the "Keep Cache Time" preference.  This removes
     * any APK in the cache that is older than that preference specifies.
     */
    private void deleteExpiredApksFromCache() {
        File cacheDir = ApkCache.getApkCacheDir(getBaseContext());
        clearOldFiles(cacheDir, Preferences.get().getKeepCacheTime());
    }

    /**
     * {@link org.fdroid.fdroid.installer.Installer} instances copy the APK into
     * a safe place before installing.  It doesn't clean up them reliably yet.
     */
    private void deleteOldInstallerFiles() {
        File filesDir = getFilesDir();
        if (filesDir == null) {
            return;
        }

        final File[] files = filesDir.listFiles();
        if (files == null) {
            return;
        }

        for (File f : files) {
            if (f.getName().endsWith(".apk")) {
                clearOldFiles(f, TimeUnit.HOURS.toMillis(1));
            }
        }
    }

    /**
     * Delete index files which were downloaded, but not removed (e.g. due to F-Droid being
     * force closed during processing of the file, before getting a chance to delete). This
     * may include both "index-*-downloaded" and "index-*-extracted.xml" files.
     * <p>
     * Note that if the SD card is not ready, then the cache directory will probably not be
     * available. In this situation no files will be deleted (and thus they may still exist
     * after the SD card becomes available).
     * <p>
     * This also deletes temp files that are created by
     * {@link org.fdroid.fdroid.net.DownloaderFactory#create(Context, String)}, e.g. "dl-*"
     */
    private void deleteStrayIndexFiles() {
        File cacheDir = getCacheDir();
        if (cacheDir == null) {
            return;
        }

        final File[] files = cacheDir.listFiles();
        if (files == null) {
            return;
        }

        for (File f : files) {
            if (f.getName().startsWith("index-")) {
                clearOldFiles(f, TimeUnit.HOURS.toMillis(1));
            }
            if (f.getName().startsWith("dl-")) {
                clearOldFiles(f, TimeUnit.HOURS.toMillis(1));
            }
        }
    }

    /**
     * Delete cached icons that have not been accessed in over a year.
     */
    private void deleteOldIcons() {
        clearOldFiles(Utils.getImageCacheDir(this), TimeUnit.DAYS.toMillis(365));
    }

    /**
     * Recursively delete files in {@code f} that were last used
     * {@code millisAgo} milliseconds ago.  On {@code android-21} and newer, this
     * is based on the last access of the file, on older Android versions, it is
     * based on the last time the file was modified, e.g. downloaded.
     *
     * @param f         The file or directory to clean
     * @param millisAgo The number of milliseconds old that marks a file for deletion.
     */
    public static void clearOldFiles(File f, long millisAgo) {
        if (f == null) {
            return;
        }
        long olderThan = System.currentTimeMillis() - millisAgo;
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files == null) {
                return;
            }
            for (File file : files) {
                clearOldFiles(file, millisAgo);
            }
            f.delete();
        } else if (Build.VERSION.SDK_INT < 21) {
            if (FileUtils.isFileOlder(f, olderThan)) {
                f.delete();
            }
        } else {
            CleanCacheService21.deleteIfOld(f, olderThan);
        }
    }
}