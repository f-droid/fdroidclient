package org.fdroid.fdroid;

import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * An {@link JobIntentService} subclass for deleting the full cache for this app.
 */
public class DeleteCacheService extends JobIntentService {
    public static final String TAG = "DeleteCacheService";

    public static void deleteAll(Context context) {
        Intent intent = new Intent(context, DeleteCacheService.class);
        enqueueWork(context, DeleteCacheService.class, 0x523432, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
        Log.w(TAG, "Deleting all cached contents!");
        Glide.get(this).clearDiskCache();
        try {
            File cacheDir = getCacheDir();
            FileUtils.deleteDirectory(cacheDir);
            for (File dir : ContextCompat.getExternalCacheDirs(this)) {
                FileUtils.deleteDirectory(dir);
            }
        } catch (Throwable e) { // NOPMD
            // ignored
        }
    }
}
