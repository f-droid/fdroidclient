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
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.nostra13.universalimageloader.cache.disc.impl.LimitedAgeDiscCache;
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
import java.util.Set;

public class FDroidApp extends Application {

    // for the local repo on this device, all static since there is only one
    public static int port = 8888;
    public static String ipAddressString = null;
    public static String ssid = "";
    public static String bssid = "";
    public static Repo repo = new Repo();
    public static Set<String> selectedApps = null; // init in SelectLocalAppsFragment

    // Leaving the fully qualified class name here to help clarify the difference between spongy/bouncy castle.
    private static org.spongycastle.jce.provider.BouncyCastleProvider spongyCastleProvider;
    private static Messenger localRepoServiceMessenger = null;
    private static boolean localRepoServiceIsBound = false;

    private static final String TAG = "org.fdroid.fdroid.FDroidApp";

    BluetoothAdapter bluetoothAdapter = null;

    static {
        spongyCastleProvider = new org.spongycastle.jce.provider.BouncyCastleProvider();
        enableSpongyCastle();
    }

    public static enum Theme {
        dark, light, lightWithDarkActionBar
    }

    private static Theme curTheme = Theme.dark;

    public void reloadTheme() {
        curTheme = Theme.valueOf(PreferenceManager
                .getDefaultSharedPreferences(getBaseContext())
                .getString(Preferences.PREF_THEME, "dark"));
    }

    public void applyTheme(Activity activity) {
        switch (curTheme) {
            case dark:
                activity.setTheme(R.style.AppThemeDark);
                break;
            case light:
                activity.setTheme(R.style.AppThemeLight);
                break;
            case lightWithDarkActionBar:
                activity.setTheme(R.style.AppThemeLightWithDarkActionBar);
                break;
        }
    }

    public static Theme getCurTheme() {
        return curTheme;
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

    @Override
    public void onCreate() {
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
        // thread. In fact, we may as well start early for this reason.
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
        curTheme = Theme.valueOf(prefs.getString(Preferences.PREF_THEME, "dark"));
        if (!prefs.getBoolean(Preferences.PREF_CACHE_APK, false)) {

            File local_path = Utils.getApkCacheDir(this);
            // Things can be null if the SD card is not ready - we'll just
            // ignore that and do it next time.
            if (local_path != null) {
                final File[] files = local_path.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().endsWith(".apk")) {
                            f.delete();
                        }
                    }
                }
            }
        }

        UpdateService.schedule(getApplicationContext());
        bluetoothAdapter = getBluetoothAdapter();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
            .imageDownloader(new IconDownloader(getApplicationContext()))
            .diskCache(new LimitedAgeDiscCache(
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
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        int wifiState = wifiManager.getWifiState();
        if (wifiState == WifiManager.WIFI_STATE_ENABLING
                || wifiState == WifiManager.WIFI_STATE_ENABLED)
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
        } catch (NameNotFoundException e1) {
            e1.printStackTrace();
            found = false;
        }
        if (!found) {
            Toast.makeText(this, R.string.bluetooth_activity_not_found,
                    Toast.LENGTH_SHORT).show();
            activity.startActivity(Intent.createChooser(sendBt, getString(R.string.choose_bt_send)));
        } else {
            sendBt.setClassName(bluetoothPackageName, className);
            activity.startActivity(sendBt);
        }
    }

    private static ServiceConnection serviceConnection = new ServiceConnection() {
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

    public static void restartLocalRepoService() {
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
