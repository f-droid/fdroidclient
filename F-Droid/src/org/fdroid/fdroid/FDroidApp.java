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
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.nostra13.universalimageloader.cache.disc.impl.LimitedAgeDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;

import org.apache.commons.net.util.SubnetUtils;
import org.fdroid.fdroid.Preferences.ChangeListener;
import org.fdroid.fdroid.compat.PRNGFixes;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.InstalledAppCacheUpdater;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.net.IconDownloader;
import org.fdroid.fdroid.net.WifiStateChangeService;

import java.io.File;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.Security;
import java.util.Locale;

import sun.net.www.protocol.bluetooth.Handler;

public class FDroidApp extends Application {

    private static final String TAG = "FDroidApp";

    // for the local repo on this device, all static since there is only one
    public static int port;
    public static String ipAddressString;
    public static SubnetUtils.SubnetInfo subnetInfo;
    public static String ssid;
    public static String bssid;
    public static final Repo repo = new Repo();

    // Leaving the fully qualified class name here to help clarify the difference between spongy/bouncy castle.
    private static final org.spongycastle.jce.provider.BouncyCastleProvider spongyCastleProvider;

    @SuppressWarnings("unused")
    BluetoothAdapter bluetoothAdapter = null;

    static {
        spongyCastleProvider = new org.spongycastle.jce.provider.BouncyCastleProvider();
        enableSpongyCastle();
    }

    public enum Theme {
        dark,
        light,
        lightWithDarkActionBar, // Obsolete
    }

    private static Theme curTheme = Theme.light;

    public void reloadTheme() {
        curTheme = Theme.valueOf(PreferenceManager
                .getDefaultSharedPreferences(getBaseContext())
                .getString(Preferences.PREF_THEME, Preferences.DEFAULT_THEME));
    }

    public void applyTheme(Activity activity) {
            activity.setTheme(getCurThemeResId());
    }

    public static Theme getCurTheme() {
        return curTheme;
    }

    public static int getCurThemeResId() {
        switch (curTheme) {
            case dark:
                return R.style.AppThemeDark;
            case light:
                return R.style.AppThemeLight;
            default:
                return R.style.AppThemeLight;
        }
    }

    public static void enableSpongyCastle() {
        Security.addProvider(spongyCastleProvider);
    }

    public static void enableSpongyCastleOnLollipop() {
        if (Build.VERSION.SDK_INT == 21) {
            Security.addProvider(spongyCastleProvider);
        }
    }

    public static void disableSpongyCastleOnLollipop() {
        if (Build.VERSION.SDK_INT == 21) {
            Security.removeProvider(spongyCastleProvider.getName());
        }
    }

    public static void initWifiSettings() {
        port = 8888;
        ipAddressString = null;
        subnetInfo = (new SubnetUtils("0.0.0.0/32").getInfo());
        ssid = "";
        bssid = "";
    }

    public static void updateLanguage(Context c) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(c);
        String lang = prefs.getString("language", "");
        updateLanguage(c, lang);
    }

    public static void updateLanguage(Context c, String lang) {
        final Configuration cfg = new Configuration();
        final Locale newLocale = Utils.getLocaleFromAndroidLangTag(lang);
        cfg.locale = newLocale == null ? Locale.getDefault() : newLocale;
        c.getResources().updateConfiguration(cfg, null);
    }

    @TargetApi(9)
    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= 9 && BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
        updateLanguage(this);
        super.onCreate();

        // Needs to be setup before anything else tries to access it.
        // Perhaps the constructor is a better place, but then again,
        // it is more deterministic as to when this gets called...
        Preferences.setup(this);

        // Apply the Google PRNG fixes to properly seed SecureRandom
        PRNGFixes.apply();

        // Check that the installed app cache hasn't gotten out of sync somehow.
        // e.g. if we crashed/ran out of battery half way through responding
        // to a package installed intent. It doesn't really matter where
        // we put this in the bootstrap process, because it runs on a different
        // thread, which will be delayed by some seconds to avoid an error where
        // the database is locked due to the database updater.
        InstalledAppCacheUpdater.updateInBackground(getApplicationContext());

        // If the user changes the preference to do with filtering rooted apps,
        // it is easier to just notify a change in the app provider,
        // so that the newly updated list will correctly filter relevant apps.
        Preferences.get().registerAppsRequiringRootChangeListener(new Preferences.ChangeListener() {
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
                AppProvider.Helper.calcDetailsFromIndex(context);
            }
        });

        // Clear cached apk files. We used to just remove them after they'd
        // been installed, but this causes problems for proprietary gapps
        // users since the introduction of verification (on pre-4.2 Android),
        // because the install intent says it's finished when it hasn't.
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());
        curTheme = Theme.valueOf(prefs.getString(Preferences.PREF_THEME, Preferences.DEFAULT_THEME));
        Utils.deleteFiles(Utils.getApkDownloadDir(this), null, ".apk");
        if (!Preferences.get().shouldCacheApks()) {
            Utils.deleteFiles(Utils.getApkCacheDir(this), null, ".apk");
        }

        // Index files which downloaded, but were not removed (e.g. due to F-Droid being force
        // closed during processing of the file, before getting a chance to delete). This may
        // include both "index-*-downloaded" and "index-*-extracted.xml" files. The first is from
        // either signed or unsigned repos, and the later is from signed repos.
        Utils.deleteFiles(getCacheDir(), "index-", null);

        // As above, but for legacy F-Droid clients that downloaded under a different name, and
        // extracted to the files directory rather than the cache directory.
        // TODO: This can be removed in a a few months or a year (e.g. 2016) because people will
        // have upgraded their clients, this code will have executed, and they will not have any
        // left over files any more. Even if they do hold off upgrading until this code is removed,
        // the only side effect is that they will have a few more MiB of storage taken up on their
        // device until they uninstall and re-install F-Droid.
        Utils.deleteFiles(getCacheDir(), "dl-", null);
        Utils.deleteFiles(getFilesDir(), "index-", null);

        UpdateService.schedule(getApplicationContext());
        bluetoothAdapter = getBluetoothAdapter();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
            .imageDownloader(new IconDownloader(getApplicationContext()))
            .diskCache(new LimitedAgeDiskCache(
                        new File(StorageUtils.getCacheDirectory(getApplicationContext(), true),
                            "icons"),
                        null,
                        new FileNameGenerator() {
                            @Override
                            public String generate(String imageUri) {
                                return imageUri.substring(
                                    imageUri.lastIndexOf('/') + 1);
                            } },
                        // 30 days in secs: 30*24*60*60 = 2592000
                        2592000)
                    )
            .threadPoolSize(4)
            .threadPriority(Thread.NORM_PRIORITY - 2) // Default is NORM_PRIORITY - 1
            .build();
        ImageLoader.getInstance().init(config);

        // TODO reintroduce PinningTrustManager and MemorizingTrustManager

        // initialized the local repo information
        FDroidApp.initWifiSettings();
        startService(new Intent(this, WifiStateChangeService.class));
        // if the HTTPS pref changes, then update all affected things
        Preferences.get().registerLocalRepoHttpsListeners(new ChangeListener() {
            @Override
            public void onPreferenceChange() {
                startService(new Intent(FDroidApp.this, WifiStateChangeService.class));
            }
        });
    }

    @TargetApi(18)
    private BluetoothAdapter getBluetoothAdapter() {
        // to use the new, recommended way of getting the adapter
        // http://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html
        if (Build.VERSION.SDK_INT < 18)
            return BluetoothAdapter.getDefaultAdapter();
        else
            return ((BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getAdapter();
    }

    public void sendViaBluetooth(Activity activity, int resultCode, String packageName) {
        if (resultCode == Activity.RESULT_CANCELED)
            return;
        String bluetoothPackageName = null;
        String className = null;
        boolean found = false;
        Intent sendBt = null;
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName,
                    PackageManager.GET_META_DATA);
            sendBt = new Intent(Intent.ACTION_SEND);
            // The APK type is blocked by stock Android, so use zip
            // sendBt.setType("application/vnd.android.package-archive");
            sendBt.setType("application/zip");
            sendBt.putExtra(Intent.EXTRA_STREAM,
                    Uri.parse("file://" + appInfo.publicSourceDir));
            // not all devices have the same Bluetooth Activities, so
            // let's find it
            for (ResolveInfo info : pm.queryIntentActivities(sendBt, 0)) {
                bluetoothPackageName = info.activityInfo.packageName;
                if (bluetoothPackageName.equals("com.android.bluetooth")
                        || bluetoothPackageName.equals("com.mediatek.bluetooth")) {
                    className = info.activityInfo.name;
                    found = true;
                    break;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not get application info to send via bluetooth", e);
            found = false;
        }
        if (sendBt != null) {
            if (!found) {
                Toast.makeText(this, R.string.bluetooth_activity_not_found,
                        Toast.LENGTH_SHORT).show();
                activity.startActivity(Intent.createChooser(sendBt, getString(R.string.choose_bt_send)));
            } else {
                sendBt.setClassName(bluetoothPackageName, className);
                activity.startActivity(sendBt);
            }
        }
    }
}
