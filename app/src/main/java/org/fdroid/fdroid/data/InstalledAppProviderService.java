package org.fdroid.fdroid.data;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Process;

import org.fdroid.fdroid.Utils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
 * {@link #insertAppIntoDb(Context, String, PackageInfo)} and
 * {@link #deleteAppFromDb(Context, String)} are both static methods to enable easy testing
 * of this stuff.
 */
public class InstalledAppProviderService extends IntentService {
    private static final String TAG = "InstalledAppProviderSer";

    private static final String ACTION_INSERT = "org.fdroid.fdroid.data.action.INSERT";
    private static final String ACTION_DELETE = "org.fdroid.fdroid.data.action.DELETE";

    private static final String EXTRA_PACKAGE_INFO = "org.fdroid.fdroid.data.extra.PACKAGE_INFO";

    private ScheduledExecutorService worker;
    private boolean notifyChangeNeedsSending;

    public InstalledAppProviderService() {
        super("InstalledAppProviderService");
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
        context.startService(intent);
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
        context.startService(intent);
    }

    /**
     * Make sure that {@link InstalledAppProvider}, our database of installed apps,
     * is in sync with what the {@link PackageManager} tells us is installed. Once
     * completed, the relevant {@link android.content.ContentProvider}s will be
     * notified of any changes to installed statuses.
     * <p/>
     * The installed app cache could get out of sync, e.g. if F-Droid crashed/ or
     * ran out of battery half way through responding to {@link Intent#ACTION_PACKAGE_ADDED}.
     * This method returns immediately, and will continue to work in an
     * {@link IntentService}.  It doesn't really matter where we put this in the
     * bootstrap process, because it runs in its own thread, at the lowest priority:
     * {@link Process#THREAD_PRIORITY_LOWEST}.
     */
    public static void compareToPackageManager(Context context) {
        Map<String, Long> cachedInfo = InstalledAppProvider.Helper.all(context);

        List<PackageInfo> packageInfoList = context.getPackageManager()
                .getInstalledPackages(PackageManager.GET_SIGNATURES);
        for (PackageInfo packageInfo : packageInfoList) {
            if (cachedInfo.containsKey(packageInfo.packageName)) {
                if (packageInfo.lastUpdateTime > cachedInfo.get(packageInfo.packageName)) {
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

    @Override
    protected void onHandleIntent(Intent intent) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
        if (intent != null) {
            String packageName = intent.getData().getSchemeSpecificPart();
            final String action = intent.getAction();
            if (ACTION_INSERT.equals(action)) {
                insertAppIntoDb(this, packageName, (PackageInfo) intent.getParcelableExtra(EXTRA_PACKAGE_INFO));
            } else if (ACTION_DELETE.equals(action)) {
                deleteAppFromDb(this, packageName);
            }
            notifyChange();
        }
    }

    static void insertAppIntoDb(Context context, String packageName, PackageInfo packageInfo) {
        if (packageInfo == null) {
            try {
                packageInfo = context.getPackageManager().getPackageInfo(packageName,
                        PackageManager.GET_SIGNATURES);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return;
            }
        }

        Uri uri = InstalledAppProvider.getContentUri();
        ContentValues contentValues = new ContentValues();
        contentValues.put(InstalledAppProvider.DataColumns.PACKAGE_NAME, packageInfo.packageName);
        contentValues.put(InstalledAppProvider.DataColumns.VERSION_CODE, packageInfo.versionCode);
        contentValues.put(InstalledAppProvider.DataColumns.VERSION_NAME, packageInfo.versionName);
        contentValues.put(InstalledAppProvider.DataColumns.APPLICATION_LABEL,
                InstalledAppProvider.getApplicationLabel(context, packageInfo.packageName));
        contentValues.put(InstalledAppProvider.DataColumns.SIGNATURE,
                InstalledAppProvider.getPackageSig(packageInfo));
        contentValues.put(InstalledAppProvider.DataColumns.LAST_UPDATE_TIME, packageInfo.lastUpdateTime);

        String hashType = "sha256";
        String hash = Utils.getBinaryHash(new File(packageInfo.applicationInfo.publicSourceDir), hashType);
        contentValues.put(InstalledAppProvider.DataColumns.HASH_TYPE, hashType);
        contentValues.put(InstalledAppProvider.DataColumns.HASH, hash);

        context.getContentResolver().insert(uri, contentValues);
    }

    static void deleteAppFromDb(Context context, String packageName) {
        Uri uri = InstalledAppProvider.getAppUri(packageName);
        context.getContentResolver().delete(uri, null, null);
    }

    /**
     * This notifies the users of this {@link android.content.ContentProvider}
     * that the contents has changed.  Since {@link Intent}s can come in slow
     * or fast, and this can trigger a lot of UI updates, the actual
     * notifications are rate limited to one per second.
     */
    private void notifyChange() {
        notifyChangeNeedsSending = true;
        Runnable task = new Runnable() {
            @Override
            public void run() {
                if (notifyChangeNeedsSending) {
                    Utils.debugLog(TAG, "Notifying content providers (so they can update the relevant views).");
                    getContentResolver().notifyChange(AppProvider.getContentUri(), null);
                    getContentResolver().notifyChange(ApkProvider.getContentUri(), null);
                    notifyChangeNeedsSending = false;
                } else {
                    worker.shutdown();
                    worker = null;
                }
            }
        };
        if (worker == null || worker.isShutdown()) {
            worker = Executors.newSingleThreadScheduledExecutor();
            worker.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
        }
    }
}