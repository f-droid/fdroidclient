package org.fdroid.fdroid.data;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;

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

    public void commit(ContentValues repoDetailsToSave, long repoIdToCommit) throws RepoUpdater.UpdateException {
        flushBufferToDb();
        TempAppProvider.Helper.commitAppsAndApks(context, repoIdToCommit);
        RepoProvider.Helper.update(context, repo, repoDetailsToSave);
    }

    private void flushBufferToDb() throws RepoUpdater.UpdateException {
        if (!hasBeenInitialized) {
            // This is where we will store all of the metadata before commiting at the
            // end of the process. This is due to the fact that we can't verify the cert
            // the index was signed with until we've finished reading it - and we don't
            // want to put stuff in the real database until we are sure it is from a
            // trusted source. It also helps performance as it is done via an in-memory database.
            TempAppProvider.Helper.init(context, repo.getId());
            hasBeenInitialized = true;
        }

        if (apksToSave.size() > 0 || appsToSave.size() > 0) {
            Utils.debugLog(TAG, "Flushing details of up to " + MAX_APP_BUFFER + " apps/packages to the database.");
            Map<String, Long> appIds = flushAppsToDbInBatch();
            flushApksToDbInBatch(appIds);
            apksToSave.clear();
            appsToSave.clear();
        }
    }

    private void flushApksToDbInBatch(Map<String, Long> appIds) throws RepoUpdater.UpdateException {
        List<Apk> apksToSaveList = new ArrayList<>();
        for (Map.Entry<String, List<Apk>> entries : apksToSave.entrySet()) {
            for (Apk apk : entries.getValue()) {
                apk.appId = appIds.get(apk.packageName);
            }
            apksToSaveList.addAll(entries.getValue());
        }

        calcApkCompatibilityFlags(apksToSaveList);

        ArrayList<ContentProviderOperation> apkOperations = insertApks(apksToSaveList);

        try {
            context.getContentResolver().applyBatch(TempApkProvider.getAuthority(), apkOperations);
        } catch (RemoteException | OperationApplicationException e) {
            throw new RepoUpdater.UpdateException("An internal error occurred while updating the database", e);
        }
    }

    /**
     * Will first insert new or update existing rows in the database for each {@link RepoPersister#appsToSave}.
     * Then, will query the database for the ID + packageName for each of these apps, so that they
     * can be returned and the relevant apks can be joined to the app table correctly.
     */
    private Map<String, Long> flushAppsToDbInBatch() throws RepoUpdater.UpdateException {
        ArrayList<ContentProviderOperation> appOperations = insertApps(appsToSave);

        try {
            context.getContentResolver().applyBatch(TempAppProvider.getAuthority(), appOperations);
            return getIdsForPackages(appsToSave);
        } catch (RemoteException | OperationApplicationException e) {
            throw new RepoUpdater.UpdateException("An internal error occurred while updating the database", e);
        }
    }

    /**
     * Although this might seem counter intuitive - receiving a list of apps, then querying the
     * database again for info about these apps, it is required because the apps came from the
     * repo metadata, but we are really interested in their IDs from the database. These IDs only
     * exist in SQLite and not the repo metadata.
     */
    private Map<String, Long> getIdsForPackages(List<App> apps) {
        List<String> packageNames = new ArrayList<>(appsToSave.size());
        for (App app : apps) {
            packageNames.add(app.packageName);
        }

        String[] projection = {
                Schema.AppMetadataTable.Cols.ROW_ID,
                Schema.AppMetadataTable.Cols.Package.PACKAGE_NAME,
        };

        List<App> fromDb = TempAppProvider.Helper.findByPackageNames(context, packageNames, repo.id, projection);

        Map<String, Long> ids = new HashMap<>(fromDb.size());
        for (App app : fromDb) {
            ids.put(app.packageName, app.getId());
        }
        return ids;
    }

    private ArrayList<ContentProviderOperation> insertApps(List<App> apps) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>(apps.size());
        for (App app : apps) {
            ContentValues values = app.toContentValues();
            Uri uri = TempAppProvider.getContentUri();
            operations.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
        }
        return operations;
    }

    private ArrayList<ContentProviderOperation> insertApks(List<Apk> packages) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>(packages.size());
        for (Apk apk : packages) {
            ContentValues values = apk.toContentValues();
            Uri uri = TempApkProvider.getContentUri();
            operations.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
        }

        return operations;
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
                apk.incompatibleReasons = reasons.toArray(new String[reasons.size()]);
            } else {
                apk.compatible = true;
                apk.incompatibleReasons = null;
            }
        }
    }

}
