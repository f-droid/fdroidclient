package org.fdroid.fdroid.data;

import android.app.Application;
import android.content.ContentValues;
import android.net.Uri;
import android.support.annotation.Nullable;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Schema.RepoTable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

// TODO: Use sdk=24 when Robolectric supports this
@Config(constants = BuildConfig.class, application = Application.class, sdk = 23)
@RunWith(RobolectricGradleTestRunner.class)
public class RepoProviderTest extends FDroidProviderTest {

    private static final String[] COLS = RepoTable.Cols.ALL;

    @Test
    public void findByUrl() {

        Repo fdroidRepo = RepoProvider.Helper.findByAddress(context, "https://f-droid.org/repo");
        Repo fdroidArchiveRepo = RepoProvider.Helper.findByAddress(context, "https://f-droid.org/archive");

        String[] noRepos = {
                "https://not-a-repo.example.com",
                "https://f-droid.org",
                "https://f-droid.org/",
        };

        for (String url : noRepos) {
            assertNull(RepoProvider.Helper.findByUrl(context, Uri.parse(url), COLS));
        }

        String[] fdroidRepoUrls = {
                "https://f-droid.org/repo/index.jar",
                "https://f-droid.org/repo/index.jar?random-junk-in-query=yes",
                "https://f-droid.org/repo/index.jar?random-junk-in-query=yes&more-junk",
                "https://f-droid.org/repo/icons/org.fdroid.fdroid.100.png",
                "https://f-droid.org/repo/icons-640/org.fdroid.fdroid.100.png",
        };

        assertUrlsBelongToRepo(fdroidRepoUrls, fdroidRepo);

        String[] fdroidArchiveUrls = {
                "https://f-droid.org/archive/index.jar",
                "https://f-droid.org/archive/index.jar?random-junk-in-query=yes",
                "https://f-droid.org/archive/index.jar?random-junk-in-query=yes&more-junk",
                "https://f-droid.org/archive/icons/org.fdroid.fdroid.100.png",
                "https://f-droid.org/archive/icons-640/org.fdroid.fdroid.100.png",
        };

        assertUrlsBelongToRepo(fdroidArchiveUrls, fdroidArchiveRepo);
    }

    private void assertUrlsBelongToRepo(String[] urls, Repo expectedRepo) {
        for (String url : urls) {
            Repo actualRepo = RepoProvider.Helper.findByUrl(context, Uri.parse(url), COLS);
            assertNotNull("No repo matching URL " + url, actualRepo);
            assertEquals("Invalid repo for URL [" + url + "]. Expected [" + expectedRepo.address + "] but got [" + actualRepo.address + "]", expectedRepo.id, actualRepo.id);
        }

    }

    /**
     * The {@link DBHelper} class populates four default repos when it first creates a database:
     *  * F-Droid
     *  * F-Droid (Archive)
     *  * Guardian Project
     *  * Guardian Project (Archive)
     * The names/URLs/signing certificates for these repos are all hard coded in the source/res.
     */
    @Test
    public void defaultRepos() {
        List<Repo> defaultRepos = RepoProvider.Helper.all(context);
        assertEquals(defaultRepos.size(), 4);
        assertRepo(
                defaultRepos.get(0),
                context.getString(R.string.fdroid_repo_address),
                context.getString(R.string.fdroid_repo_description),
                Utils.calcFingerprint(context.getString(R.string.fdroid_repo_pubkey)),
                context.getString(R.string.fdroid_repo_name)
        );

        assertRepo(
                defaultRepos.get(1),
                context.getString(R.string.fdroid_archive_address),
                context.getString(R.string.fdroid_archive_description),
                Utils.calcFingerprint(context.getString(R.string.fdroid_archive_pubkey)),
                context.getString(R.string.fdroid_archive_name)
        );

        assertRepo(
                defaultRepos.get(2),
                context.getString(R.string.guardianproject_repo_address),
                context.getString(R.string.guardianproject_repo_description),
                Utils.calcFingerprint(context.getString(R.string.guardianproject_repo_pubkey)),
                context.getString(R.string.guardianproject_repo_name)
        );

        assertRepo(
                defaultRepos.get(3),
                context.getString(R.string.guardianproject_archive_address),
                context.getString(R.string.guardianproject_archive_description),
                Utils.calcFingerprint(context.getString(R.string.guardianproject_archive_pubkey)),
                context.getString(R.string.guardianproject_archive_name)
        );
    }

    @Test
    public void canAddRepo() {

        assertEquals(4, RepoProvider.Helper.all(context).size());

        Repo mock1 = insertRepo(
                "https://mock-repo-1.example.com/fdroid/repo",
                "Just a made up repo",
                "ABCDEF1234567890",
                "Mock Repo 1"
        );

        Repo mock2 = insertRepo(
                "http://mock-repo-2.example.com/fdroid/repo",
                "Mock repo without a name",
                "0123456789ABCDEF"
        );

        assertEquals(6, RepoProvider.Helper.all(context).size());

        assertRepo(
                mock1,
                "https://mock-repo-1.example.com/fdroid/repo",
                "Just a made up repo",
                "ABCDEF1234567890",
                "Mock Repo 1"
        );

        assertRepo(
                mock2,
                "http://mock-repo-2.example.com/fdroid/repo",
                "Mock repo without a name",
                "0123456789ABCDEF",
                "mock-repo-2.example.com/fdroid/repo"
        );
    }

    private static void assertRepo(Repo actualRepo, String expectedAddress, String expectedDescription,
                                   String expectedFingerprint, String expectedName) {
        assertEquals(expectedAddress, actualRepo.address);
        assertEquals(expectedDescription, actualRepo.description);
        assertEquals(expectedFingerprint, actualRepo.fingerprint);
        assertEquals(expectedName, actualRepo.name);
    }

    @Test
    public void canDeleteRepo() {
        Repo mock1 = insertRepo(
                "https://mock-repo-1.example.com/fdroid/repo",
                "Just a made up repo",
                "ABCDEF1234567890",
                "Mock Repo 1"
        );

        Repo mock2 = insertRepo(
                "http://mock-repo-2.example.com/fdroid/repo",
                "Mock repo without a name",
                "0123456789ABCDEF"
        );

        List<Repo> beforeDelete = RepoProvider.Helper.all(context);
        assertEquals(6, beforeDelete.size()); // Expect six repos, because of the four default ones.
        assertEquals(mock1.id, beforeDelete.get(4).id);
        assertEquals(mock2.id, beforeDelete.get(5).id);

        RepoProvider.Helper.remove(context, mock1.getId());

        List<Repo> afterDelete = RepoProvider.Helper.all(context);
        assertEquals(5, afterDelete.size());
        assertEquals(mock2.id, afterDelete.get(4).id);
    }

    protected Repo insertRepo(String address, String description, String fingerprint) {
        return insertRepo(address, description, fingerprint, null);
    }

    protected Repo insertRepo(String address, String description, String fingerprint, @Nullable String name) {
        ContentValues values = new ContentValues();
        values.put(RepoTable.Cols.ADDRESS, address);
        values.put(RepoTable.Cols.DESCRIPTION, description);
        values.put(RepoTable.Cols.FINGERPRINT, fingerprint);
        values.put(RepoTable.Cols.NAME, name);

        RepoProvider.Helper.insert(context, values);
        return RepoProvider.Helper.findByAddress(context, address);
    }
}
