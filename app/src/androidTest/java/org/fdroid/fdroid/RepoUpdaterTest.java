
package org.fdroid.fdroid;

import android.app.Instrumentation;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.fdroid.fdroid.RepoUpdater.UpdateException;
import org.fdroid.fdroid.data.Repo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class RepoUpdaterTest {
    public static final String TAG = "RepoUpdaterTest";

    private Context context;
    private Repo repo;
    private RepoUpdater repoUpdater;
    private File testFilesDir;

    String simpleIndexSigningCert = "308201ee30820157a0030201020204300d845b300d06092a864886f70d01010b0500302a3110300e060355040b1307462d44726f6964311630140603550403130d70616c6174736368696e6b656e301e170d3134303432373030303633315a170d3431303931323030303633315a302a3110300e060355040b1307462d44726f6964311630140603550403130d70616c6174736368696e6b656e30819f300d06092a864886f70d010101050003818d0030818902818100a439472e4b6d01141bfc94ecfe131c7c728fdda670bb14c57ca60bd1c38a8b8bc0879d22a0a2d0bc0d6fdd4cb98d1d607c2caefbe250a0bd0322aedeb365caf9b236992fac13e6675d3184a6c7c6f07f73410209e399a9da8d5d7512bbd870508eebacff8b57c3852457419434d34701ccbf692267cbc3f42f1c5d1e23762d790203010001a321301f301d0603551d0e041604140b1840691dab909746fde4bfe28207d1cae15786300d06092a864886f70d01010b05000381810062424c928ffd1b6fd419b44daafef01ca982e09341f7077fb865905087aeac882534b3bd679b51fdfb98892cef38b63131c567ed26c9d5d9163afc775ac98ad88c405d211d6187bde0b0d236381cc574ba06ef9080721a92ae5a103a7301b2c397eecc141cc850dd3e123813ebc41c59d31ddbcb6e984168280c53272f6a442b";

    /**
     * Getting a writeable dir during the tests seems to be a flaky prospect.
     */
    private boolean canWrite() {
        if (testFilesDir.canWrite()) {
            return true;
        } else {
            Log.e(TAG, "ERROR: " + testFilesDir + " is not writable, skipping test");
            return false;
        }
    }

    @Before
    public void setUp() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getContext();
        testFilesDir = TestUtils.getWriteableDir(instrumentation);
        repo = new Repo();
        repo.address = "https://fake.url/fdroid/repo";
        repo.signingCertificate = this.simpleIndexSigningCert;
    }

    @Test
    public void testExtractIndexFromJar() {
        assumeTrue(canWrite());
        File simpleIndexJar = TestUtils.copyAssetToDir(context, "simpleIndex.jar", testFilesDir);
        repoUpdater = new RepoUpdater(context, repo);

        // these are supposed to succeed
        try {
            repoUpdater.processDownloadedFile(simpleIndexJar);
        } catch (UpdateException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test(expected = UpdateException.class)
    public void testExtractIndexFromOutdatedJar() throws UpdateException {
        assumeTrue(canWrite());
        File simpleIndexJar = TestUtils.copyAssetToDir(context, "simpleIndex.jar", testFilesDir);
        repo.version = 10;
        repo.timestamp = System.currentTimeMillis() / 1000L;
        repoUpdater = new RepoUpdater(context, repo);

        // these are supposed to fail
        repoUpdater.processDownloadedFile(simpleIndexJar);
        fail();
    }

    @Test(expected = UpdateException.class)
    public void testExtractIndexFromJarWithoutSignatureJar() throws UpdateException {
        assumeTrue(canWrite());
        // this is supposed to fail
        File jarFile = TestUtils.copyAssetToDir(context, "simpleIndexWithoutSignature.jar", testFilesDir);
        repoUpdater = new RepoUpdater(context, repo);
        repoUpdater.processDownloadedFile(jarFile);
        fail();
    }

    @Test
    public void testExtractIndexFromJarWithCorruptedManifestJar() {
        assumeTrue(canWrite());
        // this is supposed to fail
        try {
            File jarFile = TestUtils.copyAssetToDir(context, "simpleIndexWithCorruptedManifest.jar", testFilesDir);
            repoUpdater = new RepoUpdater(context, repo);
            repoUpdater.processDownloadedFile(jarFile);
            fail();
        } catch (UpdateException e) {
            e.printStackTrace();
            fail();
        } catch (SecurityException e) {
            // success!
        }
    }

    @Test
    public void testExtractIndexFromJarWithCorruptedSignature() {
        assumeTrue(canWrite());
        // this is supposed to fail
        try {
            File jarFile = TestUtils.copyAssetToDir(context, "simpleIndexWithCorruptedSignature.jar", testFilesDir);
            repoUpdater = new RepoUpdater(context, repo);
            repoUpdater.processDownloadedFile(jarFile);
            fail();
        } catch (UpdateException e) {
            e.printStackTrace();
            fail();
        } catch (SecurityException e) {
            // success!
        }
    }

    @Test
    public void testExtractIndexFromJarWithCorruptedCertificate() {
        assumeTrue(canWrite());
        // this is supposed to fail
        try {
            File jarFile = TestUtils.copyAssetToDir(context, "simpleIndexWithCorruptedCertificate.jar", testFilesDir);
            repoUpdater = new RepoUpdater(context, repo);
            repoUpdater.processDownloadedFile(jarFile);
            fail();
        } catch (UpdateException e) {
            e.printStackTrace();
            fail();
        } catch (SecurityException e) {
            // success!
        }
    }

    @Test
    public void testExtractIndexFromJarWithCorruptedEverything() {
        assumeTrue(canWrite());
        // this is supposed to fail
        try {
            File jarFile = TestUtils.copyAssetToDir(context, "simpleIndexWithCorruptedEverything.jar", testFilesDir);
            repoUpdater = new RepoUpdater(context, repo);
            repoUpdater.processDownloadedFile(jarFile);
            fail();
        } catch (UpdateException e) {
            e.printStackTrace();
            fail();
        } catch (SecurityException e) {
            // success!
        }
    }

    @Test
    public void testExtractIndexFromMasterKeyIndexJar() {
        assumeTrue(canWrite());
        // this is supposed to fail
        try {
            File jarFile = TestUtils.copyAssetToDir(context, "masterKeyIndex.jar", testFilesDir);
            repoUpdater = new RepoUpdater(context, repo);
            repoUpdater.processDownloadedFile(jarFile);
            fail();  //NOPMD
        } catch (UpdateException e) {
            // success!
        } catch (SecurityException e) {
            // success!
        }
    }
}
