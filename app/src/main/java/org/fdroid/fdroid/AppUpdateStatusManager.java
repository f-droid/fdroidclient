package org.fdroid.fdroid;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.installer.ErrorDialogActivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AppUpdateStatusManager {

    static final String BROADCAST_APPSTATUS_LIST_CHANGED = "org.fdroid.fdroid.installer.appstatus.listchange";
    static final String BROADCAST_APPSTATUS_ADDED = "org.fdroid.fdroid.installer.appstatus.appchange.add";
    static final String BROADCAST_APPSTATUS_CHANGED = "org.fdroid.fdroid.installer.appstatus.appchange.change";
    static final String BROADCAST_APPSTATUS_REMOVED = "org.fdroid.fdroid.installer.appstatus.appchange.remove";
    static final String EXTRA_APK_URL = "urlstring";
    static final String EXTRA_IS_STATUS_UPDATE = "isstatusupdate";

    private static final String LOGTAG = "AppUpdateStatusManager";

    public enum Status {
        Unknown,
        UpdateAvailable,
        Downloading,
        ReadyToInstall,
        Installing,
        Installed,
        InstallError
    }

    public static AppUpdateStatusManager getInstance(Context context) {
        return new AppUpdateStatusManager(context);
    }

    public class AppUpdateStatus {
        public final App app;
        public final Apk apk;
        public Status status;
        public PendingIntent intent;
        public int progressCurrent;
        public int progressMax;
        public String errorText;

        AppUpdateStatus(App app, Apk apk, Status status, PendingIntent intent) {
            this.app = app;
            this.apk = apk;
            this.status = status;
            this.intent = intent;
        }

        public String getUniqueKey() {
            return apk.getUrl();
        }
    }

    private final Context context;
    private final LocalBroadcastManager localBroadcastManager;
    private static final HashMap<String, AppUpdateStatus> appMapping = new HashMap<>();
    private boolean isBatchUpdating;

    private AppUpdateStatusManager(Context context) {
        this.context = context;
        localBroadcastManager = LocalBroadcastManager.getInstance(context.getApplicationContext());
    }

    @Nullable
    public AppUpdateStatus get(String key) {
        synchronized (appMapping) {
            return appMapping.get(key);
        }
    }

    public Collection<AppUpdateStatus> getAll() {
        synchronized (appMapping) {
            return appMapping.values();
        }
    }

    /**
     * Get all entries associated with a package name. There may be several.
     * @param packageName Package name of the app
     * @return A list of entries, or an empty list
     */
    public Collection<AppUpdateStatus> getByPackageName(String packageName) {
        ArrayList<AppUpdateStatus> returnValues = new ArrayList<>();
        synchronized (appMapping) {
            for (AppUpdateStatus entry : appMapping.values()) {
                if (entry.apk.packageName.equalsIgnoreCase(packageName)) {
                    returnValues.add(entry);
                }
            }
        }
        return returnValues;
    }

    private void setApkInternal(Apk apk, @NonNull Status status, PendingIntent intent) {
        if (apk == null) {
            return;
        }

        synchronized (appMapping) {
            AppUpdateStatus entry = appMapping.get(apk.getUrl());
            if (entry != null) {
                // Update
                Utils.debugLog(LOGTAG, "Update APK " + apk.apkName + " state to " + status.name());
                boolean isStatusUpdate = (entry.status != status);
                entry.status = status;
                entry.intent = intent;
                // If intent not set, see if we need to create a default intent
                if (entry.intent == null) {
                    entry.intent = getContentIntent(entry);
                }
                notifyChange(entry, isStatusUpdate);
            } else {
                // Add
                Utils.debugLog(LOGTAG, "Add APK " + apk.apkName + " with state " + status.name());
                entry = createAppEntry(apk, status, intent);
                // If intent not set, see if we need to create a default intent
                if (entry.intent == null) {
                    entry.intent = getContentIntent(entry);
                }
                appMapping.put(entry.getUniqueKey(), entry);
                notifyAdd(entry);
            }
        }
    }

    private void notifyChange() {
        if (!isBatchUpdating) {
            localBroadcastManager.sendBroadcast(new Intent(BROADCAST_APPSTATUS_LIST_CHANGED));
        }
    }

    private void notifyAdd(AppUpdateStatus entry) {
        if (!isBatchUpdating) {
            Intent broadcastIntent = new Intent(BROADCAST_APPSTATUS_ADDED);
            broadcastIntent.putExtra(EXTRA_APK_URL, entry.getUniqueKey());
            localBroadcastManager.sendBroadcast(broadcastIntent);
        }
    }

    private void notifyChange(AppUpdateStatus entry, boolean isStatusUpdate) {
        if (!isBatchUpdating) {
            Intent broadcastIntent = new Intent(BROADCAST_APPSTATUS_CHANGED);
            broadcastIntent.putExtra(EXTRA_APK_URL, entry.getUniqueKey());
            broadcastIntent.putExtra(EXTRA_IS_STATUS_UPDATE, isStatusUpdate);
            localBroadcastManager.sendBroadcast(broadcastIntent);
        }
    }

    private void notifyRemove(AppUpdateStatus entry) {
        if (!isBatchUpdating) {
            Intent broadcastIntent = new Intent(BROADCAST_APPSTATUS_REMOVED);
            broadcastIntent.putExtra(EXTRA_APK_URL, entry.getUniqueKey());
            localBroadcastManager.sendBroadcast(broadcastIntent);
        }
    }

    private AppUpdateStatus createAppEntry(Apk apk, Status status, PendingIntent intent) {
        synchronized (appMapping) {
            ContentResolver resolver = context.getContentResolver();
            App app = AppProvider.Helper.findSpecificApp(resolver, apk.packageName, apk.repo);
            AppUpdateStatus ret = new AppUpdateStatus(app, apk, status, intent);
            appMapping.put(apk.getUrl(), ret);
            return ret;
        }
    }


    public void addApks(List<Apk> apksToUpdate, Status status) {
        startBatchUpdates();
        for (Apk apk : apksToUpdate) {
            addApk(apk, status, null);
        }
        endBatchUpdates();
    }

    /**
     * Add an Apk to the AppUpdateStatusManager manager.
     * @param apk The apk to add.
     * @param status The current status of the app
     * @param pendingIntent Action when notification is clicked. Can be null for default action(s)
     */
    public void addApk(Apk apk, @NonNull Status status, PendingIntent pendingIntent) {
        setApkInternal(apk, status, pendingIntent);
    }

    public void updateApk(String key, @NonNull Status status, PendingIntent pendingIntent) {
        synchronized (appMapping) {
            AppUpdateStatus entry = appMapping.get(key);
            if (entry != null) {
                setApkInternal(entry.apk, status, pendingIntent);
            }
        }
    }

    @Nullable
    public Apk getApk(String key) {
        synchronized (appMapping) {
            AppUpdateStatus entry = appMapping.get(key);
            if (entry != null) {
                return entry.apk;
            }
            return null;
        }
    }

    public void removeApk(String key) {
        synchronized (appMapping) {
            AppUpdateStatus entry = appMapping.get(key);
            if (entry != null) {
                Utils.debugLog(LOGTAG, "Remove APK " + entry.apk.apkName);
                appMapping.remove(entry.apk.getUrl());
                notifyRemove(entry);
            }
        }
    }

    public void updateApkProgress(String key, int max, int current) {
        synchronized (appMapping) {
            AppUpdateStatus entry = appMapping.get(key);
            if (entry != null) {
                entry.progressMax = max;
                entry.progressCurrent = current;
                notifyChange(entry, false);
            }
        }
    }

    public void setApkError(Apk apk, String errorText) {
        synchronized (appMapping) {
            AppUpdateStatus entry = appMapping.get(apk.getUrl());
            if (entry == null) {
                entry = createAppEntry(apk, Status.InstallError, null);
            }
            entry.status = Status.InstallError;
            entry.errorText = errorText;
            entry.intent = getAppErrorIntent(entry);
            notifyChange(entry, false);
        }
    }

    private void startBatchUpdates() {
        synchronized (appMapping) {
            isBatchUpdating = true;
        }
    }

    private void endBatchUpdates() {
        synchronized (appMapping) {
            isBatchUpdating = false;
            notifyChange();
        }
    }

    void clearAllUpdates() {
        synchronized (appMapping) {
            for (Iterator<Map.Entry<String, AppUpdateStatus>> it = appMapping.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, AppUpdateStatus> entry = it.next();
                if (entry.getValue().status != Status.Installed) {
                    it.remove();
                }
            }
            notifyChange();
        }
    }

    void clearAllInstalled() {
        synchronized (appMapping) {
            for (Iterator<Map.Entry<String, AppUpdateStatus>> it = appMapping.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, AppUpdateStatus> entry = it.next();
                if (entry.getValue().status == Status.Installed) {
                    it.remove();
                }
            }
            notifyChange();
        }
    }

    private PendingIntent getContentIntent(AppUpdateStatus entry) {
        switch (entry.status) {
            case UpdateAvailable:
            case ReadyToInstall:
                // Make sure we have an intent to install the app. If not set, we create an intent
                // to open up the app details page for the app. From there, the user can hit "install"
                return getAppDetailsIntent(entry.apk);

            case InstallError:
                return getAppErrorIntent(entry);

            case Installed:
                PackageManager pm = context.getPackageManager();
                Intent intentObject = pm.getLaunchIntentForPackage(entry.app.packageName);
                if (intentObject != null) {
                    return PendingIntent.getActivity(context, 0, intentObject, 0);
                } else {
                    // Could not get launch intent, maybe not launchable, e.g. a keyboard
                    return getAppDetailsIntent(entry.apk);
                }
        }
        return null;
    }

    /**
     * Get a {@link PendingIntent} for a {@link Notification} to send when it
     * is clicked.  {@link AppDetails} handles {@code Intent}s that are missing
     * or bad {@link AppDetails#EXTRA_APPID}, so it does not need to be checked
     * here.
     */
    private PendingIntent getAppDetailsIntent(Apk apk) {
        Intent notifyIntent = new Intent(context, AppDetails.class)
                .putExtra(AppDetails.EXTRA_APPID, apk.packageName);

        return TaskStackBuilder.create(context)
                .addParentStack(AppDetails.class)
                .addNextIntent(notifyIntent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getAppErrorIntent(AppUpdateStatus entry) {
        String title = String.format(context.getString(R.string.install_error_notify_title), entry.app.name);

        Intent errorDialogIntent = new Intent(context, ErrorDialogActivity.class)
                .putExtra(ErrorDialogActivity.EXTRA_TITLE, title)
                .putExtra(ErrorDialogActivity.EXTRA_MESSAGE, entry.errorText);

        return PendingIntent.getActivity(
                context,
                0,
                errorDialogIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
