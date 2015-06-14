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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.nostra13.universalimageloader.cache.disc.impl.LimitedAgeDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;

import org.fdroid.fdroid.Preferences.ChangeListener;
import org.fdroid.fdroid.compat.PRNGFixes;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.InstalledAppCacheUpdater;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.localrepo.LocalRepoService;
import org.fdroid.fdroid.net.IconDownloader;
import org.fdroid.fdroid.net.WifiStateChangeService;

import java.io.File;
import java.security.Security;
import java.util.Locale;
import java.util.Set;

public class FDroidApp extends Application {

    // for the local repo on this device, all static since there is only one
    public static int port;
    public static String ipAddressString;
    public static String ssid;
    public static String bssid;
    public static final Repo repo = new Repo();
    public static Set<String> selectedApps = null; // init in SelectLocalAppsFragment

    // Leaving the fully qualified class name here to help clarify the difference between spongy/bouncy castle.
    private static final org.spongycastle.jce.provider.BouncyCastleProvider spongyCastleProvider;
    private static Messenger localRepoServiceMessenger = null;
    private static boolean localRepoServiceIsBound = false;

    BluetoothAdapter bluetoothAdapter = null;

    static {
        spongyCastleProvider = new org.spongycastle.jce.provider.BouncyCastleProvider();
        enableSpongyCastle();
    }

    public enum Theme {
        dark, light, lightWithDarkActionBar
    }

    private static Theme curTheme = Theme.dark;

    public void reloadTheme() {
        curTheme = Theme.valueOf(PreferenceManager
                .getDefaultSharedPreferences(getBaseContext())
                .getString(Preferences.PREF_THEME, "dark"));
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
            case lightWithDarkActionBar:
                return R.style.AppThemeLightWithDarkActionBar;
            default:
                return R.style.AppThemeDark;
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

    @Override
    public void onCreate() {
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

    void sendViaBluetooth(Activity activity, int resultCode, String packageName) {
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
        } catch (PackageManager.NameNotFoundException e1) {
            e1.printStackTrace();
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

    private static final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            localRepoServiceMessenger = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            localRepoServiceMessenger = null;
        }
    };

    public static void startLocalRepoService(Context context) {
        if (!localRepoServiceIsBound) {
            Context app = context.getApplicationContext();
            Intent service = new Intent(app, LocalRepoService.class);
            localRepoServiceIsBound = app.bindService(service, serviceConnection, Context.BIND_AUTO_CREATE);
            if (localRepoServiceIsBound)
                app.startService(service);
        }
    }

    public static void stopLocalRepoService(Context context) {
        Context app = context.getApplicationContext();
        if (localRepoServiceIsBound) {
            app.unbindService(serviceConnection);
            localRepoServiceIsBound = false;
        }
        app.stopService(new Intent(app, LocalRepoService.class));
    }

    /**
     * Handles checking if the {@link LocalRepoService} is running, and only restarts it if it was running.
     */
    public static void restartLocalRepoServiceIfRunning() {
        if (localRepoServiceMessenger != null) {
            try {
                Message msg = Message.obtain(null, LocalRepoService.RESTART, LocalRepoService.RESTART, 0);
                localRepoServiceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isLocalRepoServiceRunning() {
        return localRepoServiceIsBound;
    }
}
