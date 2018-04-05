package org.fdroid.fdroid;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * An {@link IntentService} subclass for deleting the full cache for this app.
 */
public class DeleteCacheService extends IntentService {
    public static final String TAG = "DeleteCacheService";

    public DeleteCacheService() {
        super("DeleteCacheService");
    }

    public static void deleteAll(Context context) {
        Intent intent = new Intent(context, DeleteCacheService.class);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
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
