package org.fdroid.fdroid.panic;

import org.fdroid.fdroid.data.DBHelper;
import org.fdroid.fdroid.data.FDroidProviderTest;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.RepoProviderTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PanicResponderActivityTest extends FDroidProviderTest {

    /**
     * The {@link DBHelper} class populates the default repos when it first creates a database.
     * The names/URLs/signing certificates for these repos are all hard coded in the source/res.
     */
    @Test
    public void defaultRepos() {
        int defaultRepoCount = RepoProviderTest.getDefaultRepoCount(context);

        List<Repo> defaultRepos = RepoProvider.Helper.all(context);
        assertEquals(defaultRepos.size(), defaultRepoCount);

        Repo gpRepo = RepoProvider.Helper.findByAddress(context, "https://guardianproject.info/fdroid/repo");
        setEnabled(gpRepo, true);
        assertEquals(2, RepoProvider.Helper.countEnabledRepos(context));

        PanicResponderActivity.resetRepos(context);
        assertEquals(1, RepoProvider.Helper.countEnabledRepos(context));
        defaultRepos = RepoProvider.Helper.all(context);
        assertEquals(defaultRepoCount, defaultRepos.size());

        RepoProviderTest.insertRepo(
                context,
                "https://mock-repo-1.example.com/fdroid/repo",
                "Just a made up repo",
                "ABCDEF1234567890",
                "Mock Repo 1"
        );
        defaultRepos = RepoProvider.Helper.all(context);
        assertEquals(defaultRepoCount + 1, defaultRepos.size());
        assertEquals(2, RepoProvider.Helper.countEnabledRepos(context));

        PanicResponderActivity.resetRepos(context);
        defaultRepos = RepoProvider.Helper.all(context);
        assertEquals(defaultRepoCount, defaultRepos.size());
        assertEquals(1, RepoProvider.Helper.countEnabledRepos(context));
    }
}
