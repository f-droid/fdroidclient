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

import java.io.File;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import android.preference.PreferenceManager;
import android.util.Log;

import com.nostra13.universalimageloader.cache.disc.impl.LimitedAgeDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;

import de.duenndns.ssl.MemorizingTrustManager;

import org.fdroid.fdroid.data.AppProvider;
import org.thoughtcrime.ssl.pinning.PinningTrustManager;
import org.thoughtcrime.ssl.pinning.SystemKeyStore;

public class FDroidApp extends Application {

    private static enum Theme {
        dark, light
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
                //activity.setTheme(R.style.AppThemeDark);
                return;
            case light:
                activity.setTheme(R.style.AppThemeLight);
                return;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Needs to be setup before anything else tries to access it.
        // Perhaps the constructor is a better place, but then again,
        // it is more deterministic as to when this gets called...
        Preferences.setup(this);

        // Set this up here, and the testing framework will override it when
        // it gets fired up.
        Utils.setupInstalledApkCache(new Utils.InstalledApkCache());

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
                File[] files = local_path.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().endsWith(".apk")) {
                            f.delete();
                        }
                    }
                }
            }
        }

        invalidApps = new ArrayList<String>();
        ctx = getApplicationContext();
        UpdateService.schedule(ctx);

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(ctx)
            .discCache(new LimitedAgeDiscCache(
                        new File(StorageUtils.getCacheDirectory(ctx, true),
                            "icons"),
                        new FileNameGenerator() {
                            @Override
                            public String generate(String imageUri) {
                                return imageUri.substring(
                                    imageUri.lastIndexOf('/') + 1);
                            } },
                        // 30 days in secs: 30*24*60*60 = 2592000
                        2592000)
                    )
            .threadPoolSize(Runtime.getRuntime().availableProcessors() * 2)
            .build();
        ImageLoader.getInstance().init(config);

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            X509TrustManager defaultTrustManager = null;

            /*
             * init a trust manager factory with a null keystore to access the system trust managers
             */
            TrustManagerFactory tmf =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ks = null;
            tmf.init(ks);
            TrustManager[] mgrs = tmf.getTrustManagers();

            if(mgrs.length > 0 && mgrs[0] instanceof X509TrustManager)
                defaultTrustManager = (X509TrustManager) mgrs[0];

            /*
             * compose a chain of trust managers as follows:
             * MemorizingTrustManager -> Pinning Trust Manager -> System Trust Manager
             */
            PinningTrustManager pinMgr = new PinningTrustManager(SystemKeyStore.getInstance(ctx),FDroidCertPins.getPinList(), 0);
            MemorizingTrustManager memMgr = new MemorizingTrustManager(ctx, pinMgr, defaultTrustManager);

            /*
             * initialize a SSLContext with the outermost trust manager, use this
             * context to set the default SSL socket factory for the HTTPSURLConnection
             * class.
             */
            sc.init(null, new TrustManager[] {memMgr}, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException e) {
            Log.e("FDroid", "Unable to set up trust manager chain. KeyManagementException");
        } catch (NoSuchAlgorithmException e) {
            Log.e("FDroid", "Unable to set up trust manager chain. NoSuchAlgorithmException");
        } catch (KeyStoreException e) {
            Log.e("FDroid", "Unable to set up trust manager chain. KeyStoreException");
        }
    }

    private Context ctx;

    // Set when something has changed (database or installed apps) so we know
    // we should invalidate the apps.
    private Semaphore appsInvalidLock = new Semaphore(1, false);
    private List<String> invalidApps;

    // Set apps invalid. Call this when the database has been updated with
    // new app information, or when the installed packages have changed.
    public void invalidateAllApps() {
        try {
            appsInvalidLock.acquire();
        } catch (InterruptedException e) {
            // Don't care
        } finally {
            appsInvalidLock.release();
        }
    }

    // Invalidate a single app
    public void invalidateApp(String id) {
        Log.d("FDroid", "Invalidating "+id);
        invalidApps.add(id);
    }

}
