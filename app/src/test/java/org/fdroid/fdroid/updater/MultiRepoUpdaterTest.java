
package org.fdroid.fdroid.updater;

import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import org.fdroid.fdroid.IndexV1Updater;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.RepoUpdater;
import org.fdroid.fdroid.RepoUpdater.UpdateException;
import org.fdroid.fdroid.TestUtils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.FDroidProviderTest;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class MultiRepoUpdaterTest extends FDroidProviderTest {
    @SuppressWarnings("unused")
    private static final String TAG = "AcceptableMultiRepoUpdaterTest"; // NOPMD

    protected static final String REPO_MAIN = "Test F-Droid repo";
    protected static final String REPO_ARCHIVE = "Test F-Droid repo (Archive)";
    protected static final String REPO_CONFLICTING = "Test F-Droid repo with different apps";

    protected static final String REPO_MAIN_URI = "https://f-droid.org/repo";
    protected static final String REPO_ARCHIVE_URI = "https://f-droid.org/archive";
    protected static final String REPO_CONFLICTING_URI = "https://example.com/conflicting/fdroid/repo";

    private static final String PUB_KEY =
            "3082050b308202f3a003020102020420d8f212300d06092a864886f70d01010b050030363110300e0603" +
                    "55040b1307462d44726f69643122302006035504031319657073696c6f6e2e70657465722e7365727779" +
                    "6c6f2e636f6d301e170d3135303931323233313632315a170d3433303132383233313632315a30363110" +
                    "300e060355040b1307462d44726f69643122302006035504031319657073696c6f6e2e70657465722e73" +
                    "657277796c6f2e636f6d30820222300d06092a864886f70d01010105000382020f003082020a02820201" +
                    "00b21fe72b84ce721967851364bd20511088d117bc3034e4bb4d3c1a06af2a308fdffdaf63b12e0926b9" +
                    "0545134b9ff570646cbcad89d9e86dcc8eb9977dd394240c75bccf5e8ddc3c5ef91b4f16eca5f36c36f1" +
                    "92463ff2c9257d3053b7c9ecdd1661bd01ec3fe70ee34a7e6b92ddba04f258a32d0cfb1b0ce85d047180" +
                    "97fc4bdfb54541b430dfcfc1c84458f9eb5627e0ec5341d561c3f15f228379a1282d241329198f31a7ac" +
                    "cd51ab2bbb881a1da55001123483512f77275f8990c872601198065b4e0137ddd1482e4fdefc73b857d4" +
                    "be324ca96c268ceb725398f8cc38a0dc6aa2c277f8686724e8c7ff3f320a05791fccacc6caa956cf23a9" +
                    "de2dc7070b262c0e35d90d17e90773bb11e875e79a8dfd958e359d5d5ad903a7cbc2955102502bd0134c" +
                    "a1ff7a0bbbbb57302e4a251e40724dcaa8ad024f4b3a71b8fceaac664c0dcc1995a1c4cf42676edad8bc" +
                    "b03ba255ab796677f18fff2298e1aaa5b134254b44d08a4d934c9859af7bbaf078c37b7f628db0e2cffb" +
                    "0493a669d5f4770d35d71284550ce06d6f6811cd2a31585085716257a4ba08ad968b0a2bf88f34ca2f2c" +
                    "73af1c042ab147597faccfb6516ef4468cfa0c5ab3c8120eaa7bac1080e4d2310f717db20815d0e1ee26" +
                    "bd4e47eed8d790892017ae9595365992efa1b7fd1bc1963f018264b2b3749b8f7b1907bb0843f1e7fc2d" +
                    "3f3b02284cd4bae0ab0203010001a321301f301d0603551d0e0416041456110e4fed863ab1df9448bfd9" +
                    "e10a8bc32ffe08300d06092a864886f70d01010b050003820201008082572ae930ebc55ecf1110f4bb72" +
                    "ad2a952c8ac6e65bd933706beb4a310e23deabb8ef6a7e93eea8217ab1f3f57b1f477f95f1d62eccb563" +
                    "67a4d70dfa6fcd2aace2bb00b90af39412a9441a9fae2396ff8b93de1df3d9837c599b1f80b7d75285cb" +
                    "df4539d7dd9612f54b45ca59bc3041c9b92fac12753fac154d12f31df360079ab69a2d20db9f6a7277a8" +
                    "259035e93de95e8cbc80351bc83dd24256183ea5e3e1db2a51ea314cdbc120c064b77e2eb3a731530511" +
                    "1e1dabed6996eb339b7cb948d05c1a84d63094b4a4c6d11389b2a7b5f2d7ecc9a149dda6c33705ef2249" +
                    "58afdfa1d98cf646dcf8857cd8342b1e07d62cb4313f35ad209046a4a42ff73f38cc740b1e695eeda49d" +
                    "5ea0384ad32f9e3ae54f6a48a558dbc7cccabd4e2b2286dc9c804c840bd02b9937841a0e48db00be9e3c" +
                    "d7120cf0f8648ce4ed63923f0352a2a7b3b97fc55ba67a7a218b8c0b3cda4a45861280a622e0a59cc9fb" +
                    "ca1117568126c581afa4408b0f5c50293c212c406b8ab8f50aad5ed0f038cfca580ef3aba7df25464d9e" +
                    "495ffb629922cfb511d45e6294c045041132452f1ed0f20ac3ab4792f610de1734e4c8b71d743c4b0101" +
                    "98f848e0dbfce5a0f2da0198c47e6935a47fda12c518ef45adfb66ddf5aebaab13948a66c004b8592d22" +
                    "e8af60597c4ae2977977cf61dc715a572e241ae717cafdb4f71781943945ac52e0f50b";

    @Before
    public final void setupMultiRepo() throws Exception {
        // On a fresh database install, there will be F-Droid + GP repos, including their Archive
        // repos that we are not interested in.
        RepoProvider.Helper.remove(context, 1);
        RepoProvider.Helper.remove(context, 2);
        RepoProvider.Helper.remove(context, 3);
        RepoProvider.Helper.remove(context, 4);

        Preferences.setupForTests(context);
    }

    protected void assertApp(String packageName, int[] versionCodes) {
        List<Apk> apks = ApkProvider.Helper.findByPackageName(context, packageName);
        assertApksExist(apks, packageName, versionCodes);
    }

    protected void assertApp2048() {
        assertApp("com.uberspot.a2048", new int[]{19, 18});
    }

    protected void assertAppAdaway() {
        assertApp("org.adaway", new int[]{54, 53, 52, 51, 50, 49, 48, 47, 46, 45, 42, 40, 38, 37, 36, 35});
    }

    protected void assertAppAdbWireless() {
        assertApp("siir.es.adbWireless", new int[]{12});
    }

    protected void assertAppIcsImport() {
        assertApp("org.dgtale.icsimport", new int[]{3, 2});
    }

    @NonNull
    protected Repo findRepo(@NonNull String name, List<Repo> allRepos) {
        Repo repo = null;
        for (Repo r : allRepos) {
            if (TextUtils.equals(name, r.getName())) {
                repo = r;
                break;
            }
        }

        assertNotNull("Repo " + allRepos, repo);
        return repo;
    }

    /**
     * Checks that each version of appId as specified in versionCodes is present in apksToCheck.
     */
    protected void assertApksExist(List<Apk> apksToCheck, String appId, int[] versionCodes) {
        for (int versionCode : versionCodes) {
            boolean found = false;
            for (Apk apk : apksToCheck) {
                if (apk.versionCode == versionCode && apk.packageName.equals(appId)) {
                    found = true;
                    break;
                }
            }

            assertTrue("Couldn't find app " + appId + ", v" + versionCode, found);
        }
    }

    protected void assertEmpty() {
        assertEquals("No apps present", 0, AppProvider.Helper.all(context.getContentResolver()).size());

        String[] packages = {
                "com.uberspot.a2048",
                "org.adaway",
                "siir.es.adbWireless",
        };

        for (String id : packages) {
            assertEquals("No apks for " + id, 0, ApkProvider.Helper.findByPackageName(context, id).size());
        }
    }

    protected Repo createRepo(String name, String uri, Context context) {
        return createRepo(name, uri, context, PUB_KEY);
    }

    /**
     * Creates a real instance of {@code Repo} by loading it from the database,
     * that ensures it includes the primary key from the database.
     */
    static Repo createRepo(String name, String uri, Context context, String signingCert) {
        ContentValues values = new ContentValues(3);
        values.put(Schema.RepoTable.Cols.SIGNING_CERT, signingCert);
        values.put(Schema.RepoTable.Cols.ADDRESS, uri);
        values.put(Schema.RepoTable.Cols.NAME, name);
        RepoProvider.Helper.insert(context, values);
        return RepoProvider.Helper.findByAddress(context, uri);
    }

    protected RepoUpdater createRepoUpdater(String name, String uri, Context context) {
        return new RepoUpdater(context, createRepo(name, uri, context));
    }

    protected RepoUpdater createRepoUpdater(String name, String uri, Context context, String signingCert) {
        return new RepoUpdater(context, createRepo(name, uri, context, signingCert));
    }

    protected IndexV1Updater createIndexV1Updater(String name, String uri, Context context, String signingCert) {
        return new IndexV1Updater(context, createRepo(name, uri, context, signingCert));
    }

    protected void updateConflicting() throws UpdateException {
        updateRepo(createRepoUpdater(REPO_CONFLICTING, REPO_CONFLICTING_URI, context), "multiRepo.conflicting.jar");
    }

    protected void updateMain() throws UpdateException {
        updateRepo(createRepoUpdater(REPO_MAIN, REPO_MAIN_URI, context), "multiRepo.normal.jar");
    }

    protected void updateArchive() throws UpdateException {
        updateRepo(createRepoUpdater(REPO_ARCHIVE, REPO_ARCHIVE_URI, context), "multiRepo.archive.jar");
    }

    protected void updateRepo(RepoUpdater updater, String indexJarPath) throws UpdateException {
        File indexJar = TestUtils.copyResourceToTempFile(indexJarPath);
        try {
            if (updater instanceof IndexV1Updater) {
                JarFile jarFile = new JarFile(indexJar);
                JarEntry indexEntry = (JarEntry) jarFile.getEntry(IndexV1Updater.DATA_FILE_NAME);
                InputStream indexInputStream = jarFile.getInputStream(indexEntry);
                ((IndexV1Updater) updater).processIndexV1(indexInputStream, indexEntry, null);
            } else {
                updater.processDownloadedFile(indexJar);
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } finally {
            if (indexJar != null && indexJar.exists()) {
                indexJar.delete();
            }
        }
    }

}
