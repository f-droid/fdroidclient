package org.fdroid.fdroid;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.installer.ErrorDialogActivity;
import org.fdroid.fdroid.installer.InstallManagerService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Manages the state of APKs that are being installed or that have updates available.
 * This also ensures the state is saved across F-Droid restarts, and repopulates
 * based on {@link org.fdroid.fdroid.data.Schema.InstalledAppTable} data, APKs that
 * are present in the cache, and the {@code apks-pending-install}
 * {@link SharedPreferences} instance.
 * <p>
 * As defined in {@link org.fdroid.fdroid.installer.InstallManagerService}, the
 * canonical URL for the APK file to download is used as the unique ID to represent
 * the status of the APK throughout F-Droid.
 *
 * @see org.fdroid.fdroid.installer.InstallManagerService
 */
public final class AppUpdateStatusManager {

    public static final String TAG = "AppUpdateStatusManager";

    /**
     * Broadcast when:
     * * The user clears the list of installed apps from notification manager.
     * * The user clears the list of apps available to update from the notification manager.
     * * A repo update is completed and a bunch of new apps are ready to be updated.
     * * F-Droid is opened, and it finds a bunch of .apk files downloaded and ready to install.
     */
    public static final String BROADCAST_APPSTATUS_LIST_CHANGED = "org.fdroid.fdroid.installer.appstatus.listchange";

    /**
     * Broadcast when an app begins the download/install process (either manually or via an automatic download).
     */
    public static final String BROADCAST_APPSTATUS_ADDED = "org.fdroid.fdroid.installer.appstatus.appchange.add";

    /**
     * When the {@link AppUpdateStatus#status} of an app changes or the download progress for an app advances.
     */
    public static final String BROADCAST_APPSTATUS_CHANGED = "org.fdroid.fdroid.installer.appstatus.appchange.change";

    /**
     * Broadcast when:
     * * The associated app has the {@link Status#Installed} status, and the user either visits
     * that apps details page or clears the individual notification for the app.
     * * The download for an app is cancelled.
     */
    public static final String BROADCAST_APPSTATUS_REMOVED = "org.fdroid.fdroid.installer.appstatus.appchange.remove";

    public static final String EXTRA_APK_URL = "urlstring";
    public static final String EXTRA_STATUS = "status";

    public static final String EXTRA_REASON_FOR_CHANGE = "reason";

    public static final String REASON_READY_TO_INSTALL = "readytoinstall";
    public static final String REASON_UPDATES_AVAILABLE = "updatesavailable";
    public static final String REASON_CLEAR_ALL_UPDATES = "clearallupdates";
    public static final String REASON_CLEAR_ALL_INSTALLED = "clearallinstalled";
    public static final String REASON_REPO_DISABLED = "repodisabled";

    /**
     * If this is present and true, then the broadcast has been sent in response to the {@link AppUpdateStatus#status}
     * changing. In comparison, if it is just the download progress of an app then this should not be true.
     */
    public static final String EXTRA_IS_STATUS_UPDATE = "isstatusupdate";

    private static final String LOGTAG = "AppUpdateStatusManager";

    public enum Status {
        PendingInstall,
        DownloadInterrupted,
        UpdateAvailable,
        Downloading,
        ReadyToInstall,
        Installing,
        Installed,
        InstallError,
    }

    public static AppUpdateStatusManager getInstance(Context context) {
        if (instance == null) {
            instance = new AppUpdateStatusManager(context.getApplicationContext());
        }
        return instance;
    }

    private static AppUpdateStatusManager instance;

    public static class AppUpdateStatus implements Parcelable {
        public final App app;
        public final Apk apk;
        public Status status;
        public PendingIntent intent;
        public long progressCurrent;
        public long progressMax;
        public String errorText;

        AppUpdateStatus(App app, Apk apk, Status status, PendingIntent intent) {
            this.app = app;
            this.apk = apk;
            this.status = status;
            this.intent = intent;
        }

        /**
         * @return the unique ID used to represent this specific package's install process
         * also known as {@code urlString}.
         * @see org.fdroid.fdroid.installer.InstallManagerService
         */
        public String getUniqueKey() {
            return apk.getUrl();
        }

        /**
         * Dumps some information about the status for debugging purposes.
         */
        public String toString() {
            return app.packageName + " [Status: " + status
                    + ", Progress: " + progressCurrent + " / " + progressMax + ']';
        }

        protected AppUpdateStatus(Parcel in) {
            app = in.readParcelable(getClass().getClassLoader());
            apk = in.readParcelable(getClass().getClassLoader());
            intent = in.readParcelable(getClass().getClassLoader());
            status = (Status) in.readSerializable();
            progressCurrent = in.readLong();
            progressMax = in.readLong();
            errorText = in.readString();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeParcelable(app, 0);
            dest.writeParcelable(apk, 0);
            dest.writeParcelable(intent, 0);
            dest.writeSerializable(status);
            dest.writeLong(progressCurrent);
            dest.writeLong(progressMax);
            dest.writeString(errorText);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Parcelable.Creator<AppUpdateStatus> CREATOR = new Parcelable.Creator<AppUpdateStatus>() {
            @Override
            public AppUpdateStatus createFromParcel(Parcel in) {
                return new AppUpdateStatus(in);
            }

            @Override
            public AppUpdateStatus[] newArray(int size) {
                return new AppUpdateStatus[size];
            }
        };

        /**
         * When passing to the broadcast manager, it is important to pass a copy rather than the original object.
         * This is because if two status changes are noticed in the same event loop, than they will both refer
         * to the same status object. The objects are not parceled until the end of the event loop, and so the first
         * parceled event will refer to the updated object (with a different status) rather than the intended
         * status (i.e. the one in existence when talking to the broadcast manager).
         */
        public AppUpdateStatus copy() {
            AppUpdateStatus copy = new AppUpdateStatus(app, apk, status, intent);
            copy.errorText = errorText;
            copy.progressCurrent = progressCurrent;
            copy.progressMax = progressMax;
            return copy;
        }
    }

    private final Context context;
    private final LocalBroadcastManager localBroadcastManager;
    private final HashMap<String, AppUpdateStatus> appMapping = new HashMap<>();
    private boolean isBatchUpdating;

    private AppUpdateStatusManager(Context context) {
        this.context = context;
        localBroadcastManager = LocalBroadcastManager.getInstance(context.getApplicationContext());
    }

    public void removeAllByRepo(Repo repo) {
        boolean hasRemovedSome = false;
        Iterator<AppUpdateStatus> it = getAll().iterator();
        while (it.hasNext()) {
            AppUpdateStatus status = it.next();
            if (status.apk.repoId == repo.getId()) {
                it.remove();
                hasRemovedSome = true;
            }
        }

        if (hasRemovedSome) {
            notifyChange(REASON_REPO_DISABLED);
        }
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
     *
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

    private void updateApkInternal(@NonNull AppUpdateStatus entry, @NonNull Status status, PendingIntent intent) {
        Utils.debugLog(LOGTAG, "Update APK " + entry.apk.apkName + " state to " + status.name());
        boolean isStatusUpdate = entry.status != status;
        entry.status = status;
        entry.intent = intent;
        setEntryContentIntentIfEmpty(entry);
        notifyChange(entry, isStatusUpdate);

        if (status == Status.Installed) {
            InstallManagerService.removePendingInstall(context, entry.getUniqueKey());
        }
    }

    private void addApkInternal(@NonNull Apk apk, @NonNull Status status, PendingIntent intent) {
        Utils.debugLog(LOGTAG, "Add APK " + apk.apkName + " with state " + status.name());
        AppUpdateStatus entry = createAppEntry(apk, status, intent);
        setEntryContentIntentIfEmpty(entry);
        appMapping.put(entry.getUniqueKey(), entry);
        notifyAdd(entry);

        if (status == Status.Installed) {
            InstallManagerService.removePendingInstall(context, entry.getUniqueKey());
        }
    }

    private void notifyChange(String reason) {
        if (!isBatchUpdating) {
            Intent intent = new Intent(BROADCAST_APPSTATUS_LIST_CHANGED);
            intent.putExtra(EXTRA_REASON_FOR_CHANGE, reason);
            localBroadcastManager.sendBroadcast(intent);
        }
    }

    private void notifyAdd(AppUpdateStatus entry) {
        if (!isBatchUpdating) {
            Intent broadcastIntent = new Intent(BROADCAST_APPSTATUS_ADDED);
            broadcastIntent.putExtra(EXTRA_APK_URL, entry.getUniqueKey());
            broadcastIntent.putExtra(EXTRA_STATUS, entry.copy());
            localBroadcastManager.sendBroadcast(broadcastIntent);
        }
    }

    private void notifyChange(AppUpdateStatus entry, boolean isStatusUpdate) {
        if (!isBatchUpdating) {
            Intent broadcastIntent = new Intent(BROADCAST_APPSTATUS_CHANGED);
            broadcastIntent.putExtra(EXTRA_APK_URL, entry.getUniqueKey());
            broadcastIntent.putExtra(EXTRA_STATUS, entry.copy());
            broadcastIntent.putExtra(EXTRA_IS_STATUS_UPDATE, isStatusUpdate);
            localBroadcastManager.sendBroadcast(broadcastIntent);
        }
    }

    private void notifyRemove(AppUpdateStatus entry) {
        if (!isBatchUpdating) {
            Intent broadcastIntent = new Intent(BROADCAST_APPSTATUS_REMOVED);
            broadcastIntent.putExtra(EXTRA_APK_URL, entry.getUniqueKey());
            broadcastIntent.putExtra(EXTRA_STATUS, entry.copy());
            localBroadcastManager.sendBroadcast(broadcastIntent);
        }
    }

    private AppUpdateStatus createAppEntry(Apk apk, Status status, PendingIntent intent) {
        synchronized (appMapping) {
            ContentResolver resolver = context.getContentResolver();
            App app = AppProvider.Helper.findSpecificApp(resolver, apk.packageName, apk.repoId);
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
        endBatchUpdates(status);
    }

    /**
     * Add an Apk to the AppUpdateStatusManager manager (or update it if we already know about it).
     *
     * @param apk           The apk to add.
     * @param status        The current status of the app
     * @param pendingIntent Action when notification is clicked. Can be null for default action(s)
     */
    public void addApk(Apk apk, @NonNull Status status, @Nullable PendingIntent pendingIntent) {
        if (apk == null) {
            return;
        }

        synchronized (appMapping) {
            AppUpdateStatus entry = appMapping.get(apk.getUrl());
            if (entry != null) {
                updateApkInternal(entry, status, pendingIntent);
            } else {
                addApkInternal(apk, status, pendingIntent);
            }
        }
    }

    /**
     * @param pendingIntent Action when notification is clicked. Can be null for default action(s)
     */
    public void updateApk(String key, @NonNull Status status, @Nullable PendingIntent pendingIntent) {
        synchronized (appMapping) {
            AppUpdateStatus entry = appMapping.get(key);
            if (entry != null) {
                updateApkInternal(entry, status, pendingIntent);
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

    /**
     * Remove an APK from being tracked, since it is now considered {@link Status#Installed}
     *
     * @param key the unique ID for the install process, also called {@code urlString}
     * @see org.fdroid.fdroid.installer.InstallManagerService
     */
    public void removeApk(String key) {
        synchronized (appMapping) {
            InstallManagerService.removePendingInstall(context, key);
            AppUpdateStatus entry = appMapping.remove(key);
            if (entry != null) {
                Utils.debugLog(LOGTAG, "Remove APK " + entry.apk.apkName);
                notifyRemove(entry);
            }
        }
    }

    public void refreshApk(String key) {
        synchronized (appMapping) {
            AppUpdateStatus entry = appMapping.get(key);
            if (entry != null) {
                Utils.debugLog(LOGTAG, "Refresh APK " + entry.apk.apkName);
                notifyChange(entry, true);
            }
        }
    }

    public void updateApkProgress(String key, long max, long current) {
        synchronized (appMapping) {
            AppUpdateStatus entry = appMapping.get(key);
            if (entry != null) {
                entry.progressMax = max;
                entry.progressCurrent = current;
                notifyChange(entry, false);
            }
        }
    }

    /**
     * @param errorText If null, then it is likely because the user cancelled the download.
     */
    public void setDownloadError(String url, @Nullable String errorText) {
        synchronized (appMapping) {
            AppUpdateStatus entry = appMapping.get(url);
            if (entry != null) {
                entry.status = Status.DownloadInterrupted;
                entry.errorText = errorText;
                entry.intent = null;
                notifyChange(entry, true);
                removeApk(url);
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

            InstallManagerService.removePendingInstall(context, entry.getUniqueKey());
        }
    }

    private void startBatchUpdates() {
        synchronized (appMapping) {
            isBatchUpdating = true;
        }
    }

    private void endBatchUpdates(Status status) {
        synchronized (appMapping) {
            isBatchUpdating = false;

            String reason = null;
            if (status == Status.ReadyToInstall) {
                reason = REASON_READY_TO_INSTALL;
            } else if (status == Status.UpdateAvailable) {
                reason = REASON_UPDATES_AVAILABLE;
            }
            notifyChange(reason);
        }
    }

    @SuppressWarnings("LineLength")
    void clearAllUpdates() {
        synchronized (appMapping) {
            for (Iterator<Map.Entry<String, AppUpdateStatus>> it = appMapping.entrySet().iterator(); it.hasNext(); ) { // NOCHECKSTYLE EmptyForIteratorPad
                Map.Entry<String, AppUpdateStatus> entry = it.next();
                if (entry.getValue().status != Status.Installed) {
                    it.remove();
                }
            }
            notifyChange(REASON_CLEAR_ALL_UPDATES);
        }
    }

    @SuppressWarnings("LineLength")
    void clearAllInstalled() {
        synchronized (appMapping) {
            for (Iterator<Map.Entry<String, AppUpdateStatus>> it = appMapping.entrySet().iterator(); it.hasNext(); ) { // NOCHECKSTYLE EmptyForIteratorPad
                Map.Entry<String, AppUpdateStatus> entry = it.next();
                if (entry.getValue().status == Status.Installed) {
                    it.remove();
                }
            }
            notifyChange(REASON_CLEAR_ALL_INSTALLED);
        }
    }

    /**
     * If the {@link PendingIntent} aimed at {@link Notification.Builder#setContentIntent(PendingIntent)}
     * is not set, then create a default one.  The goal is to link the notification
     * to the most relevant action, like the installer if the APK is downloaded, or the launcher once
     * installed, if possible, or other relevant action. If there is no app launch
     * {@code PendingIntent}, the app is probably not launchable, e.g. its a keyboard.
     * If there is not an {@code PendingIntent} to install the app, this creates an {@code PendingIntent}
     * to open up the app details page for the app. From there, the user can hit "install".
     * <p>
     * Before {@code android-11}, a {@code ContentIntent} was required in every
     * {@link Notification}.  This generates a boilerplate one for places where
     * there isn't an obvious one.
     */
    private void setEntryContentIntentIfEmpty(AppUpdateStatus entry) {
        if (entry.intent != null) {
            return;
        }
        switch (entry.status) {
            case UpdateAvailable:
            case ReadyToInstall:
                entry.intent = getAppDetailsIntent(entry.apk);
                break;
            case InstallError:
                entry.intent = getAppErrorIntent(entry);
                break;
            case Installed:
                PackageManager pm = context.getPackageManager();
                Intent intentObject = pm.getLaunchIntentForPackage(entry.app.packageName);
                if (intentObject != null) {
                    entry.intent = PendingIntent.getActivity(context, 0, intentObject, 0);
                } else {
                    entry.intent = getAppDetailsIntent(entry.apk);
                }
                break;
        }
    }

    /**
     * Get a {@link PendingIntent} for a {@link Notification} to send when it
     * is clicked.  {@link AppDetails2} handles {@code Intent}s that are missing
     * or bad {@link AppDetails2#EXTRA_APPID}, so it does not need to be checked
     * here.
     */
    private PendingIntent getAppDetailsIntent(Apk apk) {
        Intent notifyIntent = new Intent(context, AppDetails2.class)
                .putExtra(AppDetails2.EXTRA_APPID, apk.packageName);

        return TaskStackBuilder.create(context)
                .addParentStack(AppDetails2.class)
                .addNextIntent(notifyIntent)
                .getPendingIntent(apk.packageName.hashCode(), PendingIntent.FLAG_UPDATE_CURRENT);
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