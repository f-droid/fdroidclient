
package org.fdroid.fdroid.updater;

import android.support.annotation.StringDef;
import android.util.Log;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.RepoUpdater;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

// TODO: Use sdk=24 when Robolectric supports this
@Config(constants = BuildConfig.class, sdk = 23)
@RunWith(RobolectricGradleTestRunner.class)
public class ProperMultiRepoUpdaterTest extends MultiRepoUpdaterTest {
    private static final String TAG = "ProperMultiRepoSupport";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({"Conflicting", "Normal"})
    public @interface RepoIdentifier {}

    @Test
    public void mainRepo() throws RepoUpdater.UpdateException {
        assertEmpty();
        updateMain();
        assertMainRepo(RepoProvider.Helper.all(context));
    }

    @Test
    public void archiveRepo() throws RepoUpdater.UpdateException {
        assertEmpty();
        updateArchive();
        assertMainArchiveRepo(RepoProvider.Helper.all(context));
    }

    @Test
    public void conflictingRepo() throws RepoUpdater.UpdateException {
        assertEmpty();
        updateConflicting();
        assertConflictingRepo(RepoProvider.Helper.all(context));
    }

    @Test
    public void testCorrectConflictingThenMainThenArchive() throws RepoUpdater.UpdateException {
        assertEmpty();

        updateConflicting();
        updateMain();
        updateArchive();

        assertExpected();
    }

    @Test
    public void testCorrectConflictingThenArchiveThenMain() throws RepoUpdater.UpdateException {
        assertEmpty();

        updateConflicting();
        updateArchive();
        updateMain();

        assertExpected();
    }

    @Test
    public void testCorrectArchiveThenMainThenConflicting() throws RepoUpdater.UpdateException {
        assertEmpty();

        updateArchive();
        updateMain();
        updateConflicting();

        assertExpected();
    }

    @Test
    public void testCorrectArchiveThenConflictingThenMain() throws RepoUpdater.UpdateException {
        assertEmpty();

        updateArchive();
        updateConflicting();
        updateMain();

        assertExpected();
    }

    @Test
    public void testCorrectMainThenArchiveThenConflicting() throws RepoUpdater.UpdateException {
        assertEmpty();

        updateMain();
        updateArchive();
        updateConflicting();

        assertExpected();
    }

    @Test
    public void testCorrectMainThenConflictingThenArchive() throws RepoUpdater.UpdateException {
        assertEmpty();

        updateMain();
        updateConflicting();
        updateArchive();

        assertExpected();
    }

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

        assert2048(repo, "Normal");
        assertAdAway(repo, "Normal");
        assertAdb(repo, "Normal");
    }

    /**
     * @param id An identifier that we've put in the metadata for each repo to ensure that
     *           we can identify the metadata is coming from the correct repo.
     */
    private void assert2048(Repo repo, @RepoIdentifier String id) {
        App a2048 = AppProvider.Helper.findByPackageName(context.getContentResolver(), "com.uberspot.a2048", repo.getId());
        assertNotNull(a2048);
        assertEquals("2048", a2048.name);
        assertEquals(String.format("<p>2048 from %s repo.</p>", id), a2048.description);
        assertEquals(String.format("Puzzle game (%s)", id), a2048.summary);
        assertEquals(String.format("https://github.com/uberspot/2048-android?%s", id), a2048.webURL);
        assertEquals(String.format("https://github.com/uberspot/2048-android?code&%s", id), a2048.sourceURL);
        assertEquals(String.format("https://github.com/uberspot/2048-android/issues?%s", id), a2048.trackerURL);
    }

    /** @see ProperMultiRepoUpdaterTest#assert2048(Repo, String) */
    private void assertAdAway(Repo repo, @RepoIdentifier String id) {
        App adaway = AppProvider.Helper.findByPackageName(context.getContentResolver(), "org.adaway", repo.getId());
        assertNotNull(adaway);
        assertEquals(String.format("AdAway", id), adaway.name);
        assertEquals(String.format("<p>AdAway from %s repo.</p>", id), adaway.description);
        assertEquals(String.format("Block advertisements (%s)", id), adaway.summary);
        assertEquals(String.format("http://sufficientlysecure.org/index.php/adaway?%s", id), adaway.webURL);
        assertEquals(String.format("https://github.com/dschuermann/ad-away?%s", id), adaway.sourceURL);
        assertEquals(String.format("https://github.com/dschuermann/ad-away/issues?%s", id), adaway.trackerURL);
        assertEquals(String.format("https://github.com/dschuermann/ad-away/raw/HEAD/CHANGELOG?%s", id), adaway.changelogURL);
        assertEquals(String.format("http://sufficientlysecure.org/index.php/adaway?%s", id), adaway.donateURL);
        assertEquals(String.format("369138", id), adaway.flattrID);
    }

    /** @see ProperMultiRepoUpdaterTest#assert2048(Repo, String) */
    private void assertAdb(Repo repo, @RepoIdentifier String id) {
        App adb = AppProvider.Helper.findByPackageName(context.getContentResolver(), "siir.es.adbWireless", repo.getId());
        assertNotNull(adb);
        assertEquals("adbWireless", adb.name);
        assertEquals(String.format("<p>adbWireless from %s repo.</p>", id), adb.description);
        assertEquals(String.format("Wireless adb (%s)", id), adb.summary);
        assertEquals(String.format("https://adbwireless.example.com?%s", id), adb.webURL);
        assertEquals(String.format("https://adbwireless.example.com/source?%s", id), adb.sourceURL);
        assertEquals(String.format("https://adbwireless.example.com/issues?%s", id), adb.trackerURL);
    }

    /** @see ProperMultiRepoUpdaterTest#assert2048(Repo, String) */
    private void assertCalendar(Repo repo, @RepoIdentifier String id) {
        App calendar = AppProvider.Helper.findByPackageName(context.getContentResolver(), "org.dgtale.icsimport", repo.getId());
        assertNotNull(calendar);
        assertEquals("Add to calendar", calendar.name);
        assertEquals(String.format("<p>Add to calendar from %s repo.</p>", id), calendar.description);
        assertEquals(String.format("Import .ics files into calendar (%s)", id), calendar.summary);
        assertEquals(String.format("https://github.com/danielegobbetti/ICSImport/blob/HEAD/README.md?%s", id), calendar.webURL);
        assertEquals(String.format("https://github.com/danielegobbetti/ICSImport?%s", id), calendar.sourceURL);
        assertEquals(String.format("https://github.com/danielegobbetti/ICSImport/issues?%s", id), calendar.trackerURL);
        assertEquals("2225390", calendar.flattrID);
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

        assertAdAway(repo, "Normal");
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
        assertEquals("Apks for conflicting repo", 6, apks.size());
        assertApksExist(apks, "org.adaway", new int[]{50, 51, 52, 53});
        assertApksExist(apks, "org.dgtale.icsimport", new int[]{2, 3});

        assertAdAway(repo, "Conflicting");
        assertCalendar(repo, "Conflicting");
    }

}
