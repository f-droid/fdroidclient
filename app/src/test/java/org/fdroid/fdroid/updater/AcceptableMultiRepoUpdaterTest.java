
package org.fdroid.fdroid.updater;

import android.content.ContentValues;
import android.support.annotation.NonNull;
import android.util.Log;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.RepoUpdater.UpdateException;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema.RepoTable.Cols;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricTestRunner.class)
public class AcceptableMultiRepoUpdaterTest extends MultiRepoUpdaterTest {
    private static final String TAG = "AcceptableMultiRepoTest";

    private void assertSomewhatAcceptable() {
        Log.i(TAG, "Asserting at least one versions of each .apk is in index.");
        List<Repo> repos = RepoProvider.Helper.all(context);
        assertEquals("Repos", 3, repos.size());

        assertApp2048();
        assertAppAdaway();
        assertAppAdbWireless();
        assertAppIcsImport();
    }

    @Test
    public void testAcceptableConflictingThenMainThenArchive() throws UpdateException {
        assertEmpty();

        updateConflicting();
        updateMain();
        updateArchive();

        assertSomewhatAcceptable();
    }

    @Test
    public void testAcceptableConflictingThenArchiveThenMain() throws UpdateException {
        assertEmpty();

        updateConflicting();
        updateArchive();
        updateMain();

        assertSomewhatAcceptable();
    }

    @Test
    public void testAcceptableArchiveThenMainThenConflicting() throws UpdateException {
        assertEmpty();

        updateArchive();
        updateMain();
        updateConflicting();

        assertSomewhatAcceptable();
    }

    @Test
    public void testAcceptableArchiveThenConflictingThenMain() throws UpdateException {
        assertEmpty();

        updateArchive();
        updateConflicting();
        updateMain();

        assertSomewhatAcceptable();
    }

    @Test
    public void testAcceptableMainThenArchiveThenConflicting() throws UpdateException {
        assertEmpty();

        updateMain();
        updateArchive();
        updateConflicting();

        assertSomewhatAcceptable();
    }

    @Test
    public void testAcceptableMainThenConflictingThenArchive() throws UpdateException {
        assertEmpty();

        updateMain();
        updateConflicting();
        updateArchive();

        assertSomewhatAcceptable();
    }

    @NonNull
    private Repo getMainRepo() {
        Repo repo = RepoProvider.Helper.findByAddress(context, REPO_MAIN_URI);
        assertNotNull(repo);
        return repo;
    }

    @NonNull
    private Repo getArchiveRepo() {
        Repo repo = RepoProvider.Helper.findByAddress(context, REPO_ARCHIVE_URI);
        assertNotNull(repo);
        return repo;
    }

    @NonNull
    private Repo getConflictingRepo() {
        Repo repo = RepoProvider.Helper.findByAddress(context, REPO_CONFLICTING_URI);
        assertNotNull(repo);
        return repo;
    }

    @Test
    public void testOrphanedApps() throws UpdateException {
        assertEmpty();

        updateArchive();
        updateMain();
        updateConflicting();

        assertSomewhatAcceptable();

        disableRepo(getArchiveRepo());
        disableRepo(getMainRepo());
        disableRepo(getConflictingRepo());

        RepoProvider.Helper.purgeApps(context, getArchiveRepo());
        RepoProvider.Helper.purgeApps(context, getMainRepo());
        RepoProvider.Helper.purgeApps(context, getConflictingRepo());

        assertEmpty();
    }

    private void disableRepo(Repo repo) {
        ContentValues values = new ContentValues(1);
        values.put(Cols.IN_USE, 0);
        RepoProvider.Helper.update(context, repo, values);
    }

}
