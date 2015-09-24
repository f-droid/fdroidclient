package org.fdroid.fdroid.receiver;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;

import org.fdroid.fdroid.AppDetails;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.net.AsyncDownloader;
import org.fdroid.fdroid.net.AsyncDownloaderFromAndroid;

/**
 * Receive notifications from the Android DownloadManager and pass them onto the
 * AppDetails activity
 */
@TargetApi(9)
public class DownloadManagerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // work out the app Id to send to the AppDetails Screen
        long downloadId = AsyncDownloaderFromAndroid.getDownloadId(intent);
        String appId = AsyncDownloaderFromAndroid.getDownloadId(context, downloadId);

        if (appId == null) {
            // bogus broadcast (e.g. download cancelled, but system sent a DOWNLOAD_COMPLETE)
            return;
        }

        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
            int status = AsyncDownloaderFromAndroid.validDownload(context, downloadId);
            if (status == 0) {
                // successful download
                showNotification(context, appId, intent, downloadId, R.string.tap_to_install);
            } else {
                // download failed!
                showNotification(context, appId, intent, downloadId, R.string.download_error);

                // clear the download to allow user to download again
                DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                dm.remove(downloadId);
            }
        } else if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(intent.getAction())) {
            // pass the notification click onto the AppDetails screen and let it handle it
            Intent appDetails = new Intent(context, AppDetails.class);
            appDetails.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            appDetails.setAction(intent.getAction());
            appDetails.putExtras(intent.getExtras());
            appDetails.putExtra(AppDetails.EXTRA_APPID, appId);
            context.startActivity(appDetails);
        }
    }

    private void showNotification(Context context, String appId, Intent intent, long downloadId,
                                  @StringRes int messageResId) {
        // show a notification the user can click to install the app
        Intent appDetails = new Intent(context, AppDetails.class);
        appDetails.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        appDetails.setAction(intent.getAction());
        appDetails.putExtras(intent.getExtras());
        appDetails.putExtra(AppDetails.EXTRA_APPID, appId);

        PendingIntent pi = PendingIntent.getActivity(
                context, 1, appDetails, PendingIntent.FLAG_ONE_SHOT);

        // launch LocalRepoActivity if the user selects this notification
        String downloadTitle = AsyncDownloaderFromAndroid.getDownloadTitle(context, downloadId);
        Notification notif = new NotificationCompat.Builder(context)
                .setContentTitle(downloadTitle)
                .setContentText(context.getString(messageResId))
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify((int)downloadId, notif);
    }
}
