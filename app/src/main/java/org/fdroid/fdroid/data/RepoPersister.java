package org.fdroid.fdroid.data;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.fdroid.fdroid.CompatibilityChecker;
import org.fdroid.fdroid.RepoUpdater;
import org.fdroid.fdroid.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Crappy benchmark with a Nexus 4, Android 5.0 on a fairly crappy internet connection I get:
     * * 25 = 37 seconds
     * * 50 = 33 seconds
     * * 100 = 30 seconds
     * * 200 = 32 seconds
     * Raising this means more memory consumption, so we'd like it to be low, but not
     * so low that it takes too long.
     */
    private static final int MAX_APP_BUFFER = 50;

    @NonNull
    private final Repo repo;

    private boolean hasBeenInitialized;

    @NonNull
    private final Context context;

    @NonNull
    private final List<App> appsToSave = new ArrayList<>();

    @NonNull
    private final Map<String, List<Apk>> apksToSave = new HashMap<>();

    @NonNull
    private final CompatibilityChecker checker;

    public RepoPersister(@NonNull Context context, @NonNull Repo repo) {
        this.repo = repo;
        this.context = context;
        checker = new CompatibilityChecker(context);
    }

    public void saveToDb(App app, List<Apk> packages) throws RepoUpdater.UpdateException {
        appsToSave.add(app);
        apksToSave.put(app.packageName, packages);

        if (appsToSave.size() >= MAX_APP_BUFFER) {
            flushBufferToDb();
        }
    }

    public void commit(ContentValues repoDetailsToSave) throws RepoUpdater.UpdateException {
        flushBufferToDb();
        TempAppProvider.Helper.commitAppsAndApks(context);
        RepoProvider.Helper.update(context, repo, repoDetailsToSave);
    }

    private void flushBufferToDb() throws RepoUpdater.UpdateException {
        if (!hasBeenInitialized) {
            // This is where we will store all of the metadata before commiting at the
            // end of the process. This is due to the fact that we can't verify the cert
            // the index was signed with until we've finished reading it - and we don't
            // want to put stuff in the real database until we are sure it is from a
            // trusted source.
            TempAppProvider.Helper.init(context);
            TempApkProvider.Helper.init(context);
            hasBeenInitialized = true;
        }

        if (apksToSave.size() > 0 || appsToSave.size() > 0) {
            Log.d(TAG, "Flushing details of up to " + MAX_APP_BUFFER + " apps and their packages to the database.");
            flushAppsToDbInBatch();
            flushApksToDbInBatch();
            apksToSave.clear();
            appsToSave.clear();
        }
    }

    private void flushApksToDbInBatch() throws RepoUpdater.UpdateException {
        List<Apk> apksToSaveList = new ArrayList<>();
        for (Map.Entry<String, List<Apk>> entries : apksToSave.entrySet()) {
            apksToSaveList.addAll(entries.getValue());
        }

        calcApkCompatibilityFlags(apksToSaveList);

        ArrayList<ContentProviderOperation> apkOperations = new ArrayList<>();
        ContentProviderOperation clearOrphans = deleteOrphanedApks(appsToSave, apksToSave);
        if (clearOrphans != null) {
            apkOperations.add(clearOrphans);
        }
        apkOperations.addAll(insertOrUpdateApks(apksToSaveList));

        try {
            context.getContentResolver().applyBatch(TempApkProvider.getAuthority(), apkOperations);
        } catch (RemoteException | OperationApplicationException e) {
            throw new RepoUpdater.UpdateException(repo, "An internal error occurred while updating the database", e);
        }
    }

    private void flushAppsToDbInBatch() throws RepoUpdater.UpdateException {
        ArrayList<ContentProviderOperation> appOperations = insertOrUpdateApps(appsToSave);

        try {
            context.getContentResolver().applyBatch(TempAppProvider.getAuthority(), appOperations);
        } catch (RemoteException | OperationApplicationException e) {
            throw new RepoUpdater.UpdateException(repo, "An internal error occurred while updating the database", e);
        }
    }

    /**
     * Depending on whether the {@link App}s have been added to the database previously, this
     * will queue up an update or an insert {@link ContentProviderOperation} for each app.
     */
    private ArrayList<ContentProviderOperation> insertOrUpdateApps(List<App> apps) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>(apps.size());
        for (App app : apps) {
            if (isAppInDatabase(app)) {
                operations.add(updateExistingApp(app));
            } else {
                operations.add(insertNewApp(app));
            }
        }
        return operations;
    }

    /**
     * Depending on whether the .apks have been added to the database previously, this
     * will queue up an update or an insert {@link ContentProviderOperation} for each package.
     */
    private ArrayList<ContentProviderOperation> insertOrUpdateApks(List<Apk> packages) {
        String[] projection = new String[]{
            ApkProvider.DataColumns.PACKAGE_NAME,
            ApkProvider.DataColumns.VERSION_CODE,
        };
        List<Apk> existingApks = ApkProvider.Helper.knownApks(context, packages, projection);
        ArrayList<ContentProviderOperation> operations = new ArrayList<>(packages.size());
        for (Apk apk : packages) {
            boolean exists = false;
            for (Apk existing : existingApks) {
                if (existing.packageName.equals(apk.packageName) && existing.vercode == apk.vercode) {
                    exists = true;
                    break;
                }
            }

            if (exists) {
                operations.add(updateExistingApk(apk));
            } else {
                operations.add(insertNewApk(apk));
            }
        }

        return operations;
    }

    /**
     * Creates an update {@link ContentProviderOperation} for the {@link App} in question.
     * <strong>Does not do any checks to see if the app already exists or not.</strong>
     */
    private ContentProviderOperation updateExistingApp(App app) {
        Uri uri = TempAppProvider.getAppUri(app);
        ContentValues values = app.toContentValues();
        for (final String toIgnore : APP_FIELDS_TO_IGNORE) {
            if (values.containsKey(toIgnore)) {
                values.remove(toIgnore);
            }
        }
        return ContentProviderOperation.newUpdate(uri).withValues(values).build();
    }

    /**
     * Creates an insert {@link ContentProviderOperation} for the {@link App} in question.
     * <strong>Does not do any checks to see if the app already exists or not.</strong>
     */
    private ContentProviderOperation insertNewApp(App app) {
        ContentValues values = app.toContentValues();
        Uri uri = TempAppProvider.getContentUri();
        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    /**
     * Looks in the database to see which apps we already know about. Only
     * returns ids of apps that are in the database if they are in the "apps"
     * array.
     */
    private boolean isAppInDatabase(App app) {
        String[] fields = {AppProvider.DataColumns.PACKAGE_NAME};
        App found = AppProvider.Helper.findByPackageName(context.getContentResolver(), app.packageName, fields);
        return found != null;
    }

    /**
     * Creates an update {@link ContentProviderOperation} for the {@link Apk} in question.
     * <strong>Does not do any checks to see if the apk already exists or not.</strong>
     */
    private ContentProviderOperation updateExistingApk(final Apk apk) {
        Uri uri = TempApkProvider.getApkUri(apk);
        ContentValues values = apk.toContentValues();
        return ContentProviderOperation.newUpdate(uri).withValues(values).build();
    }

    /**
     * Creates an insert {@link ContentProviderOperation} for the {@link Apk} in question.
     * <strong>Does not do any checks to see if the apk already exists or not.</strong>
     */
    private ContentProviderOperation insertNewApk(final Apk apk) {
        ContentValues values = apk.toContentValues();
        Uri uri = TempApkProvider.getContentUri();
        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    /**
     * Finds all apks from the repo we are currently updating, that belong to the specified app,
     * and delete them as they are no longer provided by that repo.
     */
    @Nullable
    private ContentProviderOperation deleteOrphanedApks(List<App> apps, Map<String, List<Apk>> packages) {
        String[] projection = new String[]{ApkProvider.DataColumns.PACKAGE_NAME, ApkProvider.DataColumns.VERSION_CODE};
        List<Apk> existing = ApkProvider.Helper.find(context, repo, apps, projection);
        List<Apk> toDelete = new ArrayList<>();

        for (Apk existingApk : existing) {
            boolean shouldStay = false;

            if (packages.containsKey(existingApk.packageName)) {
                for (Apk newApk : packages.get(existingApk.packageName)) {
                    if (newApk.vercode == existingApk.vercode) {
                        shouldStay = true;
                        break;
                    }
                }
            }

            if (!shouldStay) {
                toDelete.add(existingApk);
            }
        }

        if (toDelete.size() == 0) {
            return null;
        }
        Uri uri = TempApkProvider.getApksUri(repo, toDelete);
        return ContentProviderOperation.newDelete(uri).build();
    }

    /**
     * This cannot be offloaded to the database (as we did with the query which
     * updates apps, depending on whether their apks are compatible or not).
     * The reason is that we need to interact with the CompatibilityChecker
     * in order to see if, and why an apk is not compatible.
     */
    private void calcApkCompatibilityFlags(List<Apk> apks) {
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

}
