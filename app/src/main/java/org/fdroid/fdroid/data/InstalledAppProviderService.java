package org.fdroid.fdroid.data;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Process;

import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.Utils;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

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

    /**
     * This is for notifing the users of this {@link android.content.ContentProvider}
     * that the contents has changed.  Since {@link Intent}s can come in slow
     * or fast, and this can trigger a lot of UI updates, the actual
     * notifications are rate limited to one per second.
     */
    private PublishSubject<Void> notifyEvents;

    public InstalledAppProviderService() {
        super("InstalledAppProviderService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notifyEvents = PublishSubject.create();
        notifyEvents.debounce(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Action1<Void>() {
                        @Override
                        public void call(Void voidArg) {
                            Utils.debugLog(TAG, "Notifying content providers (so they can update the relevant views).");
                            getContentResolver().notifyChange(AppProvider.getContentUri(), null);
                            getContentResolver().notifyChange(ApkProvider.getContentUri(), null);
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
                PackageInfo packageInfo = intent.getParcelableExtra(EXTRA_PACKAGE_INFO);
                String hashType = "sha256";
                String hash = Utils.getBinaryHash(new File(packageInfo.applicationInfo.publicSourceDir), hashType);
                insertAppIntoDb(this, packageName, packageInfo, hashType, hash);
            } else if (ACTION_DELETE.equals(action)) {
                deleteAppFromDb(this, packageName);
            }
            notifyEvents.onNext(null);
        }
    }

    /**
     * @param hash Although the has could be calculated within this function, it is helpful to inject
     *             the hash so as to be able to use this method during testing. Otherwise, the
     *             hashing method will try to hash a non-existent .apk file and try to insert NULL
     *             into the database when under test.
     */
    static void insertAppIntoDb(Context context, String packageName, PackageInfo packageInfo, String hashType, String hash) {
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
        contentValues.put(InstalledAppProvider.DataColumns.SIGNATURE, getPackageSig(packageInfo));
        contentValues.put(InstalledAppProvider.DataColumns.LAST_UPDATE_TIME, packageInfo.lastUpdateTime);

        contentValues.put(InstalledAppProvider.DataColumns.HASH_TYPE, hashType);
        contentValues.put(InstalledAppProvider.DataColumns.HASH, hash);

        context.getContentResolver().insert(uri, contentValues);
    }

    static void deleteAppFromDb(Context context, String packageName) {
        Uri uri = InstalledAppProvider.getAppUri(packageName);
        context.getContentResolver().delete(uri, null, null);
    }

    private static String getPackageSig(PackageInfo info) {
        if (info == null || info.signatures == null || info.signatures.length < 1) {
            return "";
        }
        Signature sig = info.signatures[0];
        String sigHash = "";
        try {
            Hasher hash = new Hasher("MD5", sig.toCharsString().getBytes());
            sigHash = hash.getHash();
        } catch (NoSuchAlgorithmException e) {
            // ignore
        }
        return sigHash;
    }

}