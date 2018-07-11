package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.JobIntentService;
import android.util.Log;
import org.acra.ACRA;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Schema.InstalledAppTable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Handles all updates to {@link InstalledAppProvider}, whether checking the contents
 * versus what Android says is installed, or processing {@link Intent}s that come
 * from {@link android.content.BroadcastReceiver}s for {@link Intent#ACTION_PACKAGE_ADDED}
 * and {@link Intent#ACTION_PACKAGE_REMOVED}
 * <p/>
 * Since {@link android.content.ContentProvider#insert(Uri, ContentValues)} does not check
 * for duplicate records, it is entirely the job of this service to ensure that it is not
 * inserting duplicate versions of the same installed APK. On that note,
 * {@link #insertAppIntoDb(Context, PackageInfo, String, String)} and
 * {@link #deleteAppFromDb(Context, String)} are both static methods to enable easy testing
 * of this stuff.
 */
@SuppressWarnings("LineLength")
public class InstalledAppProviderService extends JobIntentService {
    private static final String TAG = "InstalledAppProviderSer";

    private static final String ACTION_INSERT = "org.fdroid.fdroid.data.action.INSERT";
    private static final String ACTION_DELETE = "org.fdroid.fdroid.data.action.DELETE";

    private static final String EXTRA_PACKAGE_INFO = "org.fdroid.fdroid.data.extra.PACKAGE_INFO";

    /**
     * This is for notifing the users of this {@link android.content.ContentProvider}
     * that the contents has changed.  Since {@link Intent}s can come in slow
     * or fast, and this can trigger a lot of UI updates, the actual
     * notifications are rate limited to one per second.
     */
    private PublishSubject<String> packageChangeNotifier;

    @Override
    public void onCreate() {
        super.onCreate();
        packageChangeNotifier = PublishSubject.create();

        // This "debounced" event will queue up any number of invocations within one second, and
        // only emit an event to the subscriber after it has not received any new events for one second.
        // This ensures that we don't constantly ask our lists of apps to update as we iterate over
        // the list of installed apps and insert them to the database...
        packageChangeNotifier
                .subscribeOn(Schedulers.newThread())
                .debounce(3, TimeUnit.SECONDS)
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String packageName) {
                        Utils.debugLog(TAG, "Notifying content providers (so they can update the relevant views).");
                        getContentResolver().notifyChange(AppProvider.getContentUri(), null);
                        getContentResolver().notifyChange(ApkProvider.getContentUri(), null);
                    }
                });

        // ...alternatively, this non-debounced version will instantly emit an event about the
        // particular package being updated. This is required so that our AppDetails view can update
        // itself immediately in response to an app being installed/upgraded/removed.
        // It does this _without_ triggering the main lists to update themselves, because they listen
        // only for changes to specific URIs in the AppProvider. These are triggered when a more
        // general notification (e.g. to AppProvider.getContentUri()) is fired, but not when a
        // sibling such as AppProvider.getHighestPriorityMetadataUri() is fired.
        packageChangeNotifier.subscribeOn(Schedulers.newThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String packageName) {
                        getContentResolver()
                                .notifyChange(AppProvider.getHighestPriorityMetadataUri(packageName), null);
                    }
                });
    }

    /**
     * Inserts an app into {@link InstalledAppProvider} based on a {@code package:} {@link Uri}.
     * This has no checks for whether it is inserting an exact duplicate, whatever is provided
     * will be inserted.
     */
    public static void insert(Context context, PackageInfo packageInfo) {
        insert(context, Utils.getPackageUri(packageInfo.packageName), packageInfo);
    }

    /**
     * Inserts an app into {@link InstalledAppProvider} based on a {@code package:} {@link Uri}.
     * This has no checks for whether it is inserting an exact duplicate, whatever is provided
     * will be inserted.
     */
    public static void insert(Context context, Uri uri) {
        insert(context, uri, null);
    }

    private static void insert(Context context, Uri uri, PackageInfo packageInfo) {
        Intent intent = new Intent(context, InstalledAppProviderService.class);
        intent.setAction(ACTION_INSERT);
        intent.setData(uri);
        intent.putExtra(EXTRA_PACKAGE_INFO, packageInfo);
        enqueueWork(context, intent);
    }

    /**
     * Deletes an app from {@link InstalledAppProvider} based on a {@code package:} {@link Uri}
     */
    public static void delete(Context context, String packageName) {
        delete(context, Utils.getPackageUri(packageName));
    }

    /**
     * Deletes an app from {@link InstalledAppProvider} based on a {@code package:} {@link Uri}
     */
    public static void delete(Context context, Uri uri) {
        Intent intent = new Intent(context, InstalledAppProviderService.class);
        intent.setAction(ACTION_DELETE);
        intent.setData(uri);
        enqueueWork(context, intent);
    }

    private static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, InstalledAppProviderService.class, 0x192834, intent);
    }

    /**
     * Make sure that {@link InstalledAppProvider}, our database of installed apps,
     * is in sync with what the {@link PackageManager} tells us is installed. Once
     * completed, the relevant {@link android.content.ContentProvider}s will be
     * notified of any changes to installed statuses.  The packages are processed
     * in alphabetically order so that "{@code android}" is processed first.  That
     * is always present and signed by the system key, so it is the source of the
     * system key for comparing all packages.
     * <p>
     * The installed app cache could get out of sync, e.g. if F-Droid crashed/ or
     * ran out of battery half way through responding to {@link Intent#ACTION_PACKAGE_ADDED}.
     * This method returns immediately, and will continue to work in an
     * {@link JobIntentService}.  It doesn't really matter where we put this in the
     * bootstrap process, because it runs in its own thread, at the lowest priority:
     * {@link Process#THREAD_PRIORITY_LOWEST}.
     * <p>
     * APKs installed in {@code /system} will often have zeroed out timestamps, like
     * 2008-01-01 (ziptime) or 2009-01-01.  So instead anything older than 2010 every
     * time since we have no way to know whether an APK wasn't changed as part of an
     * OTA update.  An OTA update could change the APK without changing the
     * {@link PackageInfo#versionCode} or {@link PackageInfo#lastUpdateTime}.
     *
     * @see <a href="https://gitlab.com/fdroid/fdroidclient/issues/819>issue #819</a>
     */
    public static void compareToPackageManager(Context context) {
        Utils.debugLog(TAG, "Comparing package manager to our installed app cache.");
        Map<String, Long> cachedInfo = InstalledAppProvider.Helper.all(context);

        List<PackageInfo> packageInfoList = context.getPackageManager()
                .getInstalledPackages(PackageManager.GET_SIGNATURES);
        Collections.sort(packageInfoList, new Comparator<PackageInfo>() {
            @Override
            public int compare(PackageInfo o1, PackageInfo o2) {
                return o1.packageName.compareTo(o2.packageName);
            }
        });
        for (PackageInfo packageInfo : packageInfoList) {
            if (cachedInfo.containsKey(packageInfo.packageName)) {
                if (packageInfo.lastUpdateTime < 1262300400000L // 2010-01-01 00:00
                        || packageInfo.lastUpdateTime > cachedInfo.get(packageInfo.packageName)) {
                    insert(context, packageInfo);
                }
                cachedInfo.remove(packageInfo.packageName);
            } else {
                insert(context, packageInfo);
            }
        }

        for (String packageName : cachedInfo.keySet()) {
            delete(context, packageName);
        }
    }

    @Nullable
    public static File getPathToInstalledApk(PackageInfo packageInfo) {
        File apk = new File(packageInfo.applicationInfo.publicSourceDir);
        if (apk.isDirectory()) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".apk");
                }
            };
            File[] files = apk.listFiles(filter);
            if (files == null) {
                String msg = packageInfo.packageName + " sourceDir has no APKs: " + apk.getAbsolutePath();
                Utils.debugLog(TAG, msg);
                ACRA.getErrorReporter().handleException(new IllegalArgumentException(msg), false);
                return null;
            }
            apk = files[0];
        }

        return apk;
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

        String packageName = intent.getData().getSchemeSpecificPart();
        final String action = intent.getAction();
        if (ACTION_INSERT.equals(action)) {
            PackageInfo packageInfo = getPackageInfo(intent, packageName);
            if (packageInfo != null) {
                File apk = getPathToInstalledApk(packageInfo);
                if (apk == null) {
                    return;
                }
                if (apk.exists() && apk.canRead()) {
                    try {
                        String hashType = "sha256";
                        String hash = Utils.getBinaryHash(apk, hashType);
                        insertAppIntoDb(this, packageInfo, hashType, hash);
                    } catch (IllegalArgumentException e) {
                        Utils.debugLog(TAG, e.getMessage());
                        ACRA.getErrorReporter().handleException(e, false);
                        return;
                    }
                }
            }
        } else if (ACTION_DELETE.equals(action)) {
            deleteAppFromDb(this, packageName);
        }
        packageChangeNotifier.onNext(packageName);
    }

    /**
     * This class will either have received an intent from the {@link InstalledAppProviderService}
     * itself, while iterating over installed apps, or from a {@link Intent#ACTION_PACKAGE_ADDED}
     * broadcast. In the first case, it will already have a {@link PackageInfo} for us. However if
     * it is from the later case, we'll need to query the {@link PackageManager} ourselves to get
     * this info.
     * <p>
     * Can still return null, as there is potentially race conditions to do with uninstalling apps
     * such that querying the {@link PackageManager} for a given package may throw an exception.
     */
    @Nullable
    private PackageInfo getPackageInfo(Intent intent, String packageName) {
        PackageInfo packageInfo = intent.getParcelableExtra(EXTRA_PACKAGE_INFO);
        if (packageInfo != null) {
            return packageInfo;
        }

        try {
            return getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param hash Although the has could be calculated within this function, it is helpful to inject
     *             the hash so as to be able to use this method during testing. Otherwise, the
     *             hashing method will try to hash a non-existent .apk file and try to insert NULL
     *             into the database when under test.
     */
    static void insertAppIntoDb(Context context, PackageInfo packageInfo, String hashType, String hash) {
        Log.d(TAG, "insertAppIntoDb " + packageInfo.packageName);
        Uri uri = InstalledAppProvider.getContentUri();
        ContentValues contentValues = new ContentValues();
        contentValues.put(InstalledAppTable.Cols.Package.NAME, packageInfo.packageName);
        contentValues.put(InstalledAppTable.Cols.VERSION_CODE, packageInfo.versionCode);
        contentValues.put(InstalledAppTable.Cols.VERSION_NAME, packageInfo.versionName);
        contentValues.put(InstalledAppTable.Cols.APPLICATION_LABEL,
                InstalledAppProvider.getApplicationLabel(context, packageInfo.packageName));
        contentValues.put(InstalledAppTable.Cols.SIGNATURE, Utils.getPackageSig(packageInfo));
        contentValues.put(InstalledAppTable.Cols.LAST_UPDATE_TIME, packageInfo.lastUpdateTime);

        contentValues.put(InstalledAppTable.Cols.HASH_TYPE, hashType);
        contentValues.put(InstalledAppTable.Cols.HASH, hash);

        context.getContentResolver().insert(uri, contentValues);
    }

    static void deleteAppFromDb(Context context, String packageName) {
        Log.d(TAG, "deleteAppFromDb " + packageName);
        Uri uri = InstalledAppProvider.getAppUri(packageName);
        context.getContentResolver().delete(uri, null, null);
    }
}