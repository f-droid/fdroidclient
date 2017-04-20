package org.fdroid.fdroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * For security purposes we need to ensure that all Intent objects we give to a PendingIntent are
 * explicitly set to be delivered to an F-Droid class.
 * This class takes the global intent received from outside our process (i.e. from the
 * notification manager) and passes it onto the {@link AppUpdateStatusManager}.
 */
public class NotificationBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AppUpdateStatusManager manager = AppUpdateStatusManager.getInstance(context);
        String notificationKey = intent.getStringExtra(NotificationHelper.EXTRA_NOTIFICATION_KEY);
        switch (intent.getAction()) {
            case NotificationHelper.BROADCAST_NOTIFICATIONS_ALL_UPDATES_CLEARED:
                manager.clearAllUpdates();
                break;
            case NotificationHelper.BROADCAST_NOTIFICATIONS_ALL_INSTALLED_CLEARED:
                manager.clearAllInstalled();
                break;
            case NotificationHelper.BROADCAST_NOTIFICATIONS_UPDATE_CLEARED:
                // If clearing apps in state "InstallError" (like when auto-cancelling) we
                // remove them from the status manager entirely.
                AppUpdateStatusManager.AppUpdateStatus appUpdateStatus = manager.get(notificationKey);
                if (appUpdateStatus != null && appUpdateStatus.status == AppUpdateStatusManager.Status.InstallError) {
                    manager.removeApk(notificationKey);
                }
                break;
            case NotificationHelper.BROADCAST_NOTIFICATIONS_INSTALLED_CLEARED:
                manager.removeApk(notificationKey);
                break;
        }
    }
}
