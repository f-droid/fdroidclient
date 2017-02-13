package org.fdroid.fdroid;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;

import org.fdroid.fdroid.data.App;

import java.util.ArrayList;

class NotificationHelper {

    private static final String BROADCAST_NOTIFICATIONS_ALL_UPDATES_CLEARED = "org.fdroid.fdroid.installer.notifications.allupdates.cleared";
    private static final String BROADCAST_NOTIFICATIONS_ALL_INSTALLED_CLEARED = "org.fdroid.fdroid.installer.notifications.allinstalled.cleared";
    private static final String BROADCAST_NOTIFICATIONS_UPDATE_CLEARED = "org.fdroid.fdroid.installer.notifications.update.cleared";
    private static final String BROADCAST_NOTIFICATIONS_INSTALLED_CLEARED = "org.fdroid.fdroid.installer.notifications.installed.cleared";

    private static final int NOTIFY_ID_UPDATES = 1;
    private static final int NOTIFY_ID_INSTALLED = 2;

    private static final int MAX_UPDATES_TO_SHOW = 5;
    private static final int MAX_INSTALLED_TO_SHOW = 10;

    private static final String EXTRA_NOTIFICATION_KEY = "key";
    private static final String GROUP_UPDATES = "updates";
    private static final String GROUP_INSTALLED = "installed";

    private static final String LOGTAG = "NotificationHelper";

    private static NotificationHelper instance;

    public static NotificationHelper create(Context context) {
        if (instance == null) {
            instance = new NotificationHelper(context.getApplicationContext());
        }
        return instance;
    }

    private final Context context;
    private final NotificationManagerCompat notificationManager;
    private final AppUpdateStatusManager appUpdateStatusMananger;

    private NotificationHelper(Context context) {
        this.context = context;
        appUpdateStatusMananger = AppUpdateStatusManager.getInstance(context);
        notificationManager = NotificationManagerCompat.from(context);

        // We need to listen to when notifications are cleared, so that we "forget" all that we currently know about updates
        // and installs.
        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_NOTIFICATIONS_ALL_UPDATES_CLEARED);
        filter.addAction(BROADCAST_NOTIFICATIONS_ALL_INSTALLED_CLEARED);
        filter.addAction(BROADCAST_NOTIFICATIONS_UPDATE_CLEARED);
        filter.addAction(BROADCAST_NOTIFICATIONS_INSTALLED_CLEARED);
        context.registerReceiver(receiverNotificationsCleared, filter);
        filter = new IntentFilter();
        filter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_LIST_CHANGED);
        filter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_ADDED);
        filter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_CHANGED);
        filter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_REMOVED);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiverAppStatusChanges, filter);
    }

    private boolean useStackedNotifications() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    private void updateSummaryNotifications() {
        if (!notificationManager.areNotificationsEnabled()) {
            return;
        }

        // Get the list of updates and installed available
        ArrayList<AppUpdateStatusManager.AppUpdateStatus> updates = new ArrayList<>();
        ArrayList<AppUpdateStatusManager.AppUpdateStatus> installed = new ArrayList<>();
        for (AppUpdateStatusManager.AppUpdateStatus entry : appUpdateStatusMananger.getAll()) {
            if (entry.status == AppUpdateStatusManager.Status.Unknown) {
                continue;
            } else if (entry.status != AppUpdateStatusManager.Status.Installed) {
                updates.add(entry);
            } else {
                installed.add(entry);
            }
        }

        NotificationCompat.Builder builder;
        if (updates.size() == 0) {
            // No updates, remove summary
            notificationManager.cancel(GROUP_UPDATES, NOTIFY_ID_UPDATES);
        } else if (updates.size() == 1 && !useStackedNotifications()) {
            // If we use stacked notifications we have already created one.
            doCreateNotification(updates.get(0));
        } else {
            builder = createUpdateSummaryNotification(updates);
            notificationManager.notify(GROUP_UPDATES, NOTIFY_ID_UPDATES, builder.build());
        }
        if (installed.size() == 0) {
            // No installed, remove summary
            notificationManager.cancel(GROUP_INSTALLED, NOTIFY_ID_INSTALLED);
        } else if (installed.size() == 1 && !useStackedNotifications()) {
            // If we use stacked notifications we have already created one.
            doCreateNotification(installed.get(0));
        } else {
            builder = createInstalledSummaryNotification(installed);
            notificationManager.notify(GROUP_INSTALLED, NOTIFY_ID_INSTALLED, builder.build());
        }
    }

    private void createNotification(AppUpdateStatusManager.AppUpdateStatus entry) {
        if (useStackedNotifications() && notificationManager.areNotificationsEnabled() && entry.status != AppUpdateStatusManager.Status.Unknown) {
            doCreateNotification(entry);
        }
    }

    private void doCreateNotification(AppUpdateStatusManager.AppUpdateStatus entry) {
        NotificationCompat.Builder builder;
        int id;
        if (entry.status == AppUpdateStatusManager.Status.Installed) {
            builder = createInstalledNotification(entry);
            id = NOTIFY_ID_INSTALLED;
            notificationManager.cancel(entry.getUniqueKey(), NOTIFY_ID_UPDATES);
        } else {
            builder = createUpdateNotification(entry);
            id = NOTIFY_ID_UPDATES;
            notificationManager.cancel(entry.getUniqueKey(), NOTIFY_ID_INSTALLED);
        }
        notificationManager.notify(entry.getUniqueKey(), id, builder.build());
    }

    private NotificationCompat.Action getAction(AppUpdateStatusManager.AppUpdateStatus entry) {
        if (entry.intent != null) {
            if (entry.status == AppUpdateStatusManager.Status.UpdateAvailable) {
                return new NotificationCompat.Action(R.drawable.ic_notify_update_24dp, "Update", entry.intent);
            } else if (entry.status == AppUpdateStatusManager.Status.Downloading || entry.status == AppUpdateStatusManager.Status.Installing) {
                return new NotificationCompat.Action(R.drawable.ic_notify_cancel_24dp, "Cancel", entry.intent);
            } else if (entry.status == AppUpdateStatusManager.Status.ReadyToInstall) {
                return new NotificationCompat.Action(R.drawable.ic_notify_install_24dp, "Install", entry.intent);
            }
        }
        return null;
    }

    private String getSingleItemTitleString(App app, AppUpdateStatusManager.Status status) {
        switch (status) {
            case UpdateAvailable:
                return "Update Available";
            case Downloading:
                return app.name;
            case ReadyToInstall:
                return "Update ready to install"; // TODO - "Update"? Should just be "ready to install"?
            case Installing:
                return app.name;
            case Installed:
                return app.name;
            case InstallError:
                return "Install Failed";
        }
        return "";
    }

    private String getSingleItemContentString(App app, AppUpdateStatusManager.Status status) {
        switch (status) {
            case UpdateAvailable:
                return app.name;
            case Downloading:
                return String.format("Downloading update for \"%s\"...", app.name);
            case ReadyToInstall:
                return app.name;
            case Installing:
                return String.format("Installing \"%s\"...", app.name);
            case Installed:
                return "Successfully installed";
            case InstallError:
                return "Install Failed";
        }
        return "";
    }

    private String getMultiItemContentString(App app, AppUpdateStatusManager.Status status) {
        switch (status) {
            case UpdateAvailable:
                return "Update available";
            case Downloading:
                return "Downloading update...";
            case ReadyToInstall:
                return "Ready to install";
            case Installing:
                return "Installing";
            case Installed:
                return "Successfully installed";
            case InstallError:
                return "Install Failed";
        }
        return "";
    }

    private NotificationCompat.Builder createUpdateNotification(AppUpdateStatusManager.AppUpdateStatus entry) {
        App app = entry.app;
        AppUpdateStatusManager.Status status = entry.status;

        // TODO - async image loading
        int largeIconSize = context.getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);
        Bitmap iconLarge = ImageLoader.getInstance().loadImageSync(app.iconUrl, new ImageSize(largeIconSize, largeIconSize));

        // TODO - why?
        final int icon = Build.VERSION.SDK_INT >= 11 ? R.drawable.ic_stat_notify_updates : R.drawable.ic_launcher;

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setAutoCancel(false)
                        .setLargeIcon(iconLarge)
                        .setSmallIcon(icon)
                        .setContentTitle(getSingleItemTitleString(app, status))
                        .setContentText(getSingleItemContentString(app, status))
                        .setGroup(GROUP_UPDATES);

        // Handle intents
        //
        if (entry.intent != null) {
            builder.setContentIntent(entry.intent);
        }

        // Handle actions
        //
        NotificationCompat.Action action = getAction(entry);
        if (action != null) {
            builder.addAction(action);
        }

        // Handle progress bar (for some states)
        //
        if (status == AppUpdateStatusManager.Status.Downloading) {
            if (entry.progressMax == 0)
                builder.setProgress(100, 0, true);
            else
                builder.setProgress(entry.progressMax, entry.progressCurrent, false);
        } else if (status == AppUpdateStatusManager.Status.Installing) {
            builder.setProgress(100, 0, true); // indeterminate bar
        }

        Intent intentDeleted = new Intent(BROADCAST_NOTIFICATIONS_UPDATE_CLEARED);
        intentDeleted.putExtra(EXTRA_NOTIFICATION_KEY, entry.getUniqueKey());
        PendingIntent piDeleted = PendingIntent.getBroadcast(context, 0, intentDeleted, 0);
        builder.setDeleteIntent(piDeleted);
        return builder;
    }

    private NotificationCompat.Builder createUpdateSummaryNotification(ArrayList<AppUpdateStatusManager.AppUpdateStatus> updates) {
        String title = String.format("%d Updates", updates.size());
        StringBuilder text = new StringBuilder();

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(title);

        for (int i = 0; i < MAX_UPDATES_TO_SHOW && i < updates.size(); i++) {
            AppUpdateStatusManager.AppUpdateStatus entry = updates.get(i);
            App app = entry.app;
            AppUpdateStatusManager.Status status = entry.status;

            String content = getMultiItemContentString(app, status);
            SpannableStringBuilder sb = new SpannableStringBuilder(app.name);
            sb.setSpan(new StyleSpan(Typeface.BOLD), 0, sb.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            sb.append(" ");
            sb.append(content);
            inboxStyle.addLine(sb);

            if (text.length() > 0)
                text.append(", ");
            text.append(app.name);
        }
        if (updates.size() > MAX_UPDATES_TO_SHOW) {
            int diff = updates.size() - MAX_UPDATES_TO_SHOW;
            inboxStyle.setSummaryText(context.getString(R.string.update_notification_more, diff));
        }

        // Intent to open main app list
        Intent intentObject = new Intent(context, FDroid.class);
        PendingIntent piAction = PendingIntent.getActivity(context, 0, intentObject, 0);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setAutoCancel(true)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setContentIntent(piAction)
                        .setStyle(inboxStyle);
        if (BuildConfig.DEBUG) {
            builder.setPriority(NotificationCompat.PRIORITY_LOW); // To make not at top of list!
        }
        if (useStackedNotifications()) {
            builder.setGroup(GROUP_UPDATES)
                    .setGroupSummary(true);
        }
        Intent intentDeleted = new Intent(BROADCAST_NOTIFICATIONS_ALL_UPDATES_CLEARED);
        PendingIntent piDeleted = PendingIntent.getBroadcast(context, 0, intentDeleted, 0);
        builder.setDeleteIntent(piDeleted);
        return builder;
    }

    private NotificationCompat.Builder createInstalledNotification(AppUpdateStatusManager.AppUpdateStatus entry) {
        App app = entry.app;

        int largeIconSize = context.getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);
        Bitmap iconLarge = ImageLoader.getInstance().loadImageSync(app.iconUrl, new ImageSize(largeIconSize, largeIconSize));

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setAutoCancel(true)
                        .setLargeIcon(iconLarge)
                        .setSmallIcon(R.drawable.ic_stat_notify_updates)
                        .setContentTitle(app.name)
                        .setContentText("Successfully Installed")
                        .setGroup(GROUP_INSTALLED);

        PackageManager pm = context.getPackageManager();
        Intent intentObject = pm.getLaunchIntentForPackage(app.packageName);
        PendingIntent piAction = PendingIntent.getActivity(context, 0, intentObject, 0);
        builder.setContentIntent(piAction);

        Intent intentDeleted = new Intent(BROADCAST_NOTIFICATIONS_INSTALLED_CLEARED);
        intentDeleted.putExtra(EXTRA_NOTIFICATION_KEY, entry.getUniqueKey());
        PendingIntent piDeleted = PendingIntent.getBroadcast(context, 0, intentDeleted, 0);
        builder.setDeleteIntent(piDeleted);
        return builder;
    }

    private NotificationCompat.Builder createInstalledSummaryNotification(ArrayList<AppUpdateStatusManager.AppUpdateStatus> installed) {
        String title = String.format("%d Apps Installed", installed.size());
        StringBuilder text = new StringBuilder();

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(title);

        for (int i = 0; i < MAX_INSTALLED_TO_SHOW && i < installed.size(); i++) {
            AppUpdateStatusManager.AppUpdateStatus entry = installed.get(i);
            App app = entry.app;
            if (text.length() > 0)
                text.append(", ");
            text.append(app.name);
        }
        bigTextStyle.bigText(text);
        if (installed.size() > MAX_INSTALLED_TO_SHOW) {
            int diff = installed.size() - MAX_INSTALLED_TO_SHOW;
            bigTextStyle.setSummaryText(context.getString(R.string.update_notification_more, diff));
        }

        // Intent to open main app list
        Intent intentObject = new Intent(context, FDroid.class);
        PendingIntent piAction = PendingIntent.getActivity(context, 0, intentObject, 0);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setAutoCancel(true)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setContentIntent(piAction);
        if (useStackedNotifications()) {
            builder.setGroup(GROUP_INSTALLED)
                    .setGroupSummary(true);
        }
        Intent intentDeleted = new Intent(BROADCAST_NOTIFICATIONS_ALL_INSTALLED_CLEARED);
        PendingIntent piDeleted = PendingIntent.getBroadcast(context, 0, intentDeleted, 0);
        builder.setDeleteIntent(piDeleted);
        return builder;
    }

    private BroadcastReceiver receiverNotificationsCleared = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BROADCAST_NOTIFICATIONS_ALL_UPDATES_CLEARED:
                    appUpdateStatusMananger.clearAllUpdates();
                    break;
                case BROADCAST_NOTIFICATIONS_ALL_INSTALLED_CLEARED:
                    appUpdateStatusMananger.clearAllInstalled();
                    break;
                case BROADCAST_NOTIFICATIONS_UPDATE_CLEARED:
                    break;
                case BROADCAST_NOTIFICATIONS_INSTALLED_CLEARED:
                    String key = intent.getStringExtra(EXTRA_NOTIFICATION_KEY);
                    appUpdateStatusMananger.removeApk(key);
                    break;
            }
        }
    };

    private BroadcastReceiver receiverAppStatusChanges = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case AppUpdateStatusManager.BROADCAST_APPSTATUS_LIST_CHANGED:
                    notificationManager.cancelAll();
                    for (AppUpdateStatusManager.AppUpdateStatus entry : appUpdateStatusMananger.getAll()) {
                        createNotification(entry);
                    }
                    updateSummaryNotifications();
                    break;
                case AppUpdateStatusManager.BROADCAST_APPSTATUS_ADDED: {
                    String url = intent.getStringExtra(AppUpdateStatusManager.EXTRA_APK_URL);
                    AppUpdateStatusManager.AppUpdateStatus entry = appUpdateStatusMananger.get(url);
                    if (entry != null) {
                        createNotification(entry);
                    }
                    updateSummaryNotifications();
                    break;
                }
                case AppUpdateStatusManager.BROADCAST_APPSTATUS_CHANGED: {
                    String url = intent.getStringExtra(AppUpdateStatusManager.EXTRA_APK_URL);
                    AppUpdateStatusManager.AppUpdateStatus entry = appUpdateStatusMananger.get(url);
                    if (entry != null) {
                        createNotification(entry);
                    }
                    if (intent.getBooleanExtra(AppUpdateStatusManager.EXTRA_IS_STATUS_UPDATE, false)) {
                        updateSummaryNotifications();
                    }
                    break;
                }
                case AppUpdateStatusManager.BROADCAST_APPSTATUS_REMOVED: {
                    String url = intent.getStringExtra(AppUpdateStatusManager.EXTRA_APK_URL);
                    notificationManager.cancel(url, NOTIFY_ID_INSTALLED);
                    notificationManager.cancel(url, NOTIFY_ID_UPDATES);
                    updateSummaryNotifications();
                    break;
                }
            }
        }
    };
}
