/*
 * Copyright (C) 2010-2012  Ciaran Gultnieks, ciaran@ciarang.com
 * Copyright (C) 2013-2016  Peter Serwylo <peter@serwylo.com>
 * Copyright (C) 2014-2018  Hans-Christoph Steiner <hans@eds.org>
 * Copyright (C) 2015-2016  Daniel Martí <mvdan@mvdan.cc>
 * Copyright (c) 2018  Senecto Limited
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;
import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiskCache;
import com.nostra13.universalimageloader.core.DefaultConfigurationFactory;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.process.BitmapProcessor;
import info.guardianproject.netcipher.NetCipher;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.apache.commons.net.util.SubnetUtils;
import org.fdroid.fdroid.Preferences.ChangeListener;
import org.fdroid.fdroid.Preferences.Theme;
import org.fdroid.fdroid.compat.PRNGFixes;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.InstalledAppProviderService;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.installer.ApkFileProvider;
import org.fdroid.fdroid.installer.InstallHistoryService;
import org.fdroid.fdroid.net.ConnectivityMonitorService;
import org.fdroid.fdroid.net.HttpDownloader;
import org.fdroid.fdroid.net.ImageLoaderForUIL;
import org.fdroid.fdroid.net.WifiStateChangeService;
import org.fdroid.fdroid.views.hiding.HidingManager;

import javax.microedition.khronos.opengles.GL10;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.Security;
import java.util.List;
import java.util.UUID;

@ReportsCrashes(mailTo = "reports@f-droid.org",
        mode = ReportingInteractionMode.DIALOG,
        reportDialogClass = org.fdroid.fdroid.acra.CrashReportActivity.class,
        reportSenderFactoryClasses = org.fdroid.fdroid.acra.CrashReportSenderFactory.class,
        customReportContent = {
                ReportField.USER_COMMENT,
                ReportField.PACKAGE_NAME,
                ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION,
                ReportField.PRODUCT,
                ReportField.BRAND,
                ReportField.PHONE_MODEL,
                ReportField.DISPLAY,
                ReportField.TOTAL_MEM_SIZE,
                ReportField.AVAILABLE_MEM_SIZE,
                ReportField.CUSTOM_DATA,
                ReportField.STACK_TRACE_HASH,
                ReportField.STACK_TRACE,
        }
)
public class FDroidApp extends Application {

    private static final String TAG = "FDroidApp";
    private static final String ACRA_ID = BuildConfig.APPLICATION_ID + ":acra";

    public static final String SYSTEM_DIR_NAME = Environment.getRootDirectory().getAbsolutePath();

    private static FDroidApp instance;

    // for the local repo on this device, all static since there is only one
    public static volatile int port;
    public static volatile String ipAddressString;
    public static volatile SubnetUtils.SubnetInfo subnetInfo;
    public static volatile String ssid;
    public static volatile String bssid;
    public static volatile Repo repo = new Repo();

    public static volatile int networkState = ConnectivityMonitorService.FLAG_NET_UNAVAILABLE;

    public static final SubnetUtils.SubnetInfo UNSET_SUBNET_INFO = new SubnetUtils("0.0.0.0/32").getInfo();

    private static volatile LongSparseArray<String> lastWorkingMirrorArray = new LongSparseArray<>(1);
    private static volatile int numTries = Integer.MAX_VALUE;
    private static volatile int timeout = 10000;

    // Leaving the fully qualified class name here to help clarify the difference between spongy/bouncy castle.
    private static final org.bouncycastle.jce.provider.BouncyCastleProvider BOUNCYCASTLE_PROVIDER;

    @SuppressWarnings("unused")
    BluetoothAdapter bluetoothAdapter;

    /**
     * The construction of this notification helper has side effects including listening and
     * responding to local broadcasts. It is kept as a reference on the app object here so that
     * it doesn't get GC'ed.
     */
    @SuppressWarnings("unused")
    NotificationHelper notificationHelper;

    static {
        BOUNCYCASTLE_PROVIDER = new org.bouncycastle.jce.provider.BouncyCastleProvider();
        enableBouncyCastle();
    }

    private static Theme curTheme = Theme.light;

    public void reloadTheme() {
        curTheme = Preferences.get().getTheme();
    }

    public void applyTheme(Activity activity) {
        activity.setTheme(getCurThemeResId());
        setSecureWindow(activity);
    }

    public static int getCurThemeResId() {
        switch (curTheme) {
            case light:
                return R.style.AppThemeLight;
            case dark:
                return R.style.AppThemeDark;
            case night:
                return R.style.AppThemeNight;
            default:
                return R.style.AppThemeLight;
        }
    }

    public static boolean isAppThemeLight() {
        return curTheme == Theme.light;
    }

    public void applyDialogTheme(Activity activity) {
        activity.setTheme(getCurDialogThemeResId());
        setSecureWindow(activity);
    }

    public void setSecureWindow(Activity activity) {
        if (Preferences.get().preventScreenshots()) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private static int getCurDialogThemeResId() {
        switch (curTheme) {
            case light:
                return R.style.MinWithDialogBaseThemeLight;
            case dark:
                return R.style.MinWithDialogBaseThemeDark;
            case night:
                return R.style.MinWithDialogBaseThemeDark;
            default:
                return R.style.MinWithDialogBaseThemeLight;
        }
    }

    /**
     * Force reload the {@link Activity to make theme changes take effect.}
     * Same as {@link Languages#forceChangeLanguage(Activity)}
     *
     * @param activity the {@code Activity} to force reload
     */
    public static void forceChangeTheme(Activity activity) {
        Intent intent = activity.getIntent();
        if (intent == null) { // when launched as LAUNCHER
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.finish();
        activity.overridePendingTransition(0, 0);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    public static void enableBouncyCastle() {
        Security.addProvider(BOUNCYCASTLE_PROVIDER);
    }

    public static void enableBouncyCastleOnLollipop() {
        if (Build.VERSION.SDK_INT == 21) {
            Security.addProvider(BOUNCYCASTLE_PROVIDER);
        }
    }

    public static void disableBouncyCastleOnLollipop() {
        if (Build.VERSION.SDK_INT == 21) {
            Security.removeProvider(BOUNCYCASTLE_PROVIDER.getName());
        }
    }

    /**
     * Initialize the settings needed to run a local swap repo. This should
     * only ever be called in {@link org.fdroid.fdroid.net.WifiStateChangeService.WifiInfoThread},
     * after the single init call in {@link FDroidApp#onCreate()}.
     */
    public static void initWifiSettings() {
        port = 8888;
        ipAddressString = null;
        subnetInfo = UNSET_SUBNET_INFO;
        ssid = "";
        bssid = "";
        repo = new Repo();
    }

    public static String getMirror(String urlString, long repoId) throws IOException {
        return getMirror(urlString, RepoProvider.Helper.findById(getInstance(), repoId));
    }

    public static String getMirror(String urlString, Repo repo2) throws IOException {
        if (repo2.hasMirrors()) {
            String lastWorkingMirror = lastWorkingMirrorArray.get(repo2.getId());
            if (lastWorkingMirror == null) {
                lastWorkingMirror = repo2.address;
            }
            if (numTries <= 0) {
                if (timeout == 10000) {
                    timeout = 30000;
                    numTries = Integer.MAX_VALUE;
                } else if (timeout == 30000) {
                    timeout = 60000;
                    numTries = Integer.MAX_VALUE;
                } else {
                    Utils.debugLog(TAG, "Mirrors: Giving up");
                    throw new IOException("Ran out of mirrors");
                }
            }
            if (numTries == Integer.MAX_VALUE) {
                numTries = repo2.getMirrorCount();
            }
            String mirror = repo2.getMirror(lastWorkingMirror);
            String newUrl = urlString.replace(lastWorkingMirror, mirror);
            Utils.debugLog(TAG, "Trying mirror " + mirror + " after " + lastWorkingMirror + " failed," +
                    " timeout=" + timeout / 1000 + "s");
            lastWorkingMirrorArray.put(repo2.getId(), mirror);
            numTries--;
            return newUrl;
        } else {
            throw new IOException("No mirrors available");
        }
    }

    public static int getTimeout() {
        return timeout;
    }

    public static void resetMirrorVars() {
        // Reset last working mirror, numtries, and timeout
        for (int i = 0; i < lastWorkingMirrorArray.size(); i++) {
            lastWorkingMirrorArray.removeAt(i);
        }
        numTries = Integer.MAX_VALUE;
        timeout = 10000;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Languages.setLanguage(this);

        // update the descriptions based on the new language preferences
        SharedPreferences atStartTime = getAtStartTimeSharedPreferences();
        final String lastLocaleKey = "lastLocale";
        String lastLocale = atStartTime.getString(lastLocaleKey, null);
        String currentLocale;
        if (Build.VERSION.SDK_INT < 24) {
            currentLocale = newConfig.locale.toString();
        } else {
            currentLocale = newConfig.getLocales().toString();
        }
        if (!TextUtils.equals(lastLocale, currentLocale)) {
            UpdateService.forceUpdateRepo(this);
        }
        atStartTime.edit().putString(lastLocaleKey, currentLocale).apply();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_BACKGROUND) {
            ImageLoader.getInstance().clearMemoryCache();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ImageLoader.getInstance().clearMemoryCache();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
        Preferences.setup(this);
        Languages.setLanguage(this);
        Preferences preferences = Preferences.get();

        if (preferences.promptToSendCrashReports()) {
            ACRA.init(this);
            if (isAcraProcess() || HidingManager.isHidden(this)) {
                return;
            }
        }

        PRNGFixes.apply();

        curTheme = preferences.getTheme();
        preferences.configureProxy();

        // bug specific to exactly 5.0 makes it only work with the old index
        // which includes an ugly, hacky workaround
        // https://gitlab.com/fdroid/fdroidclient/issues/1014
        if (Build.VERSION.SDK_INT == 21) {
            preferences.setExpertMode(true);
            preferences.setForceOldIndex(true);
        }

        InstalledAppProviderService.compareToPackageManager(this);

        // If the user changes the preference to do with filtering anti-feature apps,
        // it is easier to just notify a change in the app provider,
        // so that the newly updated list will correctly filter relevant apps.
        preferences.registerAppsRequiringAntiFeaturesChangeListener(new Preferences.ChangeListener() {
            @Override
            public void onPreferenceChange() {
                getContentResolver().notifyChange(AppProvider.getContentUri(), null);
            }
        });

        preferences.registerUnstableUpdatesChangeListener(new Preferences.ChangeListener() {
            @Override
            public void onPreferenceChange() {
                AppProvider.Helper.calcSuggestedApks(FDroidApp.this);
            }
        });

        CleanCacheService.schedule(this);

        notificationHelper = new NotificationHelper(getApplicationContext());
        bluetoothAdapter = getBluetoothAdapter();

        // There are a couple things to pay attention to with this config: memory usage,
        // especially on small devices; and, image processing vulns, since images are
        // submitted via app's git repos, so anyone with commit privs there could submit
        // exploits hidden in images.  Luckily, F-Droid doesn't need EXIF at all, and
        // that is where the JPEG/PNG vulns have been. So it can be entirely stripped.
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        int maxSize = GL10.GL_MAX_TEXTURE_SIZE; // see ImageScaleType.NONE_SAFE javadoc
        int width = display.getWidth();
        if (width > maxSize) {
            maxSize = width;
        }
        int height = display.getHeight();
        if (height > maxSize) {
            maxSize = height;
        }

        DiskCache diskCache;
        long available = Utils.getImageCacheDirAvailableMemory(this);
        int percentageFree = Utils.getPercent(available, Utils.getImageCacheDirTotalMemory(this));
        if (percentageFree > 5) {
            diskCache = new UnlimitedDiskCache(Utils.getImageCacheDir(this));
        } else {
            Log.i(TAG, "Switching to LruDiskCache(" + available / 2L + ") to save disk space!");
            try {
                diskCache = new LruDiskCache(Utils.getImageCacheDir(this),
                        DefaultConfigurationFactory.createFileNameGenerator(),
                        available / 2L);
            } catch (IOException e) {
                diskCache = new UnlimitedDiskCache(Utils.getImageCacheDir(this));
            }
        }
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .imageDownloader(new ImageLoaderForUIL(getApplicationContext()))
                .defaultDisplayImageOptions(Utils.getDefaultDisplayImageOptionsBuilder().build())
                .diskCache(diskCache)
                .diskCacheExtraOptions(maxSize, maxSize, new BitmapProcessor() {
                    @Override
                    public Bitmap process(Bitmap bitmap) {
                        // converting JPEGs to Bitmaps, then saving them removes EXIF metadata
                        return bitmap;
                    }
                })
                .threadPoolSize(getThreadPoolSize())
                .build();
        ImageLoader.getInstance().init(config);

        if (preferences.isIndexNeverUpdated()) {
            // force this check to ensure it starts fetching the index on initial runs
            networkState = ConnectivityMonitorService.getNetworkState(this);
        }
        ConnectivityMonitorService.registerAndStart(this);
        UpdateService.schedule(getApplicationContext());

        FDroidApp.initWifiSettings();
        WifiStateChangeService.start(this, null);
        // if the HTTPS pref changes, then update all affected things
        preferences.registerLocalRepoHttpsListeners(new ChangeListener() {
            @Override
            public void onPreferenceChange() {
                WifiStateChangeService.start(getApplicationContext(), null);
            }
        });

        configureTor(preferences.isTorEnabled());

        if (preferences.isKeepingInstallHistory()) {
            InstallHistoryService.register(this);
        }

        String packageName = getString(R.string.install_history_reader_packageName);
        String unset = getString(R.string.install_history_reader_packageName_UNSET);
        if (!TextUtils.equals(packageName, unset)) {
            int modeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            if (Build.VERSION.SDK_INT >= 19) {
                modeFlags |= Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
            }
            grantUriPermission(packageName, InstallHistoryService.LOG_URI, modeFlags);
        }

        // find and process provisions if any.
        Provisioner.scanAndProcess(getApplicationContext());

        // if the underlying OS version has changed, then fully rebuild the database
        SharedPreferences atStartTime = getAtStartTimeSharedPreferences();
        if (Build.VERSION.SDK_INT != atStartTime.getInt("build-version", Build.VERSION.SDK_INT)) {
            UpdateService.forceUpdateRepo(this);
        }
        atStartTime.edit().putInt("build-version", Build.VERSION.SDK_INT).apply();

        final String queryStringKey = "http-downloader-query-string";
        if (preferences.sendVersionAndUUIDToServers()) {
            HttpDownloader.queryString = atStartTime.getString(queryStringKey, null);
            if (HttpDownloader.queryString == null) {
                UUID uuid = UUID.randomUUID();
                ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE * 2);
                buffer.putLong(uuid.getMostSignificantBits());
                buffer.putLong(uuid.getLeastSignificantBits());
                String id = Base64.encodeToString(buffer.array(),
                        Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
                StringBuilder builder = new StringBuilder("id=").append(id);
                String versionName = Uri.encode(Utils.getVersionName(this));
                if (versionName != null) {
                    builder.append("&client_version=").append(versionName);
                }
                HttpDownloader.queryString = builder.toString();
            }
            if (!atStartTime.contains(queryStringKey)) {
                atStartTime.edit().putString(queryStringKey, HttpDownloader.queryString).apply();
            }
        } else {
            atStartTime.edit().remove(queryStringKey).apply();
        }
    }

    /**
     * Asks if the current process is "org.fdroid.fdroid:acra".
     * <p>
     * This is helpful for bailing out of the {@link FDroidApp#onCreate} method early, preventing
     * problems that arise from executing the code twice. This happens due to the `android:process`
     * statement in AndroidManifest.xml causes another process to be created to run
     * {@link org.fdroid.fdroid.acra.CrashReportActivity}. This was causing lots of things to be
     * started/run twice including {@link CleanCacheService} and {@link WifiStateChangeService}.
     * <p>
     * Note that it is not perfect, because some devices seem to not provide a list of running app
     * processes when asked. In such situations, F-Droid may regress to the behaviour where some
     * services may run twice and thus cause weirdness or slowness. However that is probably better
     * for end users than experiencing a deterministic crash every time F-Droid is started.
     */
    private boolean isAcraProcess() {
        ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
        if (processes == null) {
            return false;
        }

        int pid = android.os.Process.myPid();
        for (RunningAppProcessInfo processInfo : processes) {
            if (processInfo.pid == pid && ACRA_ID.equals(processInfo.processName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return the number of threads Universal Image Loader should use, based on
     * the total RAM in the device.  Devices with lots of RAM can do lots of
     * parallel operations for fast icon loading.
     */
    @TargetApi(16)
    private int getThreadPoolSize() {
        if (Build.VERSION.SDK_INT >= 16) {
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            if (activityManager != null) {
                activityManager.getMemoryInfo(memInfo);
                return (int) Math.max(1, Math.min(16, memInfo.totalMem / 256 / 1024 / 1024));
            }
        }
        return 2;
    }

    @TargetApi(18)
    private BluetoothAdapter getBluetoothAdapter() {
        // to use the new, recommended way of getting the adapter
        // http://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html
        if (Build.VERSION.SDK_INT < 18) {
            return BluetoothAdapter.getDefaultAdapter();
        }
        return ((BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getAdapter();
    }

    private SharedPreferences getAtStartTimeSharedPreferences() {
        return getSharedPreferences("at-start-time", Context.MODE_PRIVATE);
    }

    public void sendViaBluetooth(Activity activity, int resultCode, String packageName) {
        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }

        String bluetoothPackageName = null;
        String className = null;
        Intent sendBt = null;

        try {
            PackageManager pm = getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            sendBt = new Intent(Intent.ACTION_SEND);

            // The APK type ("application/vnd.android.package-archive") is blocked by stock Android, so use zip
            sendBt.setType("application/zip");
            sendBt.putExtra(Intent.EXTRA_STREAM, ApkFileProvider.getSafeUri(this, packageInfo));

            // not all devices have the same Bluetooth Activities, so
            // let's find it
            for (ResolveInfo info : pm.queryIntentActivities(sendBt, 0)) {
                bluetoothPackageName = info.activityInfo.packageName;
                if ("com.android.bluetooth".equals(bluetoothPackageName)
                        || "com.mediatek.bluetooth".equals(bluetoothPackageName)) {
                    className = info.activityInfo.name;
                    break;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not get application info to send via bluetooth", e);
            className = null;
        } catch (IOException e) {
            Exception toLog = new RuntimeException("Error preparing file to send via Bluetooth", e);
            ACRA.getErrorReporter().handleException(toLog, false);
        }

        if (sendBt != null) {
            if (className != null) {
                sendBt.setClassName(bluetoothPackageName, className);
                activity.startActivity(sendBt);
            } else {
                Toast.makeText(this, R.string.bluetooth_activity_not_found,
                        Toast.LENGTH_SHORT).show();
                activity.startActivity(Intent.createChooser(sendBt, getString(R.string.choose_bt_send)));
            }
        }
    }

    private static boolean useTor;

    /**
     * Set the proxy settings based on whether Tor should be enabled or not.
     */
    private static void configureTor(boolean enabled) {
        useTor = enabled;
        if (useTor) {
            NetCipher.useTor();
        } else {
            NetCipher.clearProxy();
        }
    }

    public static void checkStartTor(Context context) {
        if (useTor) {
            OrbotHelper.requestStartTor(context);
        }
    }

    public static boolean isUsingTor() {
        return useTor;
    }

    public static Context getInstance() {
        return instance;
    }
}
