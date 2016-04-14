package org.fdroid.fdroid.data;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;

import org.fdroid.fdroid.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Compares what is in the fdroid_installedApp SQLite database table with the package
 * info that we can gleam from the {@link android.content.pm.PackageManager}. If there
 * is any updates/removals/insertions which need to take place, we will perform them.
 * TODO: The content providers are not thread safe, so it is possible we will be writing
 * to the database at the same time we respond to a broadcasted intent.
 */
public final class InstalledAppCacheUpdater {

    private static final String TAG = "InstalledAppCache";

    private final Context context;

    private final List<PackageInfo> toInsert = new ArrayList<>();
    private final List<String>      toDelete = new ArrayList<>();

    private InstalledAppCacheUpdater(Context context) {
        this.context = context;
    }

    /**
     * Ensure our database of installed apps is in sync with what the PackageManager tells us is installed.
     * Once completed, the relevant ContentProviders will be notified of any changes to installed statuses.
     * This method will block until completed, which could be in the order of a few seconds (depending on
     * how many apps are installed).
     */
    public static void updateInForeground(Context context) {
        InstalledAppCacheUpdater updater = new InstalledAppCacheUpdater(context);
        if (updater.update()) {
            updater.notifyProviders();
        }
    }

    /**
     * Ensure our database of installed apps is in sync with what the PackageManager tells us is installed.
     * The installed app cache hasn't gotten out of sync somehow, e.g. if we crashed/ran out of battery
     * half way through responding to a package installed {@link android.content.Intent}. Once completed,
     * the relevant {@link android.content.ContentProvider}s will be notified of any changes to installed
     * statuses. This method returns immediately, and will continue to work in an AsyncTask.  It doesn't
     * really matter where we put this in the bootstrap process, because it runs on a different thread,
     * which will be delayed by some seconds to avoid an error where the database is locked due to the
     * database updater.
     */
    public static void updateInBackground(Context context) {
        InstalledAppCacheUpdater updater = new InstalledAppCacheUpdater(context);
        updater.startBackgroundWorker();
    }

    private boolean update() {

        long startTime = System.currentTimeMillis();

        compareCacheToPackageManager();
        updateCache();

        long duration = System.currentTimeMillis() - startTime;
        Utils.debugLog(TAG, "Took " + duration + "ms to compare the installed app cache with PackageManager.");

        return hasChanged();
    }

    private void notifyProviders() {
        Utils.debugLog(TAG, "Installed app cache has changed, notifying content providers (so they can update the relevant views).");
        context.getContentResolver().notifyChange(AppProvider.getContentUri(), null);
        context.getContentResolver().notifyChange(ApkProvider.getContentUri(), null);
    }

    private void startBackgroundWorker() {
        new PostponedWorker().execute();
    }

    /**
     * If any of the cached app details have been removed, updated or inserted,
     * then the cache has changed.
     */
    private boolean hasChanged() {
        return toInsert.size() > 0 || toDelete.size() > 0;
    }

    private void updateCache() {

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.addAll(deleteFromCache(toDelete));
        ops.addAll(insertIntoCache(toInsert));

        if (ops.size() > 0) {
            try {
                context.getContentResolver().applyBatch(InstalledAppProvider.getAuthority(), ops);
                Utils.debugLog(TAG, "Finished executing " + ops.size() + " CRUD operations on installed app cache.");
            } catch (RemoteException | OperationApplicationException e) {
                Log.e(TAG, "Error updating installed app cache: " + e);
            }
        }

    }

    private void compareCacheToPackageManager() {

        Map<String, Integer> cachedInfo = InstalledAppProvider.Helper.all(context);

        List<PackageInfo> installedPackages = context.getPackageManager()
                .getInstalledPackages(PackageManager.GET_SIGNATURES);
        for (PackageInfo appInfo : installedPackages) {
            toInsert.add(appInfo);
            if (cachedInfo.containsKey(appInfo.packageName)) {
                cachedInfo.remove(appInfo.packageName);
            }
        }

        if (cachedInfo.size() > 0) {
            for (Map.Entry<String, Integer> entry : cachedInfo.entrySet()) {
                toDelete.add(entry.getKey());
            }
        }
    }

    private List<ContentProviderOperation> insertIntoCache(List<PackageInfo> appsToInsert) {
        List<ContentProviderOperation> ops = new ArrayList<>(appsToInsert.size());
        if (appsToInsert.size() > 0) {
            Utils.debugLog(TAG, "Preparing to cache installed info for " + appsToInsert.size() + " new apps.");
            Uri uri = InstalledAppProvider.getContentUri();
            for (PackageInfo info : appsToInsert) {
                ContentProviderOperation op = ContentProviderOperation.newInsert(uri)
                        .withValue(InstalledAppProvider.DataColumns.PACKAGE_NAME, info.packageName)
                        .withValue(InstalledAppProvider.DataColumns.VERSION_CODE, info.versionCode)
                        .withValue(InstalledAppProvider.DataColumns.VERSION_NAME, info.versionName)
                        .withValue(InstalledAppProvider.DataColumns.APPLICATION_LABEL,
                                InstalledAppProvider.getApplicationLabel(context, info.packageName))
                        .withValue(InstalledAppProvider.DataColumns.SIGNATURE,
                                InstalledAppProvider.getPackageSig(info))
                        .build();
                ops.add(op);
            }
        }
        return ops;
    }

    private List<ContentProviderOperation> deleteFromCache(List<String> packageNames) {
        List<ContentProviderOperation> ops = new ArrayList<>(packageNames.size());
        if (packageNames.size() > 0) {
            Utils.debugLog(TAG, "Preparing to remove " + packageNames.size() + " apps from the installed app cache.");
            for (final String packageName : packageNames) {
                Uri uri = InstalledAppProvider.getAppUri(packageName);
                ops.add(ContentProviderOperation.newDelete(uri).build());
            }
        }
        return ops;
    }

    /**
     * Waits 5 seconds before beginning to update cache of installed apps.
     * This is due to a bug where the database was locked as F-Droid was starting,
     * which caused a crash.
     */
    private class PostponedWorker extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) { }
            return update();
        }

        @Override
        protected void onPostExecute(Boolean changed) {
            if (changed) {
                notifyProviders();
            }
        }
    }

}
