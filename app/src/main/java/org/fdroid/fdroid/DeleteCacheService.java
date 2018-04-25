package org.fdroid.fdroid;

import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.content.ContextCompat;
import android.util.Log;
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
        try {
            FileUtils.deleteDirectory(getCacheDir());
            for (File dir : ContextCompat.getExternalCacheDirs(this)) {
                FileUtils.deleteDirectory(dir);
            }
        } catch (Exception e) {
            // ignored
        }
    }
}
