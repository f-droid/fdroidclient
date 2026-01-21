package org.fdroid.fdroid;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.index.v2.FileV1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

public class TestUtils {

    @SuppressWarnings("unused")
    private static final String TAG = "TestUtils"; // NOPMD

    /**
     * These signers are used to sign the Bitcoin Wallet in f-droid.org/repo.
     */
    public static final String FDROID_SIGNER = "bcb4b93636bb10c7ddaf61aa9604ff795dfdb05fad4d9412b335682f0b612e32";
    public static final String UPSTREAM_SIGNER = "58dcd8a0edf2a590683ba022d22a8dca5659aabf4728741a5c07af738d53db38";

    public static Apk getApk(int versionCode) {
        return getApk(versionCode, "signature", null);
    }

    public static Apk getApk(int versionCode, String signer, String releaseChannel) {
        Apk apk = new Apk();
        apk.repoAddress = "http://www.example.com/fdroid/repo";
        apk.canonicalRepoAddress = "http://www.example.com/fdroid/repo";
        apk.versionCode = versionCode;
        apk.repoId = 1;
        apk.versionName = "The good one";
        apk.apkFile = new FileV1("Test Apk", "hash", null, null);
        apk.size = 10000;
        apk.compatible = true;
        apk.signer = signer;
        apk.releaseChannels = releaseChannel == null ?
                null : Collections.singletonList(releaseChannel);
        return apk;
    }

    public static App getApp() {
        App app = new App();
        app.packageName = "com.example.app";
        app.name = "Test App";
        app.repoId = 1;
        app.summary = "test summary";
        app.description = "test description";
        app.license = "GPL?";
        app.compatible = true;
        return app;
    }

    public static File copyResourceToTempFile(String resourceName) {
        File tempFile = null;
        InputStream input = null;
        OutputStream output = null;
        try {
            tempFile = File.createTempFile(resourceName + "-", ".testasset");
            input = TestUtils.class.getClassLoader().getResourceAsStream(resourceName);
            output = new FileOutputStream(tempFile);
            Utils.copy(input, output);
        } catch (IOException e) {
            e.printStackTrace();
            if (tempFile != null && tempFile.exists()) {
                assertTrue(tempFile.delete());
            }
            fail();
            return null;
        } finally {
            Utils.closeQuietly(output);
            Utils.closeQuietly(input);
        }
        return tempFile;
    }
}
