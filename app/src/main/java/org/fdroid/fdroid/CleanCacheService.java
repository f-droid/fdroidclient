package org.fdroid.fdroid;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.os.SystemClock;

import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Handles cleaning up caches files that are not going to be used, and do not
 * block the operation of the app itself.  For things that must happen before
 * F-Droid starts normal operation, that should go into
 * {@link FDroidApp#onCreate()}
 */
public class CleanCacheService extends IntentService {

    /**
     * Schedule or cancel this service to update the app index, according to the
     * current preferences. Should be called a) at boot, b) if the preference
     * is changed, or c) on startup, in case we get upgraded.
     */
    public static void schedule(Context context) {
        long keepTime = Preferences.get().getKeepCacheTime();
        long interval = 604800000; // 1 day
        if (keepTime < interval) {
            interval = keepTime * 1000;
        }

        Intent intent = new Intent(context, CleanCacheService.class);
        PendingIntent pending = PendingIntent.getService(context, 0, intent, 0);

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pending);
        alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 5000, interval, pending);
    }

    public CleanCacheService() {
        super("CleanCacheService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
        Utils.clearOldFiles(Utils.getApkCacheDir(this), Preferences.get().getKeepCacheTime());
        deleteStrayIndexFiles();
        deleteOldInstallerFiles();
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
            if (f.getName().startsWith("install-")) {
                FileUtils.deleteQuietly(f);
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
                FileUtils.deleteQuietly(f);
            }
            if (f.getName().startsWith("dl-")) {
                FileUtils.deleteQuietly(f);
            }
        }
    }
}
