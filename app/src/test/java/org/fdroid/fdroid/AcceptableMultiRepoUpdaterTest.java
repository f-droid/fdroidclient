
package org.fdroid.fdroid;

import android.util.Log;

import org.fdroid.fdroid.RepoUpdater.UpdateException;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.assertEquals;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricGradleTestRunner.class)
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
        if (updateConflicting() && updateMain() && updateArchive()) {
            assertSomewhatAcceptable();
        }
    }

    @Test
    public void testAcceptableConflictingThenArchiveThenMain() throws UpdateException {
        assertEmpty();
        if (updateConflicting() && updateArchive() && updateMain()) {
            assertSomewhatAcceptable();
        }
    }

    @Test
    public void testAcceptableArchiveThenMainThenConflicting() throws UpdateException {
        assertEmpty();
        if (updateArchive() && updateMain() && updateConflicting()) {
            assertSomewhatAcceptable();
        }
    }

    @Test
    public void testAcceptableArchiveThenConflictingThenMain() throws UpdateException {
        assertEmpty();
        if (updateArchive() && updateConflicting() && updateMain()) {
            assertSomewhatAcceptable();
        }
    }

    @Test
    public void testAcceptableMainThenArchiveThenConflicting() throws UpdateException {
        assertEmpty();
        if (updateMain() && updateArchive() && updateConflicting()) {
            assertSomewhatAcceptable();
        }
    }

    @Test
    public void testAcceptableMainThenConflictingThenArchive() throws UpdateException {
        assertEmpty();
        if (updateMain() && updateConflicting() && updateArchive()) {
            assertSomewhatAcceptable();
        }
    }

}
