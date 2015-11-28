package org.fdroid.fdroid;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Repo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Saves app and apk information to the database after a {@link RepoUpdater} has processed the
 * relevant index file.
 */
public class RepoPersister {

    private static final String TAG = "RepoPersister";

    /**
     * When an app already exists in the db, and we are updating it on the off chance that some
     * values changed in the index, some fields should not be updated. Rather, they should be
     * ignored, because they were explicitly set by the user, and hence can't be automatically
     * overridden by the index.
     *
     * NOTE: In the future, these attributes will be moved to a join table, so that the app table
     * is essentially completely transient, and can be nuked at any time.
     */
    private static final String[] APP_FIELDS_TO_IGNORE = {
        AppProvider.DataColumns.IGNORE_ALLUPDATES,
        AppProvider.DataColumns.IGNORE_THISUPDATE,
    };

    @NonNull
    private final Context context;

    private final Map<String, App> appsToUpdate = new HashMap<>();
    private final List<Apk> apksToUpdate = new ArrayList<>();
    private final List<Repo> repos = new ArrayList<>();

    public RepoPersister(@NonNull Context context) {
        this.context = context;
    }

    public RepoPersister queueUpdater(RepoUpdater updater) {
        queueApps(updater.getApps());
        queueApks(updater.getApks());
        repos.add(updater.repo);
        return this;
    }

    private void queueApps(List<App> apps) {
        for (final App app : apps) {
            appsToUpdate.put(app.id, app);
        }
    }

    private void queueApks(List<Apk> apks) {
        apksToUpdate.addAll(apks);
    }

    public void save(List<Repo> disabledRepos) {

        List<App> listOfAppsToUpdate = new ArrayList<>();
        listOfAppsToUpdate.addAll(appsToUpdate.values());

        calcApkCompatibilityFlags(apksToUpdate);

        // Need to do this BEFORE updating the apks, otherwise when it continually
        // calls "get existing apks for repo X" then it will be getting the newly
        // created apks, rather than those from the fresh, juicy index we just processed.
        removeApksNoLongerInRepo(apksToUpdate, repos);

        int totalInsertsUpdates = listOfAppsToUpdate.size() + apksToUpdate.size();
        updateOrInsertApps(listOfAppsToUpdate, totalInsertsUpdates, 0);
        updateOrInsertApks(apksToUpdate, totalInsertsUpdates, listOfAppsToUpdate.size());
        removeApksFromRepos(disabledRepos);
        removeAppsWithoutApks();

        // This will sort out the icon urls, compatibility flags. and suggested version
        // for each app. It used to happen here in Java code, but was moved to SQL when
        // it became apparant we don't always have enough info (depending on which repos
        // were updated).
        AppProvider.Helper.calcDetailsFromIndex(context);

    }

    /**
     * This cannot be offloaded to the database (as we did with the query which
     * updates apps, depending on whether their apks are compatible or not).
     * The reason is that we need to interact with the CompatibilityChecker
     * in order to see if, and why an apk is not compatible.
     */
    private void calcApkCompatibilityFlags(List<Apk> apks) {
        final CompatibilityChecker checker = new CompatibilityChecker(context);
        for (final Apk apk : apks) {
            final List<String> reasons = checker.getIncompatibleReasons(apk);
            if (reasons.size() > 0) {
                apk.compatible = false;
                apk.incompatibleReasons = Utils.CommaSeparatedList.make(reasons);
            } else {
                apk.compatible = true;
                apk.incompatibleReasons = null;
            }
        }
    }

    /**
     * If a repo was updated (i.e. it is in use, and the index has changed
     * since last time we did an update), then we want to remove any apks that
     * belong to the repo which are not in the current list of apks that were
     * retrieved.
     */
    private void removeApksNoLongerInRepo(List<Apk> apksToUpdate, List<Repo> updatedRepos) {

        long startTime = System.currentTimeMillis();
        List<Apk> toRemove = new ArrayList<>();

        final String[] fields = {
            ApkProvider.DataColumns.APK_ID,
            ApkProvider.DataColumns.VERSION_CODE,
            ApkProvider.DataColumns.VERSION,
        };

        for (final Repo repo : updatedRepos) {
            final List<Apk> existingApks = ApkProvider.Helper.findByRepo(context, repo, fields);
            for (final Apk existingApk : existingApks) {
                if (!isApkToBeUpdated(existingApk, apksToUpdate)) {
                    toRemove.add(existingApk);
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        Utils.debugLog(TAG, "Found " + toRemove.size() + " apks no longer in the updated repos (took " + duration + "ms)");

        if (toRemove.size() > 0) {
            ApkProvider.Helper.deleteApks(context, toRemove);
        }
    }

    private void updateOrInsertApps(List<App> appsToUpdate, int totalUpdateCount, int currentCount) {

        List<ContentProviderOperation> operations = new ArrayList<>();
        List<String> knownAppIds = getKnownAppIds(appsToUpdate);
        for (final App app : appsToUpdate) {
            if (knownAppIds.contains(app.id)) {
                operations.add(updateExistingApp(app));
            } else {
                operations.add(insertNewApp(app));
            }
        }

        Utils.debugLog(TAG, "Updating/inserting " + operations.size() + " apps.");
        try {
            executeBatchWithStatus(AppProvider.getAuthority(), operations, currentCount, totalUpdateCount);
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Could not update or insert apps", e);
        }
    }

    private void executeBatchWithStatus(String providerAuthority,
                                        List<ContentProviderOperation> operations,
                                        int currentCount,
                                        int totalUpdateCount)
        throws RemoteException, OperationApplicationException {
        int i = 0;
        while (i < operations.size()) {
            int count = Math.min(operations.size() - i, 100);
            int progress = (int) ((double) (currentCount + i) / totalUpdateCount * 100);
            ArrayList<ContentProviderOperation> o = new ArrayList<>(operations.subList(i, i + count));
            UpdateService.sendStatus(context, UpdateService.STATUS_INFO, context.getString(
                R.string.status_inserting, progress), progress);
            context.getContentResolver().applyBatch(providerAuthority, o);
            i += 100;
        }
    }

    /**
     * Return list of apps from the "apks" argument which are already in the database.
     */
    private List<Apk> getKnownApks(List<Apk> apks) {
        final String[] fields = {
            ApkProvider.DataColumns.APK_ID,
            ApkProvider.DataColumns.VERSION,
            ApkProvider.DataColumns.VERSION_CODE,
        };
        return ApkProvider.Helper.knownApks(context, apks, fields);
    }

    private void updateOrInsertApks(List<Apk> apksToUpdate, int totalApksAppsCount, int currentCount) {

        List<ContentProviderOperation> operations = new ArrayList<>();

        List<Apk> knownApks = getKnownApks(apksToUpdate);
        for (final Apk apk : apksToUpdate) {
            boolean known = false;
            for (final Apk knownApk : knownApks) {
                if (knownApk.id.equals(apk.id) && knownApk.vercode == apk.vercode) {
                    known = true;
                    break;
                }
            }

            if (known) {
                operations.add(updateExistingApk(apk));
            } else {
                operations.add(insertNewApk(apk));
                knownApks.add(apk); // In case another repo has the same version/id combo for this apk.
            }
        }

        Utils.debugLog(TAG, "Updating/inserting " + operations.size() + " apks.");
        try {
            executeBatchWithStatus(ApkProvider.getAuthority(), operations, currentCount, totalApksAppsCount);
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Could not update/insert apps", e);
        }
    }

    private ContentProviderOperation updateExistingApk(final Apk apk) {
        Uri uri = ApkProvider.getContentUri(apk);
        ContentValues values = apk.toContentValues();
        return ContentProviderOperation.newUpdate(uri).withValues(values).build();
    }

    private ContentProviderOperation insertNewApk(final Apk apk) {
        ContentValues values = apk.toContentValues();
        Uri uri = ApkProvider.getContentUri();
        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    private ContentProviderOperation updateExistingApp(App app) {
        Uri uri = AppProvider.getContentUri(app);
        ContentValues values = app.toContentValues();
        for (final String toIgnore : APP_FIELDS_TO_IGNORE) {
            if (values.containsKey(toIgnore)) {
                values.remove(toIgnore);
            }
        }
        return ContentProviderOperation.newUpdate(uri).withValues(values).build();
    }

    private ContentProviderOperation insertNewApp(App app) {
        ContentValues values = app.toContentValues();
        Uri uri = AppProvider.getContentUri();
        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    private static boolean isApkToBeUpdated(Apk existingApk, List<Apk> apksToUpdate) {
        for (final Apk apkToUpdate : apksToUpdate) {
            if (apkToUpdate.vercode == existingApk.vercode && apkToUpdate.id.equals(existingApk.id)) {
                return true;
            }
        }
        return false;
    }

    private void removeApksFromRepos(List<Repo> repos) {
        for (final Repo repo : repos) {
            Uri uri = ApkProvider.getRepoUri(repo.getId());
            int numDeleted = context.getContentResolver().delete(uri, null, null);
            Utils.debugLog(TAG, "Removing " + numDeleted + " apks from repo " + repo.address);
        }
    }

    private void removeAppsWithoutApks() {
        int numDeleted = context.getContentResolver().delete(AppProvider.getNoApksUri(), null, null);
        Utils.debugLog(TAG, "Removing " + numDeleted + " apks that don't have any apks");
    }

    private List<String> getKnownAppIds(List<App> apps) {
        List<String> knownAppIds = new ArrayList<>();
        if (apps.isEmpty()) {
            return knownAppIds;
        }
        if (apps.size() > AppProvider.MAX_APPS_TO_QUERY) {
            int middle = apps.size() / 2;
            List<App> apps1 = apps.subList(0, middle);
            List<App> apps2 = apps.subList(middle, apps.size());
            knownAppIds.addAll(getKnownAppIds(apps1));
            knownAppIds.addAll(getKnownAppIds(apps2));
        } else {
            knownAppIds.addAll(getKnownAppIdsFromProvider(apps));
        }
        return knownAppIds;
    }

    /**
     * Looks in the database to see which apps we already know about. Only
     * returns ids of apps that are in the database if they are in the "apps"
     * array.
     */
    private List<String> getKnownAppIdsFromProvider(List<App> apps) {

        final Uri uri = AppProvider.getContentUri(apps);
        final String[] fields = {AppProvider.DataColumns.APP_ID};
        Cursor cursor = context.getContentResolver().query(uri, fields, null, null, null);

        int knownIdCount = cursor != null ? cursor.getCount() : 0;
        List<String> knownIds = new ArrayList<>(knownIdCount);
        if (cursor != null) {
            if (knownIdCount > 0) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    knownIds.add(cursor.getString(0));
                    cursor.moveToNext();
                }
            }
            cursor.close();
        }

        return knownIds;
    }

}

