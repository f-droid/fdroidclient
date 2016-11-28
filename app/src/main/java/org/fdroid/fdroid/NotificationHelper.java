package org.fdroid.fdroid;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.util.LongSparseArray;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NotificationHelper {

    private static final String BROADCAST_NOTIFICATIONS_UPDATES_CLEARED = "org.fdroid.fdroid.installer.notifications.updates.cleared";
    private static final String BROADCAST_NOTIFICATIONS_INSTALLED_CLEARED = "org.fdroid.fdroid.installer.notifications.installed.cleared";
    private static final String BROADCAST_NOTIFICATIONS_NOTIFICATION_DELETED = "org.fdroid.fdroid.installer.notifications.deleted";

    private static final int NOTIFY_ID_UPDATES = 4711;
    private static final int NOTIFY_ID_INSTALLED = 4712;

    private static final int MAX_UPDATES_TO_SHOW = 5;
    private static final int MAX_INSTALLED_TO_SHOW = 10;

    private static final String EXTRA_NOTIFICATION_TAG = "tag";
    private static final String GROUP_UPDATES = "updates";
    private static final String GROUP_INSTALLED = "installed";

    public enum Status {
        UpdateAvailable,
        Downloading,
        ReadyToInstall,
        Installing,
        Installed,
        Error
    }

    private static NotificationHelper instance;

    public static void init(Context context) {
        instance = new NotificationHelper(context);
    }

    private static NotificationHelper getInstance() {
        return instance;
    }

    private class AppEntry {
        App app;
        Apk apk;
        Status status;
        PendingIntent intent;
        int progressCurrent;
        int progressMax;

        AppEntry(App app, Apk apk, Status status, PendingIntent intent) {
            this.app = app;
            this.apk = apk;
            this.status = status;
            this.intent = intent;
        }

        String getTag() {
            return apk.getUrl();
        }

        int getId() {
            return getTag().hashCode();
        }
    }

    private final Context context;
    private final NotificationManagerCompat notificationManager;
    private HashMap<String, AppEntry> appMapping;
    private boolean isBatchUpdating;
    private ArrayList<AppEntry> updates;
    private ArrayList<AppEntry> installed;

    private NotificationHelper(Context context) {
        this.context = context;
        notificationManager = NotificationManagerCompat.from(context);

        // We need to listen to when notifications are cleared, so that we "forget" all that we currently know about updates
        // and installs.
        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_NOTIFICATIONS_UPDATES_CLEARED);
        filter.addAction(BROADCAST_NOTIFICATIONS_INSTALLED_CLEARED);
        filter.addAction(BROADCAST_NOTIFICATIONS_NOTIFICATION_DELETED);
        BroadcastReceiver mReceiverNotificationsCleared = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case BROADCAST_NOTIFICATIONS_INSTALLED_CLEARED:
                        clearAllInstalledInternal();
                        break;
                    case BROADCAST_NOTIFICATIONS_UPDATES_CLEARED:
                        clearAllUpdatesInternal();
                        break;
                    case BROADCAST_NOTIFICATIONS_NOTIFICATION_DELETED:
                        String id = intent.getStringExtra(EXTRA_NOTIFICATION_TAG);
                        // TODO
                        break;
                }
            }
        };
        context.registerReceiver(mReceiverNotificationsCleared, filter);
        appMapping = new HashMap<>();
        updates = new ArrayList<>();
        installed = new ArrayList<>();
    }

    private void setApkInternal(Apk apk, Status status, PendingIntent intent) {
        if (apk == null) {
            return;
        }

        AppEntry entry = appMapping.get(apk.getUrl());
        if (status == null) {
            // Remove
            if (entry != null) {
                appMapping.remove(apk.getUrl());
                notificationManager.cancel(entry.getTag(), entry.getId());
            }
        } else if (entry != null) {
            // Update
            boolean isStatusUpdate = (entry.status != status);
            entry.status = status;
            entry.intent = intent;
            createNotificationForAppEntry(entry);
            if (isStatusUpdate) {
                updateSummaryNotifications();
            }
        } else {
            // Add
            ContentResolver resolver = context.getContentResolver();
            App app = AppProvider.Helper.findSpecificApp(resolver, apk.packageName, apk.repo);
            entry = new AppEntry(app, apk, status, intent);
            appMapping.put(apk.getUrl(), entry);
            createNotificationForAppEntry(entry);
            updateSummaryNotifications();
        }
    }

    private void setApkProgressInternal(Apk apk, int max, int current) {
        if (appMapping.get(apk.getUrl()) != null) {
            AppEntry entry = appMapping.get(apk.getUrl());
            entry.progressMax = max;
            entry.progressCurrent = current;
            createNotificationForAppEntry(entry);
        }
    }

    private void clearAllUpdatesInternal() {
        for(Iterator<Map.Entry<String, AppEntry>> it = appMapping.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, AppEntry> entry = it.next();
            if(entry.getValue().status != Status.Installed) {
                it.remove();
            }
        }
    }

    private void clearAllInstalledInternal() {
        for(Iterator<Map.Entry<String, AppEntry>> it = appMapping.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, AppEntry> entry = it.next();
            if(entry.getValue().status == Status.Installed) {
                it.remove();
            }
        }
    }

    private void updateSummaryNotifications() {
        if (!isBatchUpdating) {
            // Get the list of updates and installed available
            updates.clear();
            installed.clear();
            for (Iterator<Map.Entry<String, AppEntry>> it = appMapping.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, AppEntry> entry = it.next();
                if (entry.getValue().status != Status.Installed) {
                    updates.add(entry.getValue());
                } else {
                    installed.add(entry.getValue());
                }
            }

            NotificationCompat.Builder builder;
            if (updates.size() == 0) {
                // No updates, remove summary
                notificationManager.cancel(GROUP_UPDATES, NOTIFY_ID_UPDATES);
            } else {
                builder = createUpdateSummaryNotification(updates);
                notificationManager.notify(GROUP_UPDATES, NOTIFY_ID_UPDATES, builder.build());
            }
            if (installed.size() == 0) {
                // No installed, remove summary
                notificationManager.cancel(GROUP_INSTALLED, NOTIFY_ID_INSTALLED);
            } else {
                builder = createInstalledSummaryNotification(installed);
                notificationManager.notify(GROUP_INSTALLED, NOTIFY_ID_INSTALLED, builder.build());
            }
        }
    }

    private void createNotificationForAppEntry(AppEntry entry) {
        NotificationCompat.Builder builder;
        if (entry.status == Status.Installed) {
            builder = createInstalledNotification(entry);
        } else {
            builder = createUpdateNotification(entry);
        }
        notificationManager.notify(entry.getTag(), entry.getId(), builder.build());
    }

    /**
     * Add an Apk to the notifications manager.
     * @param apk The apk to add.
     * @param status The current status of the app
     * @param pendingIntent Action when notification is clicked. Can be null for default action(s)
     */
    public static void setApk(Apk apk, Status status, PendingIntent pendingIntent) {
        getInstance().setApkInternal(apk, status, pendingIntent);
    }

    public static void removeApk(Apk apk) {
        getInstance().setApkInternal(apk, null, null);
    }

    public static void setApkProgress(Apk apk, int max, int current) {
        getInstance().setApkProgressInternal(apk, max, current);
    }

    public static void startBatchUpdates() {
        getInstance().isBatchUpdating = true;
    }

    public static void endBatchUpdates() {
        getInstance().isBatchUpdating = false;
        getInstance().updateSummaryNotifications();
    }

    public static void clearAllUpdates() {
        getInstance().clearAllUpdatesInternal();
    }

    public static void clearAllInstalled() {
        getInstance().clearAllInstalledInternal();
    }

    private NotificationCompat.Action getAction(AppEntry entry) {
        if (entry.status == Status.UpdateAvailable) {
            // Make sure we have an intent to install the app. If not set, we create an intent
            // to open up the app details page for the app. From there, the user can hit "install"
            PendingIntent intent = entry.intent;
            if (intent == null) {
                intent = getAppDetailsIntent(0, entry.apk);
            }
            return new NotificationCompat.Action(R.drawable.ic_notify_update_24dp, "Update", intent);
        } else if (entry.status == Status.Downloading || entry.status == Status.Installing) {
            PendingIntent intent = entry.intent;
            if (intent != null) {
                return new NotificationCompat.Action(R.drawable.ic_notify_cancel_24dp, "Cancel", intent);
            }
        } else if (entry.status == Status.ReadyToInstall) {
            // Make sure we have an intent to install the app. If not set, we create an intent
            // to open up the app details page for the app. From there, the user can hit "install"
            PendingIntent intent = entry.intent;
            if (intent == null) {
                intent = getAppDetailsIntent(0, entry.apk);
            }
            return new NotificationCompat.Action(R.drawable.ic_notify_install_24dp, "Install", intent);
        }
        return null;
    }

    private String getSingleItemTitleString(App app, Status status) {
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
        }
        return "";
    }

    private String getSingleItemContentString(App app, Status status) {
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
        }
        return "";
    }

    private String getMultiItemContentString(App app, Status status) {
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
        }
        return "";
    }

    /**
     * Get a {@link PendingIntent} for a {@link Notification} to send when it
     * is clicked.  {@link AppDetails} handles {@code Intent}s that are missing
     * or bad {@link AppDetails#EXTRA_APPID}, so it does not need to be checked
     * here.
     */
    private PendingIntent getAppDetailsIntent(int requestCode, Apk apk) {
        Intent notifyIntent = new Intent(context, AppDetails.class)
                .putExtra(AppDetails.EXTRA_APPID, apk.packageName);
        return TaskStackBuilder.create(context)
                .addParentStack(AppDetails.class)
                .addNextIntent(notifyIntent)
                .getPendingIntent(requestCode, 0);
    }

    private NotificationCompat.Builder createUpdateNotification(AppEntry entry) {
        App app = entry.app;
        Status status = entry.status;

        // TODO - async image loading
        int largeIconSize = context.getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);
        Bitmap iconLarge = ImageLoader.getInstance().loadImageSync(app.iconUrl, new ImageSize(largeIconSize, largeIconSize));

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setAutoCancel(true)
                        .setLargeIcon(iconLarge)
                        .setSmallIcon(R.drawable.ic_stat_notify_updates)
                        .setContentTitle(getSingleItemTitleString(app, status))
                        .setContentText(getSingleItemContentString(app, status))
                        .setGroup(GROUP_UPDATES);

        // Handle actions
        //
        NotificationCompat.Action action = getAction(entry);
        if (action != null) {
            builder.addAction(action);
            // TODO - also click on whole item?
            builder.setContentIntent(action.getActionIntent());
        } else if (entry.intent != null) {
            builder.setContentIntent(entry.intent);
        }

        // Handle progress bar (for some states)
        //
        if (status == Status.Downloading) {
            if (entry.progressMax == 0)
                builder.setProgress(100, 0, true);
            else
                builder.setProgress(entry.progressMax, entry.progressCurrent, false);
        } else if (status == Status.Installing) {
            builder.setProgress(100, 0, true); // indeterminate bar
        }

        Intent intentDeleted = new Intent(BROADCAST_NOTIFICATIONS_NOTIFICATION_DELETED);
        intentDeleted.putExtra(EXTRA_NOTIFICATION_TAG, entry.getId());
        PendingIntent piDeleted = PendingIntent.getBroadcast(context, 0, intentDeleted, 0);
        builder.setDeleteIntent(piDeleted);
        return builder;
    }

    private NotificationCompat.Builder createUpdateSummaryNotification(ArrayList<AppEntry> updates) {
        String title = String.format("%d Updates", updates.size());
        StringBuilder text = new StringBuilder();

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(title);

        for (int i = 0; i < MAX_UPDATES_TO_SHOW && i < updates.size(); i++) {
            AppEntry entry = updates.get(i);
            App app = entry.app;
            Status status = entry.status;

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

        //if (updates.size() > MAX_UPDATES_TO_SHOW) {
        //    int diff = updates.size() - MAX_UPDATES_TO_SHOW;
        //    inboxStyle.setSummaryText(context.getString(R.string.update_notification_more, diff));
        //}

        inboxStyle.setSummaryText(title);


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
                        .setStyle(inboxStyle)
                        .setGroup(GROUP_UPDATES)
                        .setGroupSummary(true);
        Intent intentDeleted = new Intent(BROADCAST_NOTIFICATIONS_UPDATES_CLEARED);
        PendingIntent piDeleted = PendingIntent.getBroadcast(context, 0, intentDeleted, 0);
        builder.setDeleteIntent(piDeleted);
        return builder;
    }

    private NotificationCompat.Builder createInstalledNotification(AppEntry entry) {
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

        Intent intentDeleted = new Intent(BROADCAST_NOTIFICATIONS_NOTIFICATION_DELETED);
        intentDeleted.putExtra(EXTRA_NOTIFICATION_TAG, entry.getId());
        PendingIntent piDeleted = PendingIntent.getBroadcast(context, 0, intentDeleted, 0);
        builder.setDeleteIntent(piDeleted);
        return builder;
    }

    private NotificationCompat.Builder createInstalledSummaryNotification(ArrayList<AppEntry> installed) {
        String title = String.format("%d Apps Installed", installed.size());
        StringBuilder text = new StringBuilder();

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(title);

        for (int i = 0; i < MAX_INSTALLED_TO_SHOW && i < installed.size(); i++) {
            AppEntry entry = installed.get(i);
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
                        .setContentIntent(piAction)
                        .setGroup(GROUP_INSTALLED)
                        .setGroupSummary(true);
        Intent intentDeleted = new Intent(BROADCAST_NOTIFICATIONS_INSTALLED_CLEARED);
        PendingIntent piDeleted = PendingIntent.getBroadcast(context, 0, intentDeleted, 0);
        builder.setDeleteIntent(piDeleted);
        return builder;
    }
}
