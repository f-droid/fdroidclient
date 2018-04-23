package org.fdroid.fdroid.data;

import android.app.Application;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.TestUtils;
import org.fdroid.fdroid.data.Schema.AppMetadataTable.Cols;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Config(constants = BuildConfig.class, application = Application.class)
@RunWith(RobolectricTestRunner.class)
public class SuggestedVersionTest extends FDroidProviderTest {

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

    @Test
    public void singleRepoSingleSig() {
        App singleApp = TestUtils.insertApp(
                context, "single.app", "Single App (with beta)", 2, "https://beta.simple.repo", TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, singleApp, 1, TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, singleApp, 2, TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, singleApp, 3, TestUtils.FDROID_SIG);
        TestUtils.updateDbAfterInserting(context);
        assertSuggested("single.app", 2);

        // By enabling unstable updates, the "upstreamVersionCode" should get ignored, and we should
        // suggest the latest version (3).
        Preferences.get().setUnstableUpdates(true);
        assertSuggested("single.app", 3);
    }

    @Test
    public void singleRepoMultiSig() {
        App unrelatedApp = TestUtils.insertApp(context, "noisy.app", "Noisy App", 3, "https://simple.repo",
                TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, unrelatedApp, 3, TestUtils.FDROID_SIG);

        App singleApp = TestUtils.insertApp(context, "single.app", "Single App", 4, "https://simple.repo",
                TestUtils.UPSTREAM_SIG);
        TestUtils.insertApk(context, singleApp, 1, TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, singleApp, 2, TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, singleApp, 3, TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, singleApp, 4, TestUtils.UPSTREAM_SIG);
        TestUtils.insertApk(context, singleApp, 5, TestUtils.UPSTREAM_SIG);
        TestUtils.updateDbAfterInserting(context);

        // Given we aren't installed yet, we don't care which signature.
        // Just get as close to upstreamVersionCode as possible.
        assertSuggested("single.app", 4);

        // Now install v1 with the f-droid signature. In response, we should only suggest
        // apps with that sig in the future. That is, version 4 from upstream is not considered.
        InstalledAppTestUtils.install(context, "single.app", 1, "v1", TestUtils.FDROID_CERT);
        assertSuggested("single.app", 3, TestUtils.FDROID_SIG, 1);

        // This adds the "upstreamVersionCode" version of the app, but signed by f-droid.
        TestUtils.insertApk(context, singleApp, 4, TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, singleApp, 5, TestUtils.FDROID_SIG);
        TestUtils.updateDbAfterInserting(context);
        assertSuggested("single.app", 4, TestUtils.FDROID_SIG, 1);

        // Version 5 from F-Droid is not the "upstreamVersionCode", but with beta updates it should
        // still become the suggested version now.
        Preferences.get().setUnstableUpdates(true);
        assertSuggested("single.app", 5, TestUtils.FDROID_SIG, 1);
    }

    @Test
    public void multiRepoMultiSig() {
        App unrelatedApp = TestUtils.insertApp(context, "noisy.app", "Noisy App", 3, "https://simple.repo",
                TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, unrelatedApp, 3, TestUtils.FDROID_SIG);

        App mainApp = TestUtils.insertApp(context, "single.app", "Single App (Main repo)", 4, "https://main.repo",
                TestUtils.FDROID_SIG);
        App thirdPartyApp = TestUtils.insertApp(
                context, "single.app", "Single App (3rd party)", 4, "https://3rd-party.repo",
                TestUtils.THIRD_PARTY_SIG);

        TestUtils.insertApk(context, mainApp, 1, TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, mainApp, 2, TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, mainApp, 3, TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, mainApp, 4, TestUtils.UPSTREAM_SIG);
        TestUtils.insertApk(context, mainApp, 5, TestUtils.UPSTREAM_SIG);

        TestUtils.insertApk(context, thirdPartyApp, 3, TestUtils.THIRD_PARTY_SIG);
        TestUtils.insertApk(context, thirdPartyApp, 4, TestUtils.THIRD_PARTY_SIG);
        TestUtils.insertApk(context, thirdPartyApp, 5, TestUtils.THIRD_PARTY_SIG);
        TestUtils.insertApk(context, thirdPartyApp, 6, TestUtils.THIRD_PARTY_SIG);
        TestUtils.updateDbAfterInserting(context);

        // Given we aren't installed yet, we don't care which signature or even which repo.
        // Just get as close to upstreamVersionCode as possible.
        assertSuggested("single.app", 4);

        // Now install v1 with the f-droid signature. In response, we should only suggest
        // apps with that sig in the future. That is, version 4 from upstream is not considered.
        InstalledAppTestUtils.install(context, "single.app", 1, "v1", TestUtils.FDROID_CERT);
        assertSuggested("single.app", 3, TestUtils.FDROID_SIG, 1);

        // This adds the "upstreamVersionCode" version of the app, but signed by f-droid.
        TestUtils.insertApk(context, mainApp, 4, TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, mainApp, 5, TestUtils.FDROID_SIG);
        TestUtils.updateDbAfterInserting(context);
        assertSuggested("single.app", 4, TestUtils.FDROID_SIG, 1);

        // Uninstalling the F-Droid build and installing v3 of the third party means we can now go
        // back to suggesting version 4.
        InstalledAppProviderService.deleteAppFromDb(context, "single.app");
        InstalledAppTestUtils.install(context, "single.app", 3, "v3", TestUtils.THIRD_PARTY_CERT);
        assertSuggested("single.app", 4, TestUtils.THIRD_PARTY_SIG, 3);

        // Version 6 from the 3rd party repo is not the "upstreamVersionCode", but with beta updates
        // it should still become the suggested version now.
        Preferences.get().setUnstableUpdates(true);
        assertSuggested("single.app", 6, TestUtils.THIRD_PARTY_SIG, 3);
    }

    /**
     * This is specifically for the {@link AppProvider.Helper#findCanUpdate(android.content.Context, String[])}
     * method used by the {@link org.fdroid.fdroid.UpdateService#showAppUpdatesNotification(List)} method.
     * We need to ensure that we don't prompt people to update to the wrong sig after an update.
     */
    @Test
    public void dontSuggestUpstreamVersions() {
        // By setting the "upstreamVersionCode" to 0, we are letting F-Droid choose the highest compatible version.
        App mainApp = TestUtils.insertApp(context, "single.app", "Single App (Main repo)", 0, "https://main.repo",
                TestUtils.UPSTREAM_SIG);

        TestUtils.insertApk(context, mainApp, 1, TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, mainApp, 2, TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, mainApp, 3, TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, mainApp, 4, TestUtils.FDROID_SIG);
        TestUtils.insertApk(context, mainApp, 5, TestUtils.FDROID_SIG);

        TestUtils.insertApk(context, mainApp, 4, TestUtils.UPSTREAM_SIG);
        TestUtils.insertApk(context, mainApp, 5, TestUtils.UPSTREAM_SIG);
        TestUtils.insertApk(context, mainApp, 6, TestUtils.UPSTREAM_SIG);
        TestUtils.insertApk(context, mainApp, 7, TestUtils.UPSTREAM_SIG);
        TestUtils.updateDbAfterInserting(context);

        // If the user was to manually install the app, they should be suggested version 7 from upstream...
        assertSuggested("single.app", 7);

        // ... but we should not prompt them to update anything, because it isn't installed.
        assertEquals(Collections.EMPTY_LIST, AppProvider.Helper.findCanUpdate(context, Cols.ALL));

        // After installing an early F-Droid version, we should then suggest the latest F-Droid version.
        InstalledAppTestUtils.install(context, "single.app", 2, "v2", TestUtils.FDROID_CERT);
        assertSuggested("single.app", 5, TestUtils.FDROID_SIG, 2);

        // However once we've reached the maximum F-Droid version, then we should not suggest higher versions
        // with different signatures.
        InstalledAppProviderService.deleteAppFromDb(context, "single.app");
        InstalledAppTestUtils.install(context, "single.app", 5, "v5", TestUtils.FDROID_CERT);
        assertEquals(Collections.EMPTY_LIST, AppProvider.Helper.findCanUpdate(context, Cols.ALL));
    }

    /**
     * Same as {@link #assertSuggested(String, int, String, int)} except only for non installed apps.
     *
     * @see #assertSuggested(String, int, String, int)
     */
    private void assertSuggested(String packageName, int suggestedVersion) {
        assertSuggested(packageName, suggestedVersion, null, 0);
    }

    /**
     * Checks that the app exists, that its suggested version code is correct, and that the apk which is "suggested"
     * has the correct signature.
     * <p>
     * If {@param installedSig} is null then {@param installedVersion} is ignored and the signature of the suggested
     * apk is not checked.
     */
    public void assertSuggested(String packageName, int suggestedVersion, String installedSig, int installedVersion) {
        App suggestedApp = AppProvider.Helper.findHighestPriorityMetadata(context.getContentResolver(), packageName);
        assertEquals("Suggested version on App", suggestedVersion, suggestedApp.suggestedVersionCode);
        assertEquals("Installed signature on App", installedSig, suggestedApp.installedSig);

        Apk suggestedApk = ApkProvider.Helper.findSuggestedApk(context, suggestedApp);
        assertEquals("Suggested version on Apk", suggestedVersion, suggestedApk.versionCode);
        if (installedSig != null) {
            assertEquals("Installed signature on Apk", installedSig, suggestedApk.sig);
        }

        List<App> appsToUpdate = AppProvider.Helper.findCanUpdate(context, Schema.AppMetadataTable.Cols.ALL);
        if (installedSig == null) {
            assertEquals("Should not be able to update anything", 0, appsToUpdate.size());
        } else {
            assertEquals("Apps to update", 1, appsToUpdate.size());
            App canUpdateApp = appsToUpdate.get(0);
            assertEquals("Package name of updatable app", packageName, canUpdateApp.packageName);
            assertEquals("Installed version of updatable app", installedVersion, canUpdateApp.installedVersionCode);
            assertEquals("Suggested version to update to", suggestedVersion, canUpdateApp.suggestedVersionCode);
            assertEquals("Installed signature of updatable app", installedSig, canUpdateApp.installedSig);
        }
    }

}
