package org.fdroid.fdroid.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.fdroid.fdroid.AppDetails;

/**
 * Receive notifications from the Android DownloadManager
 */
public class DownloadManagerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // pass the download manager broadcast onto the AppDetails screen and let it handle it
        Intent appDetails = new Intent(context, AppDetails.class);
        appDetails.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appDetails.setAction(intent.getAction());
        appDetails.putExtras(intent.getExtras());
        context.startActivity(appDetails);
    }
}
