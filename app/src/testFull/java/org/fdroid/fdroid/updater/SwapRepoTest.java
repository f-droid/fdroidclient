package org.fdroid.fdroid.updater;

import android.content.ContentResolver;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import org.apache.commons.net.util.SubnetUtils;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.IndexUpdater;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.TestUtils;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.DBHelper;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema;
import org.fdroid.fdroid.data.ShadowApp;
import org.fdroid.fdroid.data.TempAppProvider;
import org.fdroid.fdroid.nearby.LocalHTTPD;
import org.fdroid.fdroid.nearby.LocalRepoKeyStore;
import org.fdroid.fdroid.nearby.LocalRepoManager;
import org.fdroid.fdroid.nearby.LocalRepoService;
import org.fdroid.fdroid.nearby.WifiStateChangeService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.File;
import java.io.IOException;
import java.security.cert.Certificate;
import java.util.List;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

/**
 * This test almost works, it needs to have the {@link android.content.ContentProvider}
 * and {@link ContentResolver} stuff worked out.  It currently fails as
 * {@code updater.update()}.
 */
@Ignore
@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowApp.class)
public class SwapRepoTest {

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

        TestUtils.registerContentProvider(ApkProvider.getAuthority(), ApkProvider.class);
        TestUtils.registerContentProvider(AppProvider.getAuthority(), AppProvider.class);
        TestUtils.registerContentProvider(RepoProvider.getAuthority(), RepoProvider.class);
        TestUtils.registerContentProvider(TempAppProvider.getAuthority(), TempAppProvider.class);

        Preferences.setupForTests(context);
    }

    @After
    public final void tearDownBase() {
        DBHelper.clearDbHelperSingleton();
    }

    /**
     * @see WifiStateChangeService.WifiInfoThread#run()
     */
    @Test
    public void testSwap()
            throws IOException, LocalRepoKeyStore.InitException, IndexUpdater.UpdateException, InterruptedException {

        PackageManager packageManager = context.getPackageManager();
        
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.flags = 0;
        appInfo.packageName = context.getPackageName();
        appInfo.minSdkVersion = 10;
        appInfo.targetSdkVersion = 23;
        appInfo.sourceDir = getClass().getClassLoader().getResource("F-Droid.apk").getPath();
        appInfo.publicSourceDir = getClass().getClassLoader().getResource("F-Droid.apk").getPath();
        System.out.println("appInfo.sourceDir " + appInfo.sourceDir);
        appInfo.name = "F-Droid";

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = appInfo.packageName;
        packageInfo.applicationInfo = appInfo;
        packageInfo.versionCode = 1002001;
        packageInfo.versionName = "1.2-fake";
        shadowOf(packageManager).addPackage(packageInfo);

        try {
            FDroidApp.initWifiSettings();
            FDroidApp.ipAddressString = "127.0.0.1";
            FDroidApp.subnetInfo = new SubnetUtils("127.0.0.0/8").getInfo();
            FDroidApp.repo.name = "test";
            FDroidApp.repo.address = "http://" + FDroidApp.ipAddressString + ":" + FDroidApp.port + "/fdroid/repo";

            LocalRepoService.runProcess(context, new String[]{context.getPackageName()});
            File indexJarFile = LocalRepoManager.get(context).getIndexJar();
            System.out.println("indexJarFile:" + indexJarFile);
            assertTrue(indexJarFile.isFile());

            localHttpd = new LocalHTTPD(
                    context,
                    FDroidApp.ipAddressString,
                    FDroidApp.port,
                    LocalRepoManager.get(context).getWebRoot(),
                    false);
            localHttpd.start();
            Thread.sleep(100); // give the server some tine to start.
            assertTrue(localHttpd.isAlive());

            LocalRepoKeyStore localRepoKeyStore = LocalRepoKeyStore.get(context);
            Certificate localCert = localRepoKeyStore.getCertificate();
            String signingCert = Hasher.hex(localCert);
            assertFalse(TextUtils.isEmpty(signingCert));
            assertFalse(TextUtils.isEmpty(Utils.calcFingerprint(localCert)));

            Repo repo = MultiIndexUpdaterTest.createRepo(FDroidApp.repo.name, FDroidApp.repo.address,
                    context, signingCert);
            IndexUpdater updater = new IndexUpdater(context, repo);
            updater.update();
            assertTrue(updater.hasChanged());
            updater.processDownloadedFile(indexJarFile);

            boolean foundRepo = false;
            for (Repo repoFromDb : RepoProvider.Helper.all(context)) {
                if (TextUtils.equals(repo.address, repoFromDb.address)) {
                    foundRepo = true;
                    repo = repoFromDb;
                }
            }
            assertTrue(foundRepo);

            assertNotEquals(-1, repo.getId());
            List<Apk> apks = ApkProvider.Helper.findByRepo(context, repo, Schema.ApkTable.Cols.ALL);
            assertEquals(1, apks.size());
            for (Apk apk : apks) {
                System.out.println(apk);
            }
            //MultiIndexUpdaterTest.assertApksExist(apks, context.getPackageName(), new int[]{BuildConfig.VERSION_CODE});
            Thread.sleep(10000);
        } finally {
            if (localHttpd != null) {
                localHttpd.stop();
            }
        }
    }

    class TestLocalRepoService extends LocalRepoService {
        @Override
        protected void onHandleIntent(Intent intent) {
            super.onHandleIntent(intent);
        }
    }
}