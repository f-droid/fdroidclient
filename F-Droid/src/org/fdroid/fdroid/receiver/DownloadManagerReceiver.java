package org.fdroid.fdroid.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.fdroid.fdroid.AppDetails;
import org.fdroid.fdroid.net.AsyncDownloader;

/**
 * Receive notifications from the Android DownloadManager
 */
public class DownloadManagerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // work out the app Id to send to the AppDetails Screen
        long downloadId = AsyncDownloader.getDownloadId(intent);
        String appId = AsyncDownloader.getAppId(context, downloadId);

        // pass the download manager broadcast onto the AppDetails screen and let it handle it
        Intent appDetails = new Intent(context, AppDetails.class);
        appDetails.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        appDetails.setAction(intent.getAction());
        appDetails.putExtras(intent.getExtras());
        appDetails.putExtra(AppDetails.EXTRA_APPID, appId);
        context.startActivity(appDetails);
    }
}
