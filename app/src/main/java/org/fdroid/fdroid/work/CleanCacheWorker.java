package org.fdroid.fdroid.work;

import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.installer.ApkCache;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class CleanCacheWorker extends Worker {
    private static final String TAG = CleanCacheWorker.class.getSimpleName();

    public CleanCacheWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
        try {
            deleteExpiredApksFromCache();
            deleteStrayIndexFiles();
            deleteOldInstallerFiles();
            deleteOldIcons();
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
    private void deleteExpiredApksFromCache() {
        File cacheDir = ApkCache.getApkCacheDir(getApplicationContext());
        clearOldFiles(cacheDir, Preferences.get().getKeepCacheTime());
    }

    /**
     * {@link org.fdroid.fdroid.installer.Installer} instances copy the APK into
     * a safe place before installing.  It doesn't clean up them reliably yet.
     */
    private void deleteOldInstallerFiles() {
        File filesDir = getApplicationContext().getFilesDir();
        if (filesDir == null) {
            Utils.debugLog(TAG, "The files directory doesn't exist.");
            return;
        }

        final File[] files = filesDir.listFiles();
        if (files == null) {
            Utils.debugLog(TAG, "The files directory doesn't have any files.");
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
        File cacheDir = getApplicationContext().getCacheDir();
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
    private void deleteOldIcons() {
        clearOldFiles(Utils.getImageCacheDir(getApplicationContext()), TimeUnit.DAYS.toMillis(365));
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
        } else if (Build.VERSION.SDK_INT <= 21) {
            if (FileUtils.isFileOlder(f, olderThan)) {
                deleteFileAndLog(f);
            }
        } else {
            Impl21.deleteIfOld(f, olderThan);
        }
    }

    private static void deleteFileAndLog(final File file) {
        file.delete();
        Utils.debugLog(TAG, "Deleted file: " + file);
    }

    @RequiresApi(api = 21)
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
        public static void deleteIfOld(File file, long olderThan) {
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
