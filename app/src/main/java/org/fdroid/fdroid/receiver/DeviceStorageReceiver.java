package org.fdroid.fdroid.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.fdroid.fdroid.DeleteCacheService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.work.WorkUtils;

public class DeviceStorageReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(action)) {
            int percentageFree = Utils.getPercent(Utils.getImageCacheDirAvailableMemory(context),
                    Utils.getImageCacheDirTotalMemory(context));
            if (percentageFree > 2) {
                WorkUtils.scheduleCleanCache(context);
            } else {
                DeleteCacheService.deleteAll(context);
            }
        }
    }
}
