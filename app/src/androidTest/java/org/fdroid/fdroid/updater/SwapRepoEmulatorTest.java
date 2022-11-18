package org.fdroid.fdroid.updater;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Looper;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import android.text.TextUtils;
import android.util.Log;

import org.fdroid.download.Downloader;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Hasher;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.nearby.LocalHTTPD;
import org.fdroid.fdroid.nearby.LocalRepoKeyStore;
import org.fdroid.fdroid.nearby.LocalRepoManager;
import org.fdroid.fdroid.nearby.LocalRepoService;
import org.fdroid.fdroid.net.DownloaderFactory;
import org.fdroid.index.IndexParser;
import org.fdroid.index.IndexParserKt;
import org.fdroid.index.v1.IndexV1;
import org.fdroid.index.v1.IndexV1UpdaterKt;
import org.fdroid.index.v1.IndexV1Verifier;
import org.fdroid.index.v1.PackageV1;
import org.fdroid.index.v2.FileV2;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.cert.Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import kotlin.Pair;

@LargeTest
public class SwapRepoEmulatorTest {
    public static final String TAG = "SwapRepoEmulatorTest";

    /**
     * @see org.fdroid.fdroid.nearby.WifiStateChangeService.WifiInfoThread#run()
     */
    @Ignore
    @Test
    public void testSwap() throws Exception {
        Looper.prepare();
        LocalHTTPD localHttpd = null;
        try {
            Log.i(TAG, "REPO: " + FDroidApp.repo);
            final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Preferences.setupForTests(context);

            FDroidApp.initWifiSettings();
            assertNull(FDroidApp.repo);

            final CountDownLatch latch = new CountDownLatch(1);
            new Thread() {
                @Override
                public void run() {
                    while (FDroidApp.repo == null) {
                        try {
                            String address = FDroidApp.repo == null ? null : FDroidApp.repo.getAddress();
                            Log.i(TAG, "Waiting for IP address... " + address);
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            // ignored
                        }
                    }
                    latch.countDown();
                }
            }.start();
            latch.await(10, TimeUnit.MINUTES);
            assertNotNull(FDroidApp.repo.getAddress());

            LocalRepoService.runProcess(context, new String[]{context.getPackageName()});
            Log.i(TAG, "REPO: " + FDroidApp.repo);
            File indexJarFile = LocalRepoManager.get(context).getIndexJar();
            assertTrue(indexJarFile.isFile());

            localHttpd = new LocalHTTPD(
                    context,
                    null,
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

            assertTrue(isPortInUse(FDroidApp.ipAddressString, FDroidApp.port));
            Thread.sleep(100);

            Uri uri = Uri.parse(FDroidApp.repo.getAddress())
                    .buildUpon()
                    .appendPath(IndexV1UpdaterKt.SIGNED_FILE_NAME)
                    .build();
            FileV2 indexFile = FileV2.fromPath("/" + IndexV1UpdaterKt.SIGNED_FILE_NAME);
            File swapJarFile = File.createTempFile("swap", "", context.getCacheDir());
            Downloader downloader =
                    DownloaderFactory.INSTANCE.createWithTryFirstMirror(FDroidApp.repo, uri, indexFile, swapJarFile);
            downloader.download();
            IndexV1Verifier verifier = new IndexV1Verifier(swapJarFile, null, fingerprint);
            Pair<String, IndexV1> pair = verifier.getStreamAndVerify(inputStream ->
                    IndexParserKt.parseV1(IndexParser.INSTANCE, inputStream)
            );
            assertEquals(signingCert, pair.getFirst());
            IndexV1 indexV1 = pair.getSecond();
            assertEquals(1, indexV1.getApps().size());
            assertEquals(context.getPackageName(), indexV1.getApps().get(0).getPackageName());
            long firstTimestamp = indexV1.getRepo().getTimestamp();

            assertEquals(1, indexV1.getPackages().size());
            List<PackageV1> apks = indexV1.getPackages().get(context.getPackageName());
            assertNotNull(apks);
            assertEquals(1, apks.size());
            for (PackageV1 apk : apks) {
                Log.i(TAG, "Apk: " + apk);
                assertNotNull(apk.getVersionCode());
                long versionCode = apk.getVersionCode();
                assertEquals(context.getPackageName(), apk.getPackageName());
                assertEquals(BuildConfig.VERSION_NAME, apk.getVersionName());
                assertEquals(BuildConfig.VERSION_CODE, versionCode);
            }

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfoList = context.getPackageManager().queryIntentActivities(mainIntent, 0);
            HashSet<String> packageNames = new HashSet<>();
            for (ResolveInfo resolveInfo : resolveInfoList) {
                if (!isSystemPackage(resolveInfo)) {
                    Log.i(TAG, "resolveInfo: " + resolveInfo);
                    packageNames.add(resolveInfo.activityInfo.packageName);
                }
            }
            LocalRepoService.runProcess(context, packageNames.toArray(new String[0]));

            swapJarFile = File.createTempFile("swap", "", context.getCacheDir());
            downloader =
                    DownloaderFactory.INSTANCE.createWithTryFirstMirror(FDroidApp.repo, uri, indexFile, swapJarFile);
            downloader.download();
            verifier = new IndexV1Verifier(swapJarFile, null, fingerprint);
            pair = verifier.getStreamAndVerify(inputStream ->
                    IndexParserKt.parseV1(IndexParser.INSTANCE, inputStream)
            );
            indexV1 = pair.getSecond();
            assertTrue(firstTimestamp < indexV1.getRepo().getTimestamp());
            for (String packageName : packageNames) {
                assertNotNull(indexV1.getPackages().get(packageName));
            }
        } finally {
            if (localHttpd != null) {
                localHttpd.stop();
            }
        }
        assertFalse(localHttpd.isAlive());
    }

    private boolean isPortInUse(String host, int port) {
        boolean result = false;

        try {
            (new Socket(host, port)).close();
            result = true;
        } catch (IOException e) {
            // Could not connect.
            e.printStackTrace();
        }
        return result;
    }

    private boolean isSystemPackage(ResolveInfo resolveInfo) {
        return (resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }
}
