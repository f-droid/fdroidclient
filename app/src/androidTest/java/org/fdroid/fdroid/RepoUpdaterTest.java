
package org.fdroid.fdroid;

import android.app.Instrumentation;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.fdroid.fdroid.RepoUpdater.UpdateException;
import org.fdroid.fdroid.data.Repo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class RepoUpdaterTest {
    private static final String TAG = "RepoUpdaterTest";

    private Context context;
    private RepoUpdater repoUpdater;
    private File testFilesDir;

    String simpleIndexPubkey = "308201ee30820157a0030201020204300d845b300d06092a864886f70d01010b0500302a3110300e060355040b1307462d44726f6964311630140603550403130d70616c6174736368696e6b656e301e170d3134303432373030303633315a170d3431303931323030303633315a302a3110300e060355040b1307462d44726f6964311630140603550403130d70616c6174736368696e6b656e30819f300d06092a864886f70d010101050003818d0030818902818100a439472e4b6d01141bfc94ecfe131c7c728fdda670bb14c57ca60bd1c38a8b8bc0879d22a0a2d0bc0d6fdd4cb98d1d607c2caefbe250a0bd0322aedeb365caf9b236992fac13e6675d3184a6c7c6f07f73410209e399a9da8d5d7512bbd870508eebacff8b57c3852457419434d34701ccbf692267cbc3f42f1c5d1e23762d790203010001a321301f301d0603551d0e041604140b1840691dab909746fde4bfe28207d1cae15786300d06092a864886f70d01010b05000381810062424c928ffd1b6fd419b44daafef01ca982e09341f7077fb865905087aeac882534b3bd679b51fdfb98892cef38b63131c567ed26c9d5d9163afc775ac98ad88c405d211d6187bde0b0d236381cc574ba06ef9080721a92ae5a103a7301b2c397eecc141cc850dd3e123813ebc41c59d31ddbcb6e984168280c53272f6a442b";

    @Before
    public void setUp() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getContext();
        testFilesDir = TestUtils.getWriteableDir(instrumentation);
        Repo repo = new Repo();
        repo.pubkey = this.simpleIndexPubkey;
        repoUpdater = new RepoUpdater(context, repo);
    }

    @Test
    public void testExtractIndexFromJar() {
        if (!testFilesDir.canWrite()) {
            return;
        }
        File simpleIndexJar = TestUtils.copyAssetToDir(context, "simpleIndex.jar", testFilesDir);

        // these are supposed to succeed
        try {
            repoUpdater.processDownloadedFile(simpleIndexJar);
        } catch (UpdateException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test(expected = UpdateException.class)
    public void testExtractIndexFromJarWithoutSignatureJar() throws UpdateException {
        if (!testFilesDir.canWrite()) {
            return;
        }
        // this is supposed to fail
        File jarFile = TestUtils.copyAssetToDir(context, "simpleIndexWithoutSignature.jar", testFilesDir);
        repoUpdater.processDownloadedFile(jarFile);
    }

    @Test
    public void testExtractIndexFromJarWithCorruptedManifestJar() {
        if (!testFilesDir.canWrite()) {
            return;
        }
        // this is supposed to fail
        try {
            File jarFile = TestUtils.copyAssetToDir(context, "simpleIndexWithCorruptedManifest.jar", testFilesDir);
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
        if (!testFilesDir.canWrite()) {
            return;
        }
        // this is supposed to fail
        try {
            File jarFile = TestUtils.copyAssetToDir(context, "simpleIndexWithCorruptedSignature.jar", testFilesDir);
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
        if (!testFilesDir.canWrite()) {
            return;
        }
        // this is supposed to fail
        try {
            File jarFile = TestUtils.copyAssetToDir(context, "simpleIndexWithCorruptedCertificate.jar", testFilesDir);
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
        if (!testFilesDir.canWrite()) {
            return;
        }
        // this is supposed to fail
        try {
            File jarFile = TestUtils.copyAssetToDir(context, "simpleIndexWithCorruptedEverything.jar", testFilesDir);
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
        if (!testFilesDir.canWrite()) {
            return;
        }
        // this is supposed to fail
        try {
            File jarFile = TestUtils.copyAssetToDir(context, "masterKeyIndex.jar", testFilesDir);
            repoUpdater.processDownloadedFile(jarFile);
            fail();  //NOPMD
        } catch (UpdateException e) {
            // success!
        } catch (SecurityException e) {
            // success!
        }
    }
}
