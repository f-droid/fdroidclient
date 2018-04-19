package org.fdroid.fdroid.data;

import android.app.Application;
import android.content.Context;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@Config(constants = BuildConfig.class, application = Application.class)
@RunWith(RobolectricTestRunner.class)
public class PreferredSignatureTest extends FDroidProviderTest {

    private static final String PACKAGE_NAME = "app.example.com";

    @Before
    public void setup() {
        TestUtils.registerContentProvider(AppProvider.getAuthority(), AppProvider.class);
        Preferences.setupForTests(context);

        // This is what the FDroidApp does when this preference is changed. Need to also do this under testing.
        Preferences.get().registerUnstableUpdatesChangeListener(new Preferences.ChangeListener() {
            @Override
            public void onPreferenceChange() {
                AppProvider.Helper.calcSuggestedApks(context);
            }
        });
    }

    private Repo createFDroidRepo() {
        return RepoProviderTest.insertRepo(context, "https://f-droid.org/fdroid/repo", "", "", "");
    }

    private App populateFDroidRepo(Repo repo) {
        App app = TestUtils.insertApp(context, PACKAGE_NAME, "App", 3100, repo, TestUtils.UPSTREAM_SIG);

        TestUtils.insertApk(context, app, 1100, TestUtils.FDROID_SIG); // 1.0
        TestUtils.insertApk(context, app, 2100, TestUtils.FDROID_SIG); // 2.0
        TestUtils.insertApk(context, app, 3100, TestUtils.FDROID_SIG); // 3.0

        TestUtils.insertApk(context, app, 2100, TestUtils.UPSTREAM_SIG); // 2.0
        TestUtils.insertApk(context, app, 3100, TestUtils.UPSTREAM_SIG); // 3.0

        TestUtils.updateDbAfterInserting(context);

        return app;
    }

    private Repo createDevRepo() {
        return RepoProviderTest.insertRepo(context, "https://dev.upstream.com/fdroid/repo", "", "", "");
    }

    private App populateDevRepo(Repo repo) {
        App app = TestUtils.insertApp(context, PACKAGE_NAME, "App", 4100, repo, TestUtils.THIRD_PARTY_SIG);

        TestUtils.insertApk(context, app, 1001, TestUtils.THIRD_PARTY_SIG); // 1.0-rc2
        TestUtils.insertApk(context, app, 1100, TestUtils.THIRD_PARTY_SIG); // 1.0
        TestUtils.insertApk(context, app, 2001, TestUtils.THIRD_PARTY_SIG); // 2.0-rc1
        TestUtils.insertApk(context, app, 2002, TestUtils.THIRD_PARTY_SIG); // 2.0-rc2
        TestUtils.insertApk(context, app, 2100, TestUtils.THIRD_PARTY_SIG); // 2.0
        TestUtils.insertApk(context, app, 3001, TestUtils.THIRD_PARTY_SIG); // 3.0-rc1
        TestUtils.insertApk(context, app, 3100, TestUtils.THIRD_PARTY_SIG); // 3.0
        TestUtils.insertApk(context, app, 4001, TestUtils.THIRD_PARTY_SIG); // 4.0-rc1
        TestUtils.insertApk(context, app, 4002, TestUtils.THIRD_PARTY_SIG); // 4.0-rc2
        TestUtils.insertApk(context, app, 4100, TestUtils.THIRD_PARTY_SIG); // 4.0
        TestUtils.insertApk(context, app, 5001, TestUtils.THIRD_PARTY_SIG); // 5.0-rc1
        TestUtils.insertApk(context, app, 5002, TestUtils.THIRD_PARTY_SIG); // 5.0-rc2
        TestUtils.insertApk(context, app, 5003, TestUtils.THIRD_PARTY_SIG); // 5.0-rc3

        TestUtils.updateDbAfterInserting(context);

        return app;
    }

    private Repo createUpstreamRepo() {
        return RepoProviderTest.insertRepo(context, "https://upstream.com/fdroid/repo", "", "", "");
    }

    private App populateUpstreamRepo(Repo repo) {
        App app = TestUtils.insertApp(context, PACKAGE_NAME, "App", 4100, repo, TestUtils.UPSTREAM_SIG);

        TestUtils.insertApk(context, app, 2100, TestUtils.UPSTREAM_SIG);
        TestUtils.insertApk(context, app, 3100, TestUtils.UPSTREAM_SIG);
        TestUtils.insertApk(context, app, 4100, TestUtils.UPSTREAM_SIG);

        TestUtils.updateDbAfterInserting(context);

        return app;
    }

    @Test
    public void onlyFDroid() {
        populateFDroidRepo(createFDroidRepo());
        assertSuggested(context, 3100, TestUtils.UPSTREAM_SIG);
    }

    /**
     * @see #assertFdroidThenDev()
     */
    @Test
    public void fdroidThenDev1() {
        Repo fdroid = createFDroidRepo();
        Repo dev = createDevRepo();

        populateFDroidRepo(fdroid);
        populateDevRepo(dev);

        assertFdroidThenDev();
    }

    /**
     * @see #assertFdroidThenDev()
     */
    @Test
    public void fdroidThenDev2() {
        Repo fdroid = createFDroidRepo();
        Repo dev = createDevRepo();

        populateDevRepo(dev);
        populateFDroidRepo(fdroid);

        assertFdroidThenDev();
    }

    /**
     * Both {@link #fdroidThenDev1()} and {@link #fdroidThenDev2()} add the same repos, with the same priorities and
     * the same apps/apks. The only difference is in the order with which they get added to the database. They both
     * then delegate here and assert that everything works as expected. The reason for testing like this is to ensure
     * that the order of rows in the database has no bearing on the correct suggestions of signatures.
     * @see #fdroidThenDev1()
     * @see #fdroidThenDev2()
     */
    private void assertFdroidThenDev() {
        assertSuggested(context, 4100, TestUtils.THIRD_PARTY_SIG);

        Preferences.get().setUnstableUpdates(true);
        assertSuggested(context, 5003, TestUtils.THIRD_PARTY_SIG);

        Preferences.get().setUnstableUpdates(false);
        assertSuggested(context, 4100, TestUtils.THIRD_PARTY_SIG);
    }

    /**
     * @see #assertFdroidThenUpstream()
     */
    @Test
    public void fdroidThenUpstream1() {
        Repo fdroid = createFDroidRepo();
        Repo upstream = createUpstreamRepo();

        populateUpstreamRepo(upstream);
        populateFDroidRepo(fdroid);

        assertFdroidThenUpstream();
    }

    /**
     * @see #assertFdroidThenUpstream()
     */
    @Test
    public void fdroidThenUpstream2() {
        Repo fdroid = createFDroidRepo();
        Repo upstream = createUpstreamRepo();

        populateFDroidRepo(fdroid);
        populateUpstreamRepo(upstream);

        assertFdroidThenUpstream();
    }

    /**
     * @see #fdroidThenUpstream1()
     * @see #fdroidThenUpstream2()
     * @see #assertFdroidThenDev()
     */
    private void assertFdroidThenUpstream() {
        assertSuggested(context, 4100, TestUtils.UPSTREAM_SIG);
    }

    /**
     * @see #assertFdroidThenUpstreamThenDev()
     */
    @Test
    public void fdroidThenUpstreamThenDev1() {
        Repo fdroid = createFDroidRepo();
        Repo upstream = createUpstreamRepo();
        Repo dev = createDevRepo();

        populateFDroidRepo(fdroid);
        populateUpstreamRepo(upstream);
        populateDevRepo(dev);

        assertFdroidThenUpstreamThenDev();
    }

    /**
     * @see #assertFdroidThenUpstreamThenDev()
     */
    @Test
    public void fdroidThenUpstreamThenDev2() {
        Repo fdroid = createFDroidRepo();
        Repo upstream = createUpstreamRepo();
        Repo dev = createDevRepo();

        populateDevRepo(dev);
        populateUpstreamRepo(upstream);
        populateFDroidRepo(fdroid);

        assertFdroidThenUpstreamThenDev();
    }

    /**
     * @see #fdroidThenUpstreamThenDev1()
     * @see #fdroidThenUpstreamThenDev2()
     * @see #assertFdroidThenDev()
     */
    private void assertFdroidThenUpstreamThenDev() {
        assertSuggested(context, 4100, TestUtils.THIRD_PARTY_SIG);

        Preferences.get().setUnstableUpdates(true);
        assertSuggested(context, 5003, TestUtils.THIRD_PARTY_SIG);

        Preferences.get().setUnstableUpdates(false);
        assertSuggested(context, 4100, TestUtils.THIRD_PARTY_SIG);
    }

    /**
     * @see #assertFdroidThenDevThenUpstream()
     */
    @Test
    public void fdroidThenDevThenUpstream1() {
        Repo fdroid = createFDroidRepo();
        Repo dev = createDevRepo();
        Repo upstream = createUpstreamRepo();

        populateFDroidRepo(fdroid);
        populateDevRepo(dev);
        populateUpstreamRepo(upstream);

        assertFdroidThenDevThenUpstream();
    }

    /**
     * @see #assertFdroidThenDevThenUpstream()
     */
    @Test
    public void fdroidThenDevThenUpstream2() {
        Repo fdroid = createFDroidRepo();
        Repo dev = createDevRepo();
        Repo upstream = createUpstreamRepo();

        populateFDroidRepo(fdroid);
        populateDevRepo(dev);
        populateUpstreamRepo(upstream);

        assertFdroidThenDevThenUpstream();
    }

    /**
     * @see #fdroidThenDevThenUpstream1()
     * @see #fdroidThenDevThenUpstream2()
     * @see #assertFdroidThenDev()
     */
    private void assertFdroidThenDevThenUpstream() {
        assertSuggested(context, 4100, TestUtils.UPSTREAM_SIG);
    }

    private void assertSuggested(Context context, int suggestedVersion, String suggestedSig) {
        App suggestedApp = AppProvider.Helper.findHighestPriorityMetadata(context.getContentResolver(), PACKAGE_NAME);
        assertEquals("Suggested version on App", suggestedVersion, suggestedApp.suggestedVersionCode);

        Apk suggestedApk = ApkProvider.Helper.findSuggestedApk(context, suggestedApp);
        assertEquals("Version on suggested Apk", suggestedVersion, suggestedApk.versionCode);
        TestUtils.assertSignaturesMatch("Signature on suggested Apk", suggestedSig, suggestedApk.sig);
    }

}
