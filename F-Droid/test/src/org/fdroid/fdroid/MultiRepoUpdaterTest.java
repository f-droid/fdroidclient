
package org.fdroid.fdroid;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockContentResolver;
import android.text.TextUtils;
import android.util.Log;

import org.fdroid.fdroid.RepoUpdater.UpdateException;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.TempApkProvider;
import org.fdroid.fdroid.data.TempAppProvider;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class MultiRepoUpdaterTest extends InstrumentationTestCase {
    private static final String TAG = "RepoUpdaterTest";

    private static final String REPO_MAIN = "Test F-Droid repo";
    private static final String REPO_ARCHIVE = "Test F-Droid repo (Archive)";
    private static final String REPO_CONFLICTING = "Test F-Droid repo with different apps";

    private Context context;
    private RepoUpdater conflictingRepoUpdater;
    private RepoUpdater mainRepoUpdater;
    private RepoUpdater archiveRepoUpdater;
    private File testFilesDir;

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

    public class TestContext extends RenamingDelegatingContext {

        private MockContentResolver resolver;

        public TestContext() {
            super(getInstrumentation().getTargetContext(), "test.");

            resolver = new MockContentResolver();
            resolver.addProvider(AppProvider.getAuthority(), prepareProvider(new AppProvider()));
            resolver.addProvider(ApkProvider.getAuthority(), prepareProvider(new ApkProvider()));
            resolver.addProvider(RepoProvider.getAuthority(), prepareProvider(new RepoProvider()));
            resolver.addProvider(TempAppProvider.getAuthority(), prepareProvider(new TempAppProvider()));
            resolver.addProvider(TempApkProvider.getAuthority(), prepareProvider(new TempApkProvider()));
        }

        private ContentProvider prepareProvider(ContentProvider provider) {
            provider.attachInfo(this, null);
            provider.onCreate();
            return provider;
        }

        @Override
        public File getFilesDir() {
            return getInstrumentation().getTargetContext().getFilesDir();
        }

        /**
         * String resources used during testing (e.g. when bootstraping the database) are from
         * the real org.fdroid.fdroid app, not the test org.fdroid.fdroid.test app.
         */
        @Override
        public Resources getResources() {
            return getInstrumentation().getTargetContext().getResources();
        }

        @Override
        public ContentResolver getContentResolver() {
            return resolver;
        }

        @Override
        public AssetManager getAssets() {
            return getInstrumentation().getContext().getAssets();
        }

        @Override
        public File getDatabasePath(String name) {
            return new File(getInstrumentation().getContext().getFilesDir(), "fdroid_test.db");
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        context = new TestContext();

        testFilesDir = TestUtils.getWriteableDir(getInstrumentation());

        // On a fresh database install, there will be F-Droid + GP repos, including their Archive
        // repos that we are not interested in.
        RepoProvider.Helper.remove(context, 1);
        RepoProvider.Helper.remove(context, 2);
        RepoProvider.Helper.remove(context, 3);
        RepoProvider.Helper.remove(context, 4);

        conflictingRepoUpdater = createUpdater(REPO_CONFLICTING, context);
        mainRepoUpdater = createUpdater(REPO_MAIN, context);
        archiveRepoUpdater = createUpdater(REPO_ARCHIVE, context);
    }

    /**
     * Check that all of the expected apps and apk versions are available in the database. This
     * check will take into account the repository the apks came from, to ensure that each
     * repository indeed contains the apks that it said it would provide.
     */
    private void assertExpected() {
        Log.d(TAG, "Asserting all versions of each .apk are in index.");
        List<Repo> repos = RepoProvider.Helper.all(context);
        assertEquals("Repos", 3, repos.size());

        assertMainRepo(repos);
        assertMainArchiveRepo(repos);
        assertConflictingRepo(repos);
    }

    /**
     *
     */
    private void assertSomewhatAcceptable() {
        Log.d(TAG, "Asserting at least one versions of each .apk is in index.");
        List<Repo> repos = RepoProvider.Helper.all(context);
        assertEquals("Repos", 3, repos.size());

        assertApp2048();
        assertAppAdaway();
        assertAppAdbWireless();
        assertAppIcsImport();
    }

    private void assertApp(String packageName, int[] versionCodes) {
        List<Apk> apks = ApkProvider.Helper.findByPackageName(context, packageName, ApkProvider.DataColumns.ALL);
        assertApksExist(apks, packageName, versionCodes);
    }

    private void assertApp2048() {
        assertApp("com.uberspot.a2048", new int[]{19, 18});
    }

    private void assertAppAdaway() {
        assertApp("org.adaway", new int[]{54, 53, 52, 51, 50, 49, 48, 47, 46, 45, 42, 40, 38, 37, 36, 35});
    }

    private void assertAppAdbWireless() {
        assertApp("siir.es.adbWireless", new int[]{12});
    }

    private void assertAppIcsImport() {
        assertApp("org.dgtale.icsimport", new int[]{3, 2});
    }

    /**
     * + 2048 (com.uberspot.a2048)
     * - Version 1.96 (19)
     * - Version 1.95 (18)
     * + AdAway (org.adaway)
     * - Version 3.0.2 (54)
     * - Version 3.0.1 (53)
     * - Version 3.0 (52)
     * + adbWireless (siir.es.adbWireless)
     * - Version 1.5.4 (12)
     */
    private void assertMainRepo(List<Repo> allRepos) {
        Repo repo = findRepo(REPO_MAIN, allRepos);

        List<Apk> apks = ApkProvider.Helper.findByRepo(context, repo, ApkProvider.DataColumns.ALL);
        assertEquals("Apks for main repo", apks.size(), 6);
        assertApksExist(apks, "com.uberspot.a2048", new int[]{18, 19});
        assertApksExist(apks, "org.adaway", new int[]{52, 53, 54});
        assertApksExist(apks, "siir.es.adbWireless", new int[]{12});
    }

    /**
     * + AdAway (org.adaway)
     * - Version 2.9.2 (51)
     * - Version 2.9.1 (50)
     * - Version 2.9 (49)
     * - Version 2.8.1 (48)
     * - Version 2.8 (47)
     * - Version 2.7 (46)
     * - Version 2.6 (45)
     * - Version 2.3 (42)
     * - Version 2.1 (40)
     * - Version 1.37 (38)
     * - Version 1.36 (37)
     * - Version 1.35 (36)
     * - Version 1.34 (35)
     */
    private void assertMainArchiveRepo(List<Repo> allRepos) {
        Repo repo = findRepo(REPO_ARCHIVE, allRepos);

        List<Apk> apks = ApkProvider.Helper.findByRepo(context, repo, ApkProvider.DataColumns.ALL);
        assertEquals("Apks for main archive repo", 13, apks.size());
        assertApksExist(apks, "org.adaway", new int[]{35, 36, 37, 38, 40, 42, 45, 46, 47, 48, 49, 50, 51});
    }

    /**
     * + AdAway (org.adaway)
     * - Version 3.0.1 (53) *
     * - Version 3.0 (52) *
     * - Version 2.9.2 (51) *
     * - Version 2.2.1 (50) *
     * + Add to calendar (org.dgtale.icsimport)
     * - Version 1.2 (3)
     * - Version 1.1 (2)
     */
    private void assertConflictingRepo(List<Repo> allRepos) {
        Repo repo = findRepo(REPO_CONFLICTING, allRepos);

        List<Apk> apks = ApkProvider.Helper.findByRepo(context, repo, ApkProvider.DataColumns.ALL);
        assertEquals("Apks for main repo", 6, apks.size());
        assertApksExist(apks, "org.adaway", new int[]{50, 51, 52, 53});
        assertApksExist(apks, "org.dgtale.icsimport", new int[]{2, 3});
    }

    @NonNull
    private Repo findRepo(@NonNull String name, List<Repo> allRepos) {
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
    private void assertApksExist(List<Apk> apksToCheck, String appId, int[] versionCodes) {
        for (int versionCode : versionCodes) {
            boolean found = false;
            for (Apk apk : apksToCheck) {
                if (apk.vercode == versionCode && apk.packageName.equals(appId)) {
                    found = true;
                    break;
                }
            }

            assertTrue("Couldn't find app " + appId + ", v" + versionCode, found);
        }
    }

    private void assertEmpty() {
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

    /* At time fo writing, the following tests did not pass. This is because the multi-repo support
       in F-Droid was not sufficient. When working on proper multi repo support than this should be
       ucommented and all these tests should pass:

    public void testCorrectConflictingThenMainThenArchive() throws UpdateException {
        assertEmpty();
        if (updateConflicting() && updateMain() && updateArchive()) {
            assertExpected();
        }
    }

    public void testCorrectConflictingThenArchiveThenMain() throws UpdateException {
        assertEmpty();
        if (updateConflicting() && updateArchive() && updateMain()) {
            assertExpected();
        }
    }

    public void testCorrectArchiveThenMainThenConflicting() throws UpdateException {
        assertEmpty();
        if (updateArchive() && updateMain() && updateConflicting()) {
            assertExpected();
        }
    }

    public void testCorrectArchiveThenConflictingThenMain() throws UpdateException {
        assertEmpty();
        if (updateArchive() && updateConflicting() && updateMain()) {
            assertExpected();
        }
    }

    public void testCorrectMainThenArchiveThenConflicting() throws UpdateException {
        assertEmpty();
        if (updateMain() && updateArchive() && updateConflicting()) {
            assertExpected();
        }
    }

    public void testCorrectMainThenConflictingThenArchive() throws UpdateException {
        assertEmpty();
        if (updateMain() && updateConflicting() && updateArchive()) {
            assertExpected();
        }
    }

    */

    public void testAcceptableConflictingThenMainThenArchive() throws UpdateException {
        assertEmpty();
        if (updateConflicting() && updateMain() && updateArchive()) {
            assertSomewhatAcceptable();
        }
    }

    public void testAcceptableConflictingThenArchiveThenMain() throws UpdateException {
        assertEmpty();
        if (updateConflicting() && updateArchive() && updateMain()) {
            assertSomewhatAcceptable();
        }
    }

    public void testAcceptableArchiveThenMainThenConflicting() throws UpdateException {
        assertEmpty();
        if (updateArchive() && updateMain() && updateConflicting()) {
            assertSomewhatAcceptable();
        }
    }

    public void testAcceptableArchiveThenConflictingThenMain() throws UpdateException {
        assertEmpty();
        if (updateArchive() && updateConflicting() && updateMain()) {
            assertSomewhatAcceptable();
        }
    }

    public void testAcceptableMainThenArchiveThenConflicting() throws UpdateException {
        assertEmpty();
        if (updateMain() && updateArchive() && updateConflicting()) {
            assertSomewhatAcceptable();
        }
    }

    public void testAcceptableMainThenConflictingThenArchive() throws UpdateException {
        assertEmpty();
        if (updateMain() && updateConflicting() && updateArchive()) {
            assertSomewhatAcceptable();
        }
    }

    private RepoUpdater createUpdater(String name, Context context) {
        Repo repo = new Repo();
        repo.pubkey = PUB_KEY;
        repo.address = UUID.randomUUID().toString();
        repo.name = name;

        ContentValues values = new ContentValues(2);
        values.put(RepoProvider.DataColumns.PUBLIC_KEY, repo.pubkey);
        values.put(RepoProvider.DataColumns.ADDRESS, repo.address);
        values.put(RepoProvider.DataColumns.NAME, repo.name);

        RepoProvider.Helper.insert(context, values);

        // Need to reload the repo based on address so that it includes the primary key from
        // the database.
        return new RepoUpdater(context, RepoProvider.Helper.findByAddress(context, repo.address));
    }

    private boolean updateConflicting() throws UpdateException {
        return updateRepo(conflictingRepoUpdater, "multiRepo.conflicting.jar");
    }

    private boolean updateMain() throws UpdateException {
        return updateRepo(mainRepoUpdater, "multiRepo.normal.jar");
    }

    private boolean updateArchive() throws UpdateException {
        return updateRepo(archiveRepoUpdater, "multiRepo.archive.jar");
    }

    private boolean updateRepo(RepoUpdater updater, String indexJarPath) throws UpdateException {
        if (!testFilesDir.canWrite())
            return false;

        File indexJar = TestUtils.copyAssetToDir(context, indexJarPath, testFilesDir);
        updater.processDownloadedFile(indexJar);
        return true;
    }

}
