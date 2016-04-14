package org.fdroid.fdroid;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Process;

import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Handles cleaning up caches files that are not going to be used, and do not
 * block the operation of the app itself.  For things that must happen before
 * F-Droid starts normal operation, that should go into
 * {@link FDroidApp#onCreate()}
 */
public class CleanCacheService extends IntentService {
    public static final String TAG = "CleanCacheService";

    public static void start(Context context) {
        Intent intent = new Intent(context, CleanCacheService.class);
        context.startService(intent);
    }

    public CleanCacheService() {
        super("CleanCacheService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

        int cachetime;
        if (Preferences.get().shouldCacheApks()) {
            cachetime = Integer.MAX_VALUE;
        } else {
            cachetime = 3600;  // keep for 1 hour to allow resumable downloads
        }
        Utils.clearOldFiles(Utils.getApkCacheDir(this), cachetime);
        deleteStrayIndexFiles();
    }

    /**
     * Delete index files which were downloaded, but not removed (e.g. due to F-Droid being
     * force closed during processing of the file, before getting a chance to delete). This
     * may include both "index-*-downloaded" and "index-*-extracted.xml" files.
     * <p/>
     * Note that if the SD card is not ready, then the cache directory will probably not be
     * available. In this situation no files will be deleted (and thus they may still exist
     * after the SD card becomes available).
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
        }
    }
}
