package org.fdroid.fdroid.updater;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.ContentResolver;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.apache.commons.net.util.SubnetUtils;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.PreferencesTest;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.nearby.LocalHTTPD;
import org.fdroid.fdroid.nearby.LocalRepoKeyStore;
import org.fdroid.fdroid.nearby.LocalRepoManager;
import org.fdroid.fdroid.nearby.LocalRepoService;
import org.fdroid.fdroid.nearby.WifiStateChangeService;
import org.fdroid.fdroid.net.ConnectivityMonitorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.io.File;
import java.io.IOException;
import java.security.cert.Certificate;
import java.util.Locale;

/**
 * This test uses the swap repo setup as a fake repo to test {@link UpdateService}.
 */
@RunWith(RobolectricTestRunner.class)
public class UpdateServiceTest {
    public static final String TAG = "UpdateService";

    private LocalHTTPD localHttpd;

    protected ContentResolver contentResolver;
    protected ContextWrapper context;

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;

        contentResolver = ApplicationProvider.getApplicationContext().getContentResolver();

        context = new ContextWrapper(ApplicationProvider.getApplicationContext()) {
            @Override
            public ContentResolver getContentResolver() {
                return contentResolver;
            }
        };

        Preferences.setupForTests(context);
    }

    /**
     * @see WifiStateChangeService.WifiInfoThread#run()
     */
    @Test
    public void testSwap()
            throws IOException, LocalRepoKeyStore.InitException, InterruptedException {

        PackageManager packageManager = context.getPackageManager();

        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.flags = 0;
        appInfo.packageName = context.getPackageName();
        appInfo.minSdkVersion = 10;
        appInfo.targetSdkVersion = 23;
        File build = new File(getClass().getClassLoader().getResource("").getPath(), "../../../..");
        File apk = new File(build.getCanonicalFile(), "outputs/apk/full/debug/app-full-debug.apk");
        Log.i(TAG, "outputs " + apk + " " + apk.isDirectory());
        appInfo.sourceDir = apk.getCanonicalPath();
        appInfo.publicSourceDir = apk.getCanonicalPath();
        System.out.println("appInfo.sourceDir " + appInfo.sourceDir);
        appInfo.name = "F-Droid";

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = appInfo.packageName;
        packageInfo.applicationInfo = appInfo;
        packageInfo.signatures = new Signature[1];
        packageInfo.signatures[0] = new Signature("fake".getBytes());
        packageInfo.versionCode = 1002001;
        packageInfo.versionName = "1.2-fake";
        shadowOf(packageManager).addPackage(packageInfo);

        try {
            String host = null; // null basically means localhost
            FDroidApp.initWifiSettings();
            FDroidApp.networkState = ConnectivityMonitorService.FLAG_NET_NO_LIMIT;
            FDroidApp.ipAddressString = "127.0.0.1";
            FDroidApp.subnetInfo = new SubnetUtils("127.0.0.0/8").getInfo();
            String address = "http://" + FDroidApp.ipAddressString + ":" + FDroidApp.port + "/fdroid/repo";
            FDroidApp.repo = FDroidApp.createSwapRepo(address, null);  // TODO create a regular repo, not swap

            LocalRepoService.runProcess(context, new String[]{context.getPackageName()});
            Log.i(TAG, "REPO: " + FDroidApp.repo);
            File indexJarFile = LocalRepoManager.get(context).getIndexJar();
            System.out.println("indexJarFile:" + indexJarFile);
            assertTrue(indexJarFile.isFile());

            localHttpd = new LocalHTTPD(
                    context,
                    host,
                    FDroidApp.port,
                    LocalRepoManager.get(context).getWebRoot(),
                    false);
            localHttpd.start();
            Thread.sleep(100); // give the server some tine to start.
            assertTrue(localHttpd.isAlive());

            LocalRepoKeyStore localRepoKeyStore = LocalRepoKeyStore.get(context);
            Certificate localCert = localRepoKeyStore.getCertificate();
            String fingerprint = Utils.calcFingerprint(localCert).toLowerCase(Locale.ROOT);
            String signingCert = Hasher.hex(localCert);
            assertFalse(TextUtils.isEmpty(signingCert));
            assertFalse(TextUtils.isEmpty(fingerprint));

            assertTrue(Utils.isPortInUse(host, FDroidApp.port));
            Thread.sleep(100);

            Log.i(TAG, "FDroidApp.networkState " + FDroidApp.networkState);
            SharedPreferences prefs = PreferencesTest.getSharedPreferences(context);
            prefs.edit()
                    .putInt(Preferences.PREF_OVER_DATA, Preferences.OVER_NETWORK_ALWAYS)
                    .putInt(Preferences.PREF_OVER_WIFI, Preferences.OVER_NETWORK_ALWAYS)
                    .commit();
            final Intent intent = UpdateService.getIntent(context, address, fingerprint);
            final TestUpdateService testUpdateService = Robolectric.buildService(TestUpdateService.class,
                    intent).bind().get();
            Thread t = new Thread() {
                @Override
                public void run() {
                    testUpdateService.onCreate();
                    testUpdateService.onHandleWork(intent);
                }
            };
            t.start();
            t.join(10000);

            // TODO test what is in the repo.
            // TODO add app/src/test/assets/urzip.apk to the repo, then test another update
            // TODO test various PREF_OVER_DATA and PREF_OVER_WIFI combos
            Thread.sleep(1000);
        } finally {
            if (localHttpd != null) {
                localHttpd.stop();
            }
        }
        assertFalse(localHttpd.isAlive());
    }

    class TestLocalRepoService extends LocalRepoService {
        @Override
        protected void onHandleIntent(Intent intent) {
            super.onHandleIntent(intent);
        }
    }

    static class TestUpdateService extends UpdateService {
        @Override
        public void onHandleWork(@NonNull Intent intent) {
            super.onHandleWork(intent);
        }
    }
}
