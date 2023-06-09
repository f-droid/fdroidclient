package org.fdroid.fdroid.work;

import android.content.Context;
import android.os.Process;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.installer.ApkCache;
import org.fdroid.fdroid.nearby.LocalRepoManager;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Deletes the built-up cruft left over from various processes.
 * <p>
 * The installation process downloads APKs to the cache, then when an APK is being
 * installed, it is copied into files for running the install. Installs can happen
 * fully in the background, so the user might clear the cache at any time, so the
 * APK cannot be installed from the cache. Also, F-Droid is not guaranteed to get
 * an event after the APK is installed, so that can't be used to delete the APK
 * from files when it is no longer needed. That's where CleanCacheWorker comes in,
 * it runs regularly to ensure things are cleaned up. If something blocks it from
 * running, then APKs can remain in {@link org.fdroid.fdroid.installer.ApkFileProvider}
 */
public class CleanCacheWorker extends Worker {
    public static final String TAG = "CleanCacheWorker";

    public CleanCacheWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Schedule or cancel a work request to clean up caches, according to the
     * current preferences. Should be called a) at boot, b) if the preference
     * is changed, or c) on startup, in case we get upgraded.
     */
    public static void schedule(@NonNull final Context context) {
        final WorkManager workManager = WorkManager.getInstance(context);
        final long keepTime = Preferences.get().getKeepCacheTime();
        long interval = TimeUnit.DAYS.toMillis(1);
        if (keepTime < interval) {
            interval = keepTime;
        }

        final Constraints.Builder constraintsBuilder = new Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresBatteryNotLow(true);
        constraintsBuilder.setRequiresDeviceIdle(true);
        final PeriodicWorkRequest cleanCache =
                new PeriodicWorkRequest.Builder(CleanCacheWorker.class, interval, TimeUnit.MILLISECONDS)
                        .setConstraints(constraintsBuilder.build())
                        .build();
        workManager.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, cleanCache);
        Utils.debugLog(TAG, "Scheduled periodic work for cleaning the cache.");
    }

    /**
     * Force a cache cleanup.  Since {@link #deleteOldInstallerFiles(Context)}
     * only deletes files older than an hour, any ongoing APK install processes
     * should not have their APKs are deleted out from under them.
     */
    public static void force(@NonNull final Context context) {
        OneTimeWorkRequest cleanCache = new OneTimeWorkRequest.Builder(CleanCacheWorker.class).build();
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.enqueueUniqueWork(TAG + ".force", ExistingWorkPolicy.KEEP, cleanCache);
        Utils.debugLog(TAG, "Enqueued forced run for cleaning the cache.");
    }

    @NonNull
    @Override
    public Result doWork() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
        try {
            final Context context = getApplicationContext();
            deleteExpiredApksFromCache(context);
            deleteStrayIndexFiles(context);
            deleteOldInstallerFiles(context);
            deleteOldIcons(context);
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }

    /**
     * All downloaded APKs will be cached for a certain amount of time, which is
     * specified by the user in the "Keep Cache Time" preference.  This removes
     * any APK in the cache that is older than that preference specifies.
     */
    static void deleteExpiredApksFromCache(@NonNull Context context) {
        File cacheDir = ApkCache.getApkCacheDir(context);
        clearOldFiles(cacheDir, Preferences.get().getKeepCacheTime());
    }

    /**
     * {@link org.fdroid.fdroid.installer.Installer} instances copy the APK into
     * a safe place before installing.  This only deletes files older than an
     * hour to avoid deleting APKs while they are still being installed.  This
     * also avoids deleting the nearby swap repo files since that might be
     * actively in use.
     */
    static void deleteOldInstallerFiles(@NonNull Context context) {
        File filesDir = context.getFilesDir();
        if (filesDir == null) {
            Utils.debugLog(TAG, "The files directory doesn't exist.");
            return;
        }

        final File[] files = filesDir.listFiles();
        if (files == null) {
            Utils.debugLog(TAG, "The files directory doesn't have any files.");
            return;
        }

        final List<String> webRootAssetFiles = Arrays.asList(LocalRepoManager.WEB_ROOT_ASSET_FILES);
        for (File f : files) {
            if (f.isFile() && !f.getName().endsWith(".html") && !webRootAssetFiles.contains(f.getName())) {
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
    static void deleteStrayIndexFiles(@NonNull Context context) {
        File cacheDir = context.getCacheDir();
        if (cacheDir == null) {
            Utils.debugLog(TAG, "The cache directory doesn't exist.");
            return;
        }

        final File[] files = cacheDir.listFiles();
        if (files == null) {
            Utils.debugLog(TAG, "The cache directory doesn't have files.");
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
    static void deleteOldIcons(@NonNull Context context) {
        clearOldFiles(Utils.getImageCacheDir(context), TimeUnit.DAYS.toMillis(365));
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
            Utils.debugLog(TAG, "No files to be cleared.");
            return;
        }
        long olderThan = System.currentTimeMillis() - millisAgo;
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files == null) {
                Utils.debugLog(TAG, "No more files to be cleared.");
                return;
            }
            for (File file : files) {
                clearOldFiles(file, millisAgo);
            }
            deleteFileAndLog(f);
        } else {
            Impl21.deleteIfOld(f, olderThan);
        }
    }

    private static void deleteFileAndLog(final File file) {
        file.delete();
        Utils.debugLog(TAG, "Deleted file: " + file);
    }

    private static class Impl21 {
        /**
         * Recursively delete files in {@code f} that were last used
         * {@code millisAgo} milliseconds ago.  On {@code android-21} and newer, this
         * is based on the last access of the file, on older Android versions, it is
         * based on the last time the file was modified, e.g. downloaded.
         *
         * @param file      The file or directory to clean
         * @param olderThan The number of milliseconds old that marks a file for deletion.
         */
        static void deleteIfOld(File file, long olderThan) {
            if (file == null || !file.exists()) {
                Utils.debugLog(TAG, "No files to be cleared.");
                return;
            }
            try {
                StructStat stat = Os.lstat(file.getAbsolutePath());
                if ((stat.st_atime * 1000L) < olderThan) {
                    deleteFileAndLog(file);
                }
            } catch (ErrnoException e) {
                Utils.debugLog(TAG, "An exception occurred while deleting: ", e);
            }
        }
    }
}
