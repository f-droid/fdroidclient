package org.fdroid.fdroid.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.fdroid.database.AppPrefs;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Config(application = Application.class)
@RunWith(RobolectricTestRunner.class)
public class SuggestedVersionTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setup() {
        Preferences.setupForTests(context);
    }

    @Test
    public void singleRepoSingleSig() {
        App singleApp = TestUtils.getApp();
        singleApp.installedVersionCode = 1;
        singleApp.installedSigner = TestUtils.FDROID_SIGNER;
        Apk apk1 = TestUtils.getApk(1, TestUtils.FDROID_SIGNER, Apk.RELEASE_CHANNEL_STABLE);
        Apk apk2 = TestUtils.getApk(2, TestUtils.FDROID_SIGNER, Apk.RELEASE_CHANNEL_STABLE);
        Apk apk3 = TestUtils.getApk(3, TestUtils.FDROID_SIGNER, Apk.RELEASE_CHANNEL_BETA);
        List<Apk> apks = new ArrayList<>();
        apks.add(apk3);
        apks.add(apk2);
        apks.add(apk1);
        assertSuggested(singleApp, apks, 2, Apk.RELEASE_CHANNEL_STABLE);

        // By enabling the beta channel we should suggest the latest version (3).
        Preferences.get().setUnstableUpdates(true);
        assertSuggested(singleApp, apks, 3, Apk.RELEASE_CHANNEL_BETA);
    }

    @Test
    public void singleRepoMultiSigner() {
        App singleApp = TestUtils.getApp();
        singleApp.installedVersionCode = 0;

        Apk apk1 = TestUtils.getApk(1, TestUtils.FDROID_SIGNER, Apk.RELEASE_CHANNEL_STABLE);
        Apk apk2 = TestUtils.getApk(2, TestUtils.FDROID_SIGNER, Apk.RELEASE_CHANNEL_STABLE);
        Apk apk3 = TestUtils.getApk(3, TestUtils.FDROID_SIGNER, Apk.RELEASE_CHANNEL_STABLE);
        Apk apk4 = TestUtils.getApk(4, TestUtils.UPSTREAM_SIGNER, Apk.RELEASE_CHANNEL_STABLE);
        Apk apk5 = TestUtils.getApk(5, TestUtils.UPSTREAM_SIGNER, Apk.RELEASE_CHANNEL_BETA);
        List<Apk> apks = new ArrayList<>();
        apks.add(apk5);
        apks.add(apk4);
        apks.add(apk3);
        apks.add(apk2);
        apks.add(apk1);

        // Given we aren't installed yet, we don't care which signature.
        // Just get as close to suggestedVersionCode as possible.
        assertSuggested(singleApp, apks, 4, Apk.RELEASE_CHANNEL_STABLE, false);

        // Now install v1 with the f-droid signature. In response, we should only suggest
        // apps with that sig in the future. That is, version 4 from upstream is not considered.
        singleApp.installedSigner = TestUtils.FDROID_SIGNER;
        singleApp.installedVersionCode = 1;
        assertSuggested(singleApp, apks, 3, Apk.RELEASE_CHANNEL_STABLE);

        // This adds the "suggestedVersionCode" version of the app, but signed by f-droid.
        Apk apk4f = TestUtils.getApk(4, TestUtils.FDROID_SIGNER, Apk.RELEASE_CHANNEL_STABLE);
        Apk apk5f = TestUtils.getApk(5, TestUtils.FDROID_SIGNER, Apk.RELEASE_CHANNEL_BETA);
        apks.clear();
        apks.add(apk5);
        apks.add(apk5f);
        apks.add(apk4);
        apks.add(apk4f);
        apks.add(apk3);
        apks.add(apk2);
        apks.add(apk1);
        assertSuggested(singleApp, apks, 4, Apk.RELEASE_CHANNEL_STABLE);

        // Version 5 from F-Droid is not the "suggestedVersionCode", but with beta updates it should
        // still become the suggested version now.
        assertSuggested(singleApp, apks, 5, Apk.RELEASE_CHANNEL_BETA);
    }

    public void assertSuggested(App app, List<Apk> apks, int suggestedVersion,
                                String releaseChannel) {
        assertSuggested(app, apks, suggestedVersion, releaseChannel, true);
    }

    @Test
    public void testIncompatibleWithBeta() {
        App singleApp = TestUtils.getApp();
        singleApp.installedVersionCode = 1;
        singleApp.installedSigner = TestUtils.FDROID_SIGNER;
        Apk apk1 = TestUtils.getApk(1, TestUtils.FDROID_SIGNER, Apk.RELEASE_CHANNEL_STABLE);
        Apk apk2 = TestUtils.getApk(2, TestUtils.FDROID_SIGNER, Apk.RELEASE_CHANNEL_STABLE);
        Apk apk3 = TestUtils.getApk(3, TestUtils.FDROID_SIGNER, Apk.RELEASE_CHANNEL_STABLE);
        apk3.compatible = false;
        List<Apk> apks = new ArrayList<>();
        apks.add(apk3);
        apks.add(apk2);
        apks.add(apk1);
        assertSuggested(singleApp, apks, 2, Apk.RELEASE_CHANNEL_BETA);
    }

    /**
     * Checks that the app exists, that its suggested version code is correct, and that the apk which is "suggested"
     * has the correct signature.
     * <p>
     * If {@param installedSigner} is null then {@param installedVersion} is
     * ignored and the signature of the suggested APK is not checked.
     */
    public void assertSuggested(App app, List<Apk> apks, int suggestedVersion,
                                String releaseChannel, boolean hasUpdates) {
        Apk suggestedApk = app.findSuggestedApk(apks, releaseChannel);
        assertNotNull(suggestedApk);
        assertEquals("Suggested version on App", suggestedVersion, suggestedApk.versionCode);

        if (app.installedSigner != null) {
            assertEquals("Installed signature on Apk", app.installedSigner, suggestedApk.signer);
        }
        assertTrue(app.canAndWantToUpdate(suggestedApk));
        AppPrefs appPrefs = new AppPrefs(app.packageName, 0, Collections.singletonList(releaseChannel));
        assertEquals(hasUpdates, app.hasUpdates(apks, appPrefs));
    }

}
