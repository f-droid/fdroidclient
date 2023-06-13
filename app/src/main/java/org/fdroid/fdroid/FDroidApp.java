/*
 * Copyright (C) 2010-2012  Ciaran Gultnieks, ciaran@ciarang.com
 * Copyright (C) 2013-2016  Peter Serwylo <peter@serwylo.com>
 * Copyright (C) 2014-2018  Hans-Christoph Steiner <hans@eds.org>
 * Copyright (C) 2015-2016  Daniel Martí <mvdan@mvdan.cc>
 * Copyright (c) 2018  Senecto Limited
 * Copyright (C) 2019 Michael Pöhn <michael.poehn@fsfe.org>
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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;

import com.bumptech.glide.Glide;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.DialogConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.apache.commons.net.util.SubnetUtils;
import org.fdroid.database.FDroidDatabase;
import org.fdroid.database.Repository;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.DBHelper;
import org.fdroid.fdroid.installer.ApkFileProvider;
import org.fdroid.fdroid.installer.InstallHistoryService;
import org.fdroid.fdroid.installer.SessionInstallManager;
import org.fdroid.fdroid.nearby.PublicSourceDirProvider;
import org.fdroid.fdroid.nearby.SDCardScannerService;
import org.fdroid.fdroid.nearby.WifiStateChangeService;
import org.fdroid.fdroid.net.ConnectivityMonitorService;
import org.fdroid.fdroid.panic.HidingManager;
import org.fdroid.fdroid.receiver.DeviceStorageReceiver;
import org.fdroid.fdroid.work.CleanCacheWorker;
import org.fdroid.index.IndexFormatVersion;
import org.fdroid.index.RepoManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.ByteBuffer;
import java.security.Security;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import info.guardianproject.netcipher.NetCipher;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FDroidApp extends Application implements androidx.work.Configuration.Provider {

    private static final String TAG = "FDroidApp";
    private static final String ACRA_ID = BuildConfig.APPLICATION_ID + ":acra";

    public static final String SYSTEM_DIR_NAME = Environment.getRootDirectory().getAbsolutePath();

    private static FDroidApp instance;
    @Nullable
    private static RepoManager repoManager;

    // for the local repo on this device, all static since there is only one
    public static volatile int port;
    public static volatile boolean generateNewPort;
    public static volatile String ipAddressString;
    public static volatile SubnetUtils.SubnetInfo subnetInfo;
    public static volatile String ssid;
    public static volatile String bssid;
    public static volatile Repository repo;

    public static volatile SessionInstallManager sessionInstallManager;

    public static volatile int networkState = ConnectivityMonitorService.FLAG_NET_UNAVAILABLE;

    public static final SubnetUtils.SubnetInfo UNSET_SUBNET_INFO = new SubnetUtils("0.0.0.0/32").getInfo();

    @Nullable
    public static volatile String queryString;

    private static final org.bouncycastle.jce.provider.BouncyCastleProvider BOUNCYCASTLE_PROVIDER;

    /**
     * The construction of this notification helper has side effects including listening and
     * responding to local broadcasts. It is kept as a reference on the app object here so that
     * it doesn't get GC'ed.
     */
    @SuppressWarnings({"FieldCanBeLocal", "unused", "PMD.SingularField"})
    private NotificationHelper notificationHelper;

    static {
        BOUNCYCASTLE_PROVIDER = new org.bouncycastle.jce.provider.BouncyCastleProvider();
        enableBouncyCastle();
    }

    /**
     * Apply pure black background in dark theme setting. Must be called in every activity's
     * {@link AppCompatActivity#onCreate()}, before super.onCreate().
     *
     * @param activity The activity to apply the setting.
     */
    public void applyPureBlackBackgroundInDarkTheme(AppCompatActivity activity) {
        final boolean isPureBlack = Preferences.get().isPureBlack();
        if (isPureBlack) {
            activity.setTheme(R.style.Theme_App_Black);
        }
    }

    public static void applyTheme() {
        Preferences.Theme curTheme = Preferences.get().getTheme();
        switch (curTheme) {
            case dark:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case light:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            default:
                // `Set by Battery Saver` for Q above (inclusive), `Use system default` for Q below
                // https://medium.com/androiddevelopers/appcompat-v23-2-daynight-d10f90c83e94
                if (Build.VERSION.SDK_INT <= 28) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                }
                break;
        }
    }

    public void setSecureWindow(AppCompatActivity activity) {
        if (Preferences.get().preventScreenshots()) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    /**
     * The built-in BouncyCastle was stripped down in {@link Build.VERSION_CODES#S}
     * so that {@code SHA1withRSA} and {@code SHA256withRSA} are no longer included.
     *
     * @see
     * <a href="https://gitlab.com/fdroid/fdroidclient/-/issues/2338">Nearby Swap Crash on Android 12: no such algorithm: SHA1WITHRSA for provider BC</a>
     */
    private static void enableBouncyCastle() {
        if (Build.VERSION.SDK_INT >= 31) {
            Security.removeProvider("BC");
        }
        Security.addProvider(BOUNCYCASTLE_PROVIDER);
    }

    /**
     * Initialize the settings needed to run a local swap repo. This should
     * only ever be called in {@link WifiStateChangeService.WifiInfoThread},
     * after the single init call in {@link FDroidApp#onCreate()}.  If there is
     * a port conflict on binding then {@code generateNewPort} will be set and
     * the whole discovery process will be restarted in {@link WifiStateChangeService}
     */
    public static void initWifiSettings() {
        if (generateNewPort) {
            port = new Random().nextInt(8888) + 1024;
            generateNewPort = false;
        } else {
            port = 8888;
        }
        ipAddressString = null;
        subnetInfo = UNSET_SUBNET_INFO;
        ssid = "";
        bssid = "";
        repo = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Languages.setLanguage(this);
        App.systemLocaleList = null;

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
            onLanguageChanged(getApplicationContext());
        }
        atStartTime.edit().putString(lastLocaleKey, currentLocale).apply();
    }

    public static void onLanguageChanged(Context context) {
        FDroidDatabase db = DBHelper.getDb(context);
        Single.fromCallable(() -> {
            long now = System.currentTimeMillis();
            LocaleListCompat locales =
                    ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration());
            db.afterLocalesChanged(locales);
            Log.d(TAG, "Updating DB locales took: " + (System.currentTimeMillis() - now) + "ms");
            return true;
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_BACKGROUND) {
            clearImageLoaderMemoryCache();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        clearImageLoaderMemoryCache();
    }

    private void clearImageLoaderMemoryCache() {
        Glide.get(getApplicationContext()).clearMemory();
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
            ACRA.init(this, new CoreConfigurationBuilder()
                    .withReportContent(
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
                            ReportField.STACK_TRACE
                    )
                    .withPluginConfigurations(
                            new MailSenderConfigurationBuilder()
                                    .withMailTo(BuildConfig.ACRA_REPORT_EMAIL)
                                    .build(),
                            new DialogConfigurationBuilder()
                                    .withResTheme(R.style.Theme_App)
                                    .withTitle(getString(R.string.crash_dialog_title))
                                    .withText(getString(R.string.crash_dialog_text))
                                    .withCommentPrompt(getString(R.string.crash_dialog_comment_prompt))
                                    .build()
                    )
            );
            if (isAcraProcess() || HidingManager.isHidden(this)) {
                return;
            }
        }

        // register broadcast receivers
        registerReceiver(new DeviceStorageReceiver(), new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW));
        WifiStateChangeService.registerReceiver(this);

        applyTheme();

        configureProxy(preferences);

        // If the user changes the preference to do with filtering anti-feature apps,
        // it is easier to just notify a change in the app provider,
        // so that the newly updated list will correctly filter relevant apps.
        preferences.registerAppsRequiringAntiFeaturesChangeListener(() -> {
            // TODO check if anything else needs updating/reloading
        });

        preferences.registerUnstableUpdatesChangeListener(() ->
                AppUpdateStatusManager.getInstance(FDroidApp.this).checkForUpdates());

        CleanCacheWorker.schedule(this);

        sessionInstallManager = new SessionInstallManager(getApplicationContext());
        notificationHelper = new NotificationHelper(getApplicationContext());

        if (preferences.isIndexNeverUpdated()) {
            preferences.setDefaultForDataOnlyConnection(this);
        }
        // force setting network state to ensure it is set before UpdateService checks it
        networkState = ConnectivityMonitorService.getNetworkState(this);
        ConnectivityMonitorService.registerAndStart(this);
        UpdateService.schedule(getApplicationContext());

        FDroidApp.initWifiSettings();
        WifiStateChangeService.start(this, null);
        // if the HTTPS pref changes, then update all affected things
        preferences.registerLocalRepoHttpsListeners(() -> WifiStateChangeService.start(getApplicationContext(),
                null));

        if (preferences.isKeepingInstallHistory()) {
            InstallHistoryService.register(this);
        }

        String packageName = getString(R.string.install_history_reader_packageName);
        String unset = getString(R.string.install_history_reader_packageName_UNSET);
        if (!TextUtils.equals(packageName, unset)) {
            int modeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            modeFlags |= Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
            grantUriPermission(packageName, InstallHistoryService.LOG_URI, modeFlags);
        }

        // if the underlying OS version has changed, then fully rebuild the database
        SharedPreferences atStartTime = getAtStartTimeSharedPreferences();
        if (Build.VERSION.SDK_INT != atStartTime.getInt("build-version", Build.VERSION.SDK_INT)) {
            UpdateService.forceUpdateRepo(this);
        }
        atStartTime.edit().putInt("build-version", Build.VERSION.SDK_INT).apply();

        final String queryStringKey = "http-downloader-query-string";
        if (preferences.sendVersionAndUUIDToServers()) {
            queryString = atStartTime.getString(queryStringKey, null);
            if (queryString == null) {
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
                queryString = builder.toString();
                atStartTime.edit().putString(queryStringKey, queryString).apply();
            }
        } else {
            atStartTime.edit().remove(queryStringKey).apply();
        }

        if (Preferences.get().isScanRemovableStorageEnabled()) {
            SDCardScannerService.scan(this);
        }
    }

    /**
     * Asks if the current process is "org.fdroid.fdroid:acra".
     * <p>
     * This is helpful for bailing out of the {@link FDroidApp#onCreate} method early, preventing
     * problems that arise from executing the code twice. This happens due to the `android:process`
     * statement in AndroidManifest.xml causes another process to be created to run ACRA.
     * This was causing lots of things to be
     * started/run twice including {@link CleanCacheWorker} and {@link WifiStateChangeService}.
     * <p>
     * Note that it is not perfect, because some devices seem to not provide a list of running app
     * processes when asked. In such situations, F-Droid may regress to the behaviour where some
     * services may run twice and thus cause weirdness or slowness. However that is probably better
     * for end users than experiencing a deterministic crash every time F-Droid is started.
     */
    private boolean isAcraProcess() {
        ActivityManager manager = ContextCompat.getSystemService(this, ActivityManager.class);
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

    private SharedPreferences getAtStartTimeSharedPreferences() {
        return getSharedPreferences("at-start-time", Context.MODE_PRIVATE);
    }

    public void sendViaBluetooth(AppCompatActivity activity, int resultCode, String packageName) {
        if (resultCode == AppCompatActivity.RESULT_CANCELED) {
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
            sendBt.setType(PublicSourceDirProvider.SHARE_APK_MIME_TYPE);
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

    /**
     * Put proxy settings (or Tor settings) globally into effect based on what's configured in Preferences.
     * <p>
     * Must be called on App startup and after every proxy configuration change.
     */
    public static void configureProxy(Preferences preferences) {
        if (preferences.isTorEnabled()) {
            NetCipher.useTor();
        } else if (preferences.isProxyEnabled()) {
            // TODO move createUnresolved to NetCipher itself once its proven
            InetSocketAddress isa = InetSocketAddress.createUnresolved(
                    preferences.getProxyHost(), preferences.getProxyPort());
            NetCipher.setProxy(new Proxy(Proxy.Type.HTTP, isa));
        } else {
            NetCipher.clearProxy();
        }
    }

    public static void checkStartTor(Context context, Preferences preferences) {
        if (preferences.isTorEnabled()) {
            OrbotHelper.requestStartTor(context);
        }
    }

    public static Repository createSwapRepo(String address, String certificate) {
        long now = System.currentTimeMillis();
        return new Repository(42L, address, now, IndexFormatVersion.ONE, certificate, 20001L, 42, now);
    }

    public static Context getInstance() {
        return instance;
    }

    public static RepoManager getRepoManager(Context context) {
        if (repoManager == null) repoManager = new RepoManager(DBHelper.getDb(context));
        return repoManager;
    }

    /**
     * Set up WorkManager on demand to avoid slowing down starts.
     *
     * @see CleanCacheWorker
     * @see org.fdroid.fdroid.work.FDroidMetricsWorker
     * @see org.fdroid.fdroid.work.UpdateWorker
     * @see <a href="https://developer.android.com/codelabs/android-adv-workmanager#3">example</a>
     */
    @NonNull
    @Override
    public androidx.work.Configuration getWorkManagerConfiguration() {
        if (BuildConfig.DEBUG) {
            return new androidx.work.Configuration.Builder()
                    .setMinimumLoggingLevel(Log.DEBUG)
                    .build();
        } else {
            return new androidx.work.Configuration.Builder()
                    .setMinimumLoggingLevel(Log.ERROR)
                    .build();
        }
    }
}
