package org.fdroid.fdroid;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.View;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.views.main.MainActivity;

import java.util.ArrayList;

@SuppressWarnings("LineLength")
class NotificationHelper {

    static final String BROADCAST_NOTIFICATIONS_ALL_UPDATES_CLEARED = "org.fdroid.fdroid.installer.notifications.allupdates.cleared";
    static final String BROADCAST_NOTIFICATIONS_ALL_INSTALLED_CLEARED = "org.fdroid.fdroid.installer.notifications.allinstalled.cleared";
    static final String BROADCAST_NOTIFICATIONS_UPDATE_CLEARED = "org.fdroid.fdroid.installer.notifications.update.cleared";
    static final String BROADCAST_NOTIFICATIONS_INSTALLED_CLEARED = "org.fdroid.fdroid.installer.notifications.installed.cleared";

    private static final int NOTIFY_ID_UPDATES = 1;
    private static final int NOTIFY_ID_INSTALLED = 2;

    private static final int MAX_UPDATES_TO_SHOW = 5;
    private static final int MAX_INSTALLED_TO_SHOW = 10;

    /**
     * Unique ID used to represent this specific package's install process,
     * including {@link Notification}s, also known as {@code urlString}.
     *
     * @see org.fdroid.fdroid.installer.InstallManagerService
     */
    static final String EXTRA_NOTIFICATION_KEY = "key";
    private static final String GROUP_UPDATES = "updates";
    private static final String GROUP_INSTALLED = "installed";

    private final Context context;
    private final NotificationManagerCompat notificationManager;
    private final AppUpdateStatusManager appUpdateStatusManager;
    private final ArrayList<AppUpdateStatusManager.AppUpdateStatus> updates = new ArrayList<>();
    private final ArrayList<AppUpdateStatusManager.AppUpdateStatus> installed = new ArrayList<>();

    NotificationHelper(Context context) {
        this.context = context;
        appUpdateStatusManager = AppUpdateStatusManager.getInstance(context);
        notificationManager = NotificationManagerCompat.from(context);

        IntentFilter filter = new IntentFilter();
        filter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_LIST_CHANGED);
        filter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_ADDED);
        filter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_CHANGED);
        filter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_REMOVED);
        BroadcastReceiver receiverAppStatusChanges = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    return;
                }

                AppUpdateStatusManager.AppUpdateStatus entry;
                String url;
                switch (intent.getAction()) {
                    case AppUpdateStatusManager.BROADCAST_APPSTATUS_LIST_CHANGED:
                        notificationManager.cancelAll();
                        updateStatusLists();
                        createSummaryNotifications();
                        for (AppUpdateStatusManager.AppUpdateStatus appUpdateStatus : appUpdateStatusManager.getAll()) {
                            createNotification(appUpdateStatus);
                        }
                        break;
                    case AppUpdateStatusManager.BROADCAST_APPSTATUS_ADDED:
                        updateStatusLists();
                        createSummaryNotifications();
                        url = intent.getStringExtra(AppUpdateStatusManager.EXTRA_APK_URL);
                        entry = appUpdateStatusManager.get(url);
                        if (entry != null) {
                            createNotification(entry);
                        }
                        break;
                    case AppUpdateStatusManager.BROADCAST_APPSTATUS_CHANGED:
                        url = intent.getStringExtra(AppUpdateStatusManager.EXTRA_APK_URL);
                        entry = appUpdateStatusManager.get(url);
                        updateStatusLists();
                        if (entry != null) {
                            createNotification(entry);
                        }
                        if (intent.getBooleanExtra(AppUpdateStatusManager.EXTRA_IS_STATUS_UPDATE, false)) {
                            createSummaryNotifications();
                        }
                        break;
                    case AppUpdateStatusManager.BROADCAST_APPSTATUS_REMOVED:
                        url = intent.getStringExtra(AppUpdateStatusManager.EXTRA_APK_URL);
                        notificationManager.cancel(url, NOTIFY_ID_INSTALLED);
                        notificationManager.cancel(url, NOTIFY_ID_UPDATES);
                        updateStatusLists();
                        createSummaryNotifications();
                        break;
                }
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(receiverAppStatusChanges, filter);
    }

    private boolean useStackedNotifications() {
        return Build.VERSION.SDK_INT >= 24;
    }

    /**
     * Populate {@link NotificationHelper#updates} and {@link NotificationHelper#installed} with
     * the relevant status entries from the {@link AppUpdateStatusManager}.
     */
    private void updateStatusLists() {
        if (!notificationManager.areNotificationsEnabled()) {
            return;
        }

        updates.clear();
        installed.clear();

        for (AppUpdateStatusManager.AppUpdateStatus entry : appUpdateStatusManager.getAll()) {
            if (entry.status == AppUpdateStatusManager.Status.Installed) {
                installed.add(entry);
            } else if (!shouldIgnoreEntry(entry)) {
                updates.add(entry);
            }
        }
    }

    private boolean shouldIgnoreEntry(AppUpdateStatusManager.AppUpdateStatus entry) {
        // Ignore unknown status
        if (entry.status == AppUpdateStatusManager.Status.DownloadInterrupted) {
            return true;
        } else if ((entry.status == AppUpdateStatusManager.Status.Downloading ||
                entry.status == AppUpdateStatusManager.Status.ReadyToInstall ||
                entry.status == AppUpdateStatusManager.Status.InstallError) &&
                AppDetails2.isAppVisible(entry.app.packageName)) {
            // Ignore downloading, readyToInstall and installError if we are showing the details screen for this app
            return true;
        }
        return false;
    }

    private void createNotification(AppUpdateStatusManager.AppUpdateStatus entry) {
        if (shouldIgnoreEntry(entry)) {
            notificationManager.cancel(entry.getUniqueKey(), NOTIFY_ID_UPDATES);
            notificationManager.cancel(entry.getUniqueKey(), NOTIFY_ID_INSTALLED);
            return;
        }

        if (!notificationManager.areNotificationsEnabled() || Preferences.get().hideAllNotifications()) {
            return;
        }

        Notification notification;
        if (entry.status == AppUpdateStatusManager.Status.Installed) {
            if (useStackedNotifications()) {
                notification = createInstalledNotification(entry);
                notificationManager.cancel(entry.getUniqueKey(), NOTIFY_ID_UPDATES);
                notificationManager.notify(entry.getUniqueKey(), NOTIFY_ID_INSTALLED, notification);
            } else if (installed.size() == 1) {
                notification = createInstalledNotification(entry);
                notificationManager.cancel(entry.getUniqueKey(), NOTIFY_ID_UPDATES);
                notificationManager.cancel(entry.getUniqueKey(), NOTIFY_ID_INSTALLED);
                notificationManager.notify(GROUP_INSTALLED, NOTIFY_ID_INSTALLED, notification);
            }
        } else {
            if (useStackedNotifications()) {
                notification = createUpdateNotification(entry);
                notificationManager.cancel(entry.getUniqueKey(), NOTIFY_ID_INSTALLED);
                notificationManager.notify(entry.getUniqueKey(), NOTIFY_ID_UPDATES, notification);
            } else if (updates.size() == 1) {
                notification = createUpdateNotification(entry);
                notificationManager.cancel(entry.getUniqueKey(), NOTIFY_ID_UPDATES);
                notificationManager.cancel(entry.getUniqueKey(), NOTIFY_ID_INSTALLED);
                notificationManager.notify(GROUP_UPDATES, NOTIFY_ID_UPDATES, notification);
            }
        }
    }

    private void createSummaryNotifications() {
        if (!notificationManager.areNotificationsEnabled() || Preferences.get().hideAllNotifications()) {
            return;
        }

        Notification notification;
        if (updates.size() != 1 || useStackedNotifications()) {
            if (updates.size() == 0) {
                // No updates, remove summary
                notificationManager.cancel(GROUP_UPDATES, NOTIFY_ID_UPDATES);
            } else {
                notification = createUpdateSummaryNotification(updates);
                notificationManager.notify(GROUP_UPDATES, NOTIFY_ID_UPDATES, notification);
            }
        }
        if (installed.size() != 1 || useStackedNotifications()) {
            if (installed.size() == 0) {
                // No installed, remove summary
                notificationManager.cancel(GROUP_INSTALLED, NOTIFY_ID_INSTALLED);
            } else {
                notification = createInstalledSummaryNotification(installed);
                notificationManager.notify(GROUP_INSTALLED, NOTIFY_ID_INSTALLED, notification);
            }
        }
    }

    private NotificationCompat.Action getAction(AppUpdateStatusManager.AppUpdateStatus entry) {
        if (entry.intent != null) {
            switch (entry.status) {
                case UpdateAvailable:
                    return new NotificationCompat.Action(R.drawable.ic_file_download, context.getString(R.string.notification_action_update), entry.intent);

                case PendingInstall:
                case Downloading:
                case Installing:
                    return new NotificationCompat.Action(R.drawable.ic_cancel, context.getString(R.string.notification_action_cancel), entry.intent);

                case ReadyToInstall:
                    return new NotificationCompat.Action(R.drawable.ic_file_install, context.getString(R.string.notification_action_install), entry.intent);
            }
        }
        return null;
    }

    private String getSingleItemTitleString(App app, AppUpdateStatusManager.Status status) {
        switch (status) {
            case UpdateAvailable:
                return context.getString(R.string.notification_title_single_update_available);
            case PendingInstall:
            case Downloading:
                return app.name;
            case ReadyToInstall:
                return context.getString(app.isInstalled(context) ? R.string.notification_title_single_ready_to_install_update : R.string.notification_title_single_ready_to_install);
            case Installing:
                return app.name;
            case Installed:
                return app.name;
            case InstallError:
                return context.getString(R.string.notification_title_single_install_error);
        }
        return "";
    }

    private String getSingleItemContentString(App app, AppUpdateStatusManager.Status status) {
        switch (status) {
            case UpdateAvailable:
                return app.name;
            case PendingInstall:
            case Downloading:
                return context.getString(app.isInstalled(context) ? R.string.notification_content_single_downloading_update : R.string.notification_content_single_downloading, app.name);
            case ReadyToInstall:
                return app.name;
            case Installing:
                return context.getString(R.string.notification_content_single_installing, app.name);
            case Installed:
                return context.getString(R.string.notification_content_single_installed);
            case InstallError:
                return app.name;
        }
        return "";
    }

    private String getMultiItemContentString(App app, AppUpdateStatusManager.Status status) {
        switch (status) {
            case UpdateAvailable:
                return context.getString(R.string.notification_title_summary_update_available);
            case PendingInstall:
            case Downloading:
                return context.getString(app.isInstalled(context) ? R.string.notification_title_summary_downloading_update : R.string.notification_title_summary_downloading);
            case ReadyToInstall:
                return context.getString(app.isInstalled(context) ? R.string.notification_title_summary_ready_to_install_update : R.string.notification_title_summary_ready_to_install);
            case Installing:
                return context.getString(R.string.notification_title_summary_installing);
            case Installed:
                return context.getString(R.string.notification_title_summary_installed);
            case InstallError:
                return context.getString(R.string.notification_title_summary_install_error);
        }
        return "";
    }

    private Notification createUpdateNotification(AppUpdateStatusManager.AppUpdateStatus entry) {
        App app = entry.app;
        AppUpdateStatusManager.Status status = entry.status;

        Bitmap iconLarge = getLargeIconForEntry(entry);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setAutoCancel(true)
                        .setContentTitle(getSingleItemTitleString(app, status))
                        .setContentText(getSingleItemContentString(app, status))
                        .setSmallIcon(R.drawable.ic_notification)
                        .setColor(ContextCompat.getColor(context, R.color.fdroid_blue))
                        .setLargeIcon(iconLarge)
                        .setLocalOnly(true)
                        .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                        .setContentIntent(entry.intent);

        /* If using stacked notifications, use groups. Note that this would not work prior to Lollipop,
           because of http://stackoverflow.com/a/34953411, but currently not an issue since stacked
           notifications are used only on >= Nougat.
        */
        if (useStackedNotifications()) {
            builder.setGroup(GROUP_UPDATES);
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
            if (entry.progressMax == 0) {
                builder.setProgress(100, 0, true);
            } else {
                builder.setProgress(Utils.bytesToKb(entry.progressMax),
                        Utils.bytesToKb(entry.progressCurrent), false);
            }
        } else if (status == AppUpdateStatusManager.Status.Installing) {
            builder.setProgress(100, 0, true); // indeterminate bar
        }

        Intent intentDeleted = new Intent(BROADCAST_NOTIFICATIONS_UPDATE_CLEARED);
        intentDeleted.putExtra(EXTRA_NOTIFICATION_KEY, entry.getUniqueKey());
        intentDeleted.setClass(context, NotificationBroadcastReceiver.class);
        PendingIntent piDeleted = PendingIntent.getBroadcast(context, 0, intentDeleted, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setDeleteIntent(piDeleted);
        return builder.build();
    }

    private Notification createUpdateSummaryNotification(ArrayList<AppUpdateStatusManager.AppUpdateStatus> updates) {
        String title = context.getResources().getQuantityString(R.plurals.notification_summary_updates,
                updates.size(), updates.size());
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

            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(app.name);
        }

        if (updates.size() > MAX_UPDATES_TO_SHOW) {
            int diff = updates.size() - MAX_UPDATES_TO_SHOW;
            inboxStyle.setSummaryText(context.getResources().getQuantityString(R.plurals.notification_summary_more,
                    diff, diff));
        }

        // Intent to open main app list
        Intent intentObject = new Intent(context, MainActivity.class);
        intentObject.putExtra(MainActivity.EXTRA_VIEW_UPDATES, true);
        PendingIntent piAction = PendingIntent.getActivity(context, 0, intentObject, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setAutoCancel(!useStackedNotifications())
                        .setSmallIcon(R.drawable.ic_notification)
                        .setColor(ContextCompat.getColor(context, R.color.fdroid_blue))
                        .setContentTitle(title)
                        .setContentText(text)
                        .setContentIntent(piAction)
                        .setLocalOnly(true)
                        .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                        .setStyle(inboxStyle);

        if (useStackedNotifications()) {
            builder.setGroup(GROUP_UPDATES)
                    .setGroupSummary(true);
        }

        Intent intentDeleted = new Intent(BROADCAST_NOTIFICATIONS_ALL_UPDATES_CLEARED);
        intentDeleted.setClass(context, NotificationBroadcastReceiver.class);
        PendingIntent piDeleted = PendingIntent.getBroadcast(context, 0, intentDeleted, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setDeleteIntent(piDeleted);
        return builder.build();
    }

    private Notification createInstalledNotification(AppUpdateStatusManager.AppUpdateStatus entry) {
        App app = entry.app;

        Bitmap iconLarge = getLargeIconForEntry(entry);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setAutoCancel(true)
                        .setLargeIcon(iconLarge)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setColor(ContextCompat.getColor(context, R.color.fdroid_blue))
                        .setContentTitle(app.name)
                        .setContentText(context.getString(R.string.notification_content_single_installed))
                        .setLocalOnly(true)
                        .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                        .setContentIntent(entry.intent);

        if (useStackedNotifications()) {
            builder.setGroup(GROUP_INSTALLED);
        }

        Intent intentDeleted = new Intent(BROADCAST_NOTIFICATIONS_INSTALLED_CLEARED);
        intentDeleted.putExtra(EXTRA_NOTIFICATION_KEY, entry.getUniqueKey());
        intentDeleted.setClass(context, NotificationBroadcastReceiver.class);
        PendingIntent piDeleted = PendingIntent.getBroadcast(context, 0, intentDeleted, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setDeleteIntent(piDeleted);

        return builder.build();
    }

    private Notification createInstalledSummaryNotification(ArrayList<AppUpdateStatusManager.AppUpdateStatus> installed) {
        String title = context.getResources().getQuantityString(R.plurals.notification_summary_installed,
                installed.size(), installed.size());
        StringBuilder text = new StringBuilder();

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(title);

        for (int i = 0; i < MAX_INSTALLED_TO_SHOW && i < installed.size(); i++) {
            AppUpdateStatusManager.AppUpdateStatus entry = installed.get(i);
            App app = entry.app;
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(app.name);
        }
        bigTextStyle.bigText(text);
        if (installed.size() > MAX_INSTALLED_TO_SHOW) {
            int diff = installed.size() - MAX_INSTALLED_TO_SHOW;
            bigTextStyle.setSummaryText(context.getResources().getQuantityString(R.plurals.notification_summary_more,
                    diff, diff));
        }

        // Intent to open main app list
        Intent intentObject = new Intent(context, MainActivity.class);
        PendingIntent piAction = PendingIntent.getActivity(context, 0, intentObject, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setAutoCancel(!useStackedNotifications())
                        .setSmallIcon(R.drawable.ic_notification)
                        .setColor(ContextCompat.getColor(context, R.color.fdroid_blue))
                        .setContentTitle(title)
                        .setContentText(text)
                        .setContentIntent(piAction)
                        .setLocalOnly(true)
                        .setVisibility(NotificationCompat.VISIBILITY_SECRET);
        if (useStackedNotifications()) {
            builder.setGroup(GROUP_INSTALLED)
                    .setGroupSummary(true);
        }
        Intent intentDeleted = new Intent(BROADCAST_NOTIFICATIONS_ALL_INSTALLED_CLEARED);
        intentDeleted.setClass(context, NotificationBroadcastReceiver.class);
        PendingIntent piDeleted = PendingIntent.getBroadcast(context, 0, intentDeleted, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setDeleteIntent(piDeleted);
        return builder.build();
    }

    private Point getLargeIconSize() {
        int w = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
        int h = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
        return new Point(w, h);
    }

    private Bitmap getLargeIconForEntry(AppUpdateStatusManager.AppUpdateStatus entry) {
        final Point largeIconSize = getLargeIconSize();
        Bitmap iconLarge = null;
        if (TextUtils.isEmpty(entry.app.iconUrl)) {
            return null;
        } else if (entry.status == AppUpdateStatusManager.Status.Downloading
                || entry.status == AppUpdateStatusManager.Status.Installing) {
            Bitmap bitmap = Bitmap.createBitmap(largeIconSize.x, largeIconSize.y, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Drawable downloadIcon = ContextCompat.getDrawable(context, R.drawable.ic_notification_download);
            if (downloadIcon != null) {
                downloadIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                downloadIcon.draw(canvas);
            }
            return bitmap;
        } else if (DiskCacheUtils.findInCache(entry.app.iconUrl, ImageLoader.getInstance().getDiskCache()) != null) {
            iconLarge = ImageLoader.getInstance().loadImageSync(
                    entry.app.iconUrl, new ImageSize(largeIconSize.x, largeIconSize.y));
        } else {
            // Load it for later!
            ImageLoader.getInstance().loadImage(entry.app.iconUrl, new ImageSize(largeIconSize.x, largeIconSize.y), new ImageLoadingListener() {

                AppUpdateStatusManager.AppUpdateStatus entry;

                ImageLoadingListener init(AppUpdateStatusManager.AppUpdateStatus entry) {
                    this.entry = entry;
                    return this;
                }

                @Override
                public void onLoadingStarted(String imageUri, View view) {

                }

                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {

                }

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    // Need to check that the notification is still valid, and also that the image
                    // is indeed cached now, so we won't get stuck in an endless loop.
                    AppUpdateStatusManager.AppUpdateStatus oldEntry = appUpdateStatusManager.get(entry.getUniqueKey());
                    if (oldEntry != null
                            && oldEntry.app != null
                            && oldEntry.app.iconUrl != null
                            && DiskCacheUtils.findInCache(oldEntry.app.iconUrl, ImageLoader.getInstance().getDiskCache()) != null) {
                        createNotification(oldEntry); // Update with new image!
                    }
                }

                @Override
                public void onLoadingCancelled(String imageUri, View view) {

                }
            }.init(entry));
        }
        return iconLarge;
    }
}
