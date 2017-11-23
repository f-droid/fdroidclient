/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.nostra13.universalimageloader.cache.disc.impl.LimitedAgeDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.acra.ACRA;
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
import org.fdroid.fdroid.data.SanitizedFile;
import org.fdroid.fdroid.installer.ApkFileProvider;
import org.fdroid.fdroid.installer.InstallHistoryService;
import org.fdroid.fdroid.net.ImageLoaderForUIL;
import org.fdroid.fdroid.net.WifiStateChangeService;

import java.io.IOException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.Security;
import java.util.List;

import info.guardianproject.netcipher.NetCipher;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import sun.net.www.protocol.bluetooth.Handler;

@ReportsCrashes(mailTo = "reports@f-droid.org",
        mode = ReportingInteractionMode.DIALOG,
        reportDialogClass = org.fdroid.fdroid.acra.CrashReportActivity.class,
        reportSenderFactoryClasses = org.fdroid.fdroid.acra.CrashReportSenderFactory.class
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

    private static volatile String lastWorkingMirror = null;
    private static volatile int numTries = Integer.MAX_VALUE;
    private static volatile int timeout = 10000;

    // Leaving the fully qualified class name here to help clarify the difference between spongy/bouncy castle.
    private static final org.spongycastle.jce.provider.BouncyCastleProvider SPONGYCASTLE_PROVIDER;

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
        SPONGYCASTLE_PROVIDER = new org.spongycastle.jce.provider.BouncyCastleProvider();
        enableSpongyCastle();
    }

    private static Theme curTheme = Theme.light;

    public void reloadTheme() {
        curTheme = Preferences.get().getTheme();
    }

    public void applyTheme(Activity activity) {
        activity.setTheme(getCurThemeResId());
        if (Preferences.get().preventScreenshots()) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
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

    public void applyDialogTheme(Activity activity) {
        activity.setTheme(getCurDialogThemeResId());
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

    public static void enableSpongyCastle() {
        Security.addProvider(SPONGYCASTLE_PROVIDER);
    }

    public static void enableSpongyCastleOnLollipop() {
        if (Build.VERSION.SDK_INT == 21) {
            Security.addProvider(SPONGYCASTLE_PROVIDER);
        }
    }

    public static void disableSpongyCastleOnLollipop() {
        if (Build.VERSION.SDK_INT == 21) {
            Security.removeProvider(SPONGYCASTLE_PROVIDER.getName());
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
        subnetInfo = new SubnetUtils("0.0.0.0/32").getInfo();
        ssid = "";
        bssid = "";
        repo = new Repo();
    }

    public static String getMirror(String urlString, long repoId) throws IOException {
        return getMirror(urlString, RepoProvider.Helper.findById(getInstance(), repoId));
    }

    public static String getMirror(String urlString, Repo repo2) throws IOException {
        if (repo2.hasMirrors()) {
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
            lastWorkingMirror = mirror;
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
        lastWorkingMirror = null;
        numTries = Integer.MAX_VALUE;
        timeout = 10000;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Languages.setLanguage(this);
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

        ACRA.init(this);
        if (isAcraProcess()) {
            return;
        }

        PRNGFixes.apply();

        curTheme = Preferences.get().getTheme();
        Preferences.get().configureProxy();

        // bug specific to exactly 5.0 makes it only work with the old index
        // which includes an ugly, hacky workaround
        // https://gitlab.com/fdroid/fdroidclient/issues/1014
        if (Build.VERSION.SDK_INT == 21) {
            Preferences p = Preferences.get();
            p.setExpertMode(true);
            p.setForceOldIndex(true);
        }

        InstalledAppProviderService.compareToPackageManager(this);
        AppUpdateStatusService.scanDownloadedApks(this);

        // If the user changes the preference to do with filtering rooted apps,
        // it is easier to just notify a change in the app provider,
        // so that the newly updated list will correctly filter relevant apps.
        Preferences.get().registerAppsRequiringRootChangeListener(new Preferences.ChangeListener() {
            @Override
            public void onPreferenceChange() {
                getContentResolver().notifyChange(AppProvider.getContentUri(), null);
            }
        });

        // If the user changes the preference to do with filtering anti-feature apps,
        // it is easier to just notify a change in the app provider,
        // so that the newly updated list will correctly filter relevant apps.
        Preferences.get().registerAppsRequiringAntiFeaturesChangeListener(new Preferences.ChangeListener() {
            @Override
            public void onPreferenceChange() {
                getContentResolver().notifyChange(AppProvider.getContentUri(), null);
            }
        });

        // This is added so that the bluetooth:// scheme we use for URLs the BluetoothDownloader
        // understands is not treated as invalid by the java.net.URL class. The actual Handler does
        // nothing, but its presence is enough.
        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
            @Override
            public URLStreamHandler createURLStreamHandler(String protocol) {
                return TextUtils.equals(protocol, "bluetooth") ? new Handler() : null;
            }
        });

        final Context context = this;
        Preferences.get().registerUnstableUpdatesChangeListener(new Preferences.ChangeListener() {
            @Override
            public void onPreferenceChange() {
                AppProvider.Helper.calcSuggestedApks(context);
            }
        });

        CleanCacheService.schedule(this);

        notificationHelper = new NotificationHelper(getApplicationContext());
        UpdateService.schedule(getApplicationContext());
        bluetoothAdapter = getBluetoothAdapter();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .imageDownloader(new ImageLoaderForUIL(getApplicationContext()))
                .diskCache(new LimitedAgeDiskCache(
                        Utils.getImageCacheDir(this),
                        null,
                        new FileNameGenerator() {
                            @NonNull
                            @Override
                            public String generate(String imageUri) {
                                if (TextUtils.isEmpty(imageUri)) {
                                    return "null";
                                }

                                String fileNameToSanitize;
                                Uri uri = Uri.parse(imageUri);
                                if (TextUtils.isEmpty(uri.getPath())) {
                                    // files with URL like "drawable://213083835209" used by the category backgrounds
                                    fileNameToSanitize = imageUri.replaceAll("[:/]", "");
                                } else {
                                    fileNameToSanitize = uri.getPath().replace("/", "-");
                                }

                                return SanitizedFile.sanitizeFileName(fileNameToSanitize);
                            }
                        },
                        // 30 days in secs: 30*24*60*60 = 2592000
                        2592000)
                )
                .threadPoolSize(4)
                .threadPriority(Thread.NORM_PRIORITY - 2) // Default is NORM_PRIORITY - 1
                .build();
        ImageLoader.getInstance().init(config);

        FDroidApp.initWifiSettings();
        startService(new Intent(this, WifiStateChangeService.class));
        // if the HTTPS pref changes, then update all affected things
        Preferences.get().registerLocalRepoHttpsListeners(new ChangeListener() {
            @Override
            public void onPreferenceChange() {
                startService(new Intent(FDroidApp.this, WifiStateChangeService.class));
            }
        });

        configureTor(Preferences.get().isTorEnabled());

        if (Preferences.get().isKeepingInstallHistory()) {
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

    @TargetApi(18)
    private BluetoothAdapter getBluetoothAdapter() {
        // to use the new, recommended way of getting the adapter
        // http://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html
        if (Build.VERSION.SDK_INT < 18) {
            return BluetoothAdapter.getDefaultAdapter();
        }
        return ((BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getAdapter();
    }

    public void sendViaBluetooth(Activity activity, int resultCode, String packageName) {
        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }

        String bluetoothPackageName = null;
        String className = null;
        boolean found = false;
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
                    found = true;
                    break;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not get application info to send via bluetooth", e);
            found = false;
        } catch (IOException e) {
            Exception toLog = new RuntimeException("Error preparing file to send via Bluetooth", e);
            ACRA.getErrorReporter().handleException(toLog, false);
        }

        if (sendBt != null) {
            if (found) {
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
