
package org.fdroid.fdroid;

import android.util.Log;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema;

import java.util.List;

import static org.junit.Assert.assertEquals;

/*
At time fo writing, the following tests did not pass. This is because the multi-repo support
in F-Droid was not sufficient. When working on proper multi repo support than this should be
uncommented and all these tests will be required to pass:

@Config(constants = BuildConfig.class)
@RunWith(RobolectricGradleTestRunner.class)
*/
@SuppressWarnings("unused")
public class ProperMultiRepoUpdaterTest extends MultiRepoUpdaterTest {
    private static final String TAG = "ProperMultiRepoSupport";

    /*@Test
    public void testCorrectConflictingThenMainThenArchive() throws UpdateException {
        assertEmpty();
        if (updateConflicting() && updateMain() && updateArchive()) {
            assertExpected();
        }
    }

    @Test
    public void testCorrectConflictingThenArchiveThenMain() throws UpdateException {
        assertEmpty();
        if (updateConflicting() && updateArchive() && updateMain()) {
            assertExpected();
        }
    }

    @Test
    public void testCorrectArchiveThenMainThenConflicting() throws UpdateException {
        assertEmpty();
        if (updateArchive() && updateMain() && updateConflicting()) {
            assertExpected();
        }
    }

    @Test
    public void testCorrectArchiveThenConflictingThenMain() throws UpdateException {
        assertEmpty();
        if (updateArchive() && updateConflicting() && updateMain()) {
            assertExpected();
        }
    }

    @Test
    public void testCorrectMainThenArchiveThenConflicting() throws UpdateException {
        assertEmpty();
        if (updateMain() && updateArchive() && updateConflicting()) {
            assertExpected();
        }
    }

    @Test
    public void testCorrectMainThenConflictingThenArchive() throws UpdateException {
        assertEmpty();
        if (updateMain() && updateConflicting() && updateArchive()) {
            assertExpected();
        }
    }*/

    /**
     * Check that all of the expected apps and apk versions are available in the database. This
     * check will take into account the repository the apks came from, to ensure that each
     * repository indeed contains the apks that it said it would provide.
     */
    private void assertExpected() {
        Log.i(TAG, "Asserting all versions of each .apk are in index.");
        List<Repo> repos = RepoProvider.Helper.all(context);
        assertEquals("Repos", 3, repos.size());

        assertMainRepo(repos);
        assertMainArchiveRepo(repos);
        assertConflictingRepo(repos);
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

        List<Apk> apks = ApkProvider.Helper.findByRepo(context, repo, Schema.ApkTable.Cols.ALL);
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

        List<Apk> apks = ApkProvider.Helper.findByRepo(context, repo, Schema.ApkTable.Cols.ALL);
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

        List<Apk> apks = ApkProvider.Helper.findByRepo(context, repo, Schema.ApkTable.Cols.ALL);
        assertEquals("Apks for main repo", 6, apks.size());
        assertApksExist(apks, "org.adaway", new int[]{50, 51, 52, 53});
        assertApksExist(apks, "org.dgtale.icsimport", new int[]{2, 3});
    }

}
