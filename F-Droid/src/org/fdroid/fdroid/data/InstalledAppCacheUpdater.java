package org.fdroid.fdroid.data;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;

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
public class InstalledAppCacheUpdater {

    private static final String TAG = "InstalledAppCache";

    private final Context context;

    private final List<PackageInfo> toInsert = new ArrayList<>();
    private final List<String>      toDelete = new ArrayList<>();

    protected InstalledAppCacheUpdater(Context context) {
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
     * Once completed, the relevant ContentProviders will be notified of any changes to installed statuses.
     * This method returns immediately, and will continue to work in an AsyncTask.
     */
    public static void updateInBackground(Context context) {
        InstalledAppCacheUpdater updater = new InstalledAppCacheUpdater(context);
        updater.startBackgroundWorker();
    }

    protected boolean update() {

        long startTime = System.currentTimeMillis();

        compareCacheToPackageManager();
        updateCache();

        long duration = System.currentTimeMillis() - startTime;
        Log.d(TAG, "Took " + duration + "ms to compare the installed app cache with PackageManager.");

        return hasChanged();
    }

    protected void notifyProviders() {
        Log.i(TAG, "Installed app cache has changed, notifying content providers (so they can update the relevant views).");
        context.getContentResolver().notifyChange(AppProvider.getContentUri(), null);
        context.getContentResolver().notifyChange(ApkProvider.getContentUri(), null);
    }

    protected void startBackgroundWorker() {
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
                Log.d(TAG, "Finished executing " + ops.size() + " CRUD operations on installed app cache.");
            } catch (RemoteException | OperationApplicationException e) {
                Log.e(TAG, "Error updating installed app cache: " + e);
            }
        }

    }

    private void compareCacheToPackageManager() {

        Map<String, Integer> cachedInfo = InstalledAppProvider.Helper.all(context);

        List<PackageInfo> installedPackages = context.getPackageManager().getInstalledPackages(0);
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
            Log.d(TAG, "Preparing to cache installed info for " + appsToInsert.size() + " new apps.");
            Uri uri = InstalledAppProvider.getContentUri();
            for (PackageInfo info : appsToInsert) {
                ContentProviderOperation op = ContentProviderOperation.newInsert(uri)
                    .withValue(InstalledAppProvider.DataColumns.APP_ID, info.packageName)
                    .withValue(InstalledAppProvider.DataColumns.VERSION_CODE, info.versionCode)
                    .withValue(InstalledAppProvider.DataColumns.VERSION_NAME, info.versionName)
                    .withValue(InstalledAppProvider.DataColumns.APPLICATION_LABEL,
                            InstalledAppProvider.getApplicationLabel(context, info.packageName))
                    .build();
                ops.add(op);
            }
        }
        return ops;
    }

    private List<ContentProviderOperation> deleteFromCache(List<String> appIds) {
        List<ContentProviderOperation> ops = new ArrayList<>(appIds.size());
        if (appIds.size() > 0) {
            Log.d(TAG, "Preparing to remove " + appIds.size() + " apps from the installed app cache.");
            for (final String appId : appIds) {
                Uri uri = InstalledAppProvider.getAppUri(appId);
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
            } catch (InterruptedException ignored) {}
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
