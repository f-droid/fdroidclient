package org.fdroid.fdroid.data;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.TestUtils;
import org.fdroid.fdroid.data.Schema.InstalledAppTable;
import org.fdroid.fdroid.mock.MockApk;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.ArrayList;
import java.util.List;

import static org.fdroid.fdroid.Assert.assertInvalidUri;
import static org.fdroid.fdroid.Assert.assertValidUri;

// TODO: Use sdk=24 when Robolectric supports this
@Config(constants = BuildConfig.class, sdk = 23)
@RunWith(RobolectricGradleTestRunner.class)
public class ProviderUriTests {

    private ShadowContentResolver resolver;

    @Before
    public void setup() {
        resolver = Shadows.shadowOf(RuntimeEnvironment.application.getContentResolver());
    }

    @After
    public void teardown() {
        FDroidProvider.clearDbHelperSingleton();
    }

    @Test
    public void invalidInstalledAppProviderUris() {
        ShadowContentResolver.registerProvider(InstalledAppProvider.getAuthority(), new InstalledAppProvider());
        assertInvalidUri(resolver, InstalledAppProvider.getAuthority());
        assertInvalidUri(resolver, "blah");
    }

    @Test
    public void validInstalledAppProviderUris() {
        ShadowContentResolver.registerProvider(InstalledAppProvider.getAuthority(), new InstalledAppProvider());
        String[] projection = new String[] {InstalledAppTable.Cols._ID};
        assertValidUri(resolver, InstalledAppProvider.getContentUri(), projection);
        assertValidUri(resolver, InstalledAppProvider.getAppUri("org.example.app"), projection);
        assertValidUri(resolver, InstalledAppProvider.getSearchUri("blah"), projection);
        assertValidUri(resolver, InstalledAppProvider.getSearchUri("\"blah\""), projection);
        assertValidUri(resolver, InstalledAppProvider.getSearchUri("blah & sneh"), projection);
        assertValidUri(resolver, InstalledAppProvider.getSearchUri("http://blah.example.com?sneh=\"sneh\""), projection);
    }

    @Test
    public void invalidRepoProviderUris() {
        ShadowContentResolver.registerProvider(RepoProvider.getAuthority(), new RepoProvider());
        assertInvalidUri(resolver, RepoProvider.getAuthority());
        assertInvalidUri(resolver, "blah");
    }

    @Test
    public void validRepoProviderUris() {
        ShadowContentResolver.registerProvider(RepoProvider.getAuthority(), new RepoProvider());
        String[] projection = new String[] {Schema.RepoTable.Cols._ID};
        assertValidUri(resolver, RepoProvider.getContentUri(), projection);
        assertValidUri(resolver, RepoProvider.getContentUri(10000L), projection);
        assertValidUri(resolver, RepoProvider.allExceptSwapUri(), projection);
    }

    @Test
    public void invalidAppProviderUris() {
        ShadowContentResolver.registerProvider(AppProvider.getAuthority(), new AppProvider());
        assertInvalidUri(resolver, AppProvider.getAuthority());
        assertInvalidUri(resolver, "blah");
    }

    @Test
    public void validAppProviderUris() {
        ShadowContentResolver.registerProvider(AppProvider.getAuthority(), new AppProvider());
        String[] projection = new String[] {Schema.AppMetadataTable.Cols._ID};
        assertValidUri(resolver, AppProvider.getContentUri(), "content://org.fdroid.fdroid.data.AppProvider", projection);
        assertValidUri(resolver, AppProvider.getSearchUri("'searching!'"), "content://org.fdroid.fdroid.data.AppProvider/search/'searching!'", projection);
        assertValidUri(resolver, AppProvider.getSearchUri("/"), "content://org.fdroid.fdroid.data.AppProvider/search/%2F", projection);
        assertValidUri(resolver, AppProvider.getSearchUri(""), "content://org.fdroid.fdroid.data.AppProvider", projection);
        assertValidUri(resolver, AppProvider.getSearchUri(null), "content://org.fdroid.fdroid.data.AppProvider", projection);
        assertValidUri(resolver, AppProvider.getNoApksUri(), "content://org.fdroid.fdroid.data.AppProvider/noApks", projection);
        assertValidUri(resolver, AppProvider.getInstalledUri(), "content://org.fdroid.fdroid.data.AppProvider/installed", projection);
        assertValidUri(resolver, AppProvider.getCanUpdateUri(), "content://org.fdroid.fdroid.data.AppProvider/canUpdate", projection);

        App app = new App();
        app.packageName = "org.fdroid.fdroid";

        assertValidUri(resolver, AppProvider.getContentUri(app), "content://org.fdroid.fdroid.data.AppProvider/org.fdroid.fdroid", projection);
        assertValidUri(resolver, AppProvider.getContentUri("org.fdroid.fdroid"), "content://org.fdroid.fdroid.data.AppProvider/org.fdroid.fdroid", projection);
    }

    @Test
    public void validTempAppProviderUris() {
        ShadowContentResolver.registerProvider(TempAppProvider.getAuthority(), new TempAppProvider());
        String[] projection = new String[]{Schema.AppMetadataTable.Cols._ID};

        // Required so that the `assertValidUri` calls below will indeed have a real temp_fdroid_app
        // table to query.
        TempAppProvider.Helper.init(TestUtils.createContextWithContentResolver(resolver));

        List<String> packageNames = new ArrayList<>(2);
        packageNames.add("org.fdroid.fdroid");
        packageNames.add("com.example.com");

        assertValidUri(resolver, TempAppProvider.getAppsUri(packageNames), "content://org.fdroid.fdroid.data.TempAppProvider/apps/org.fdroid.fdroid%2Ccom.example.com", projection);
        assertValidUri(resolver, TempAppProvider.getContentUri(), "content://org.fdroid.fdroid.data.TempAppProvider", projection);
    }

    @Test
    public void invalidApkProviderUris() {
        ShadowContentResolver.registerProvider(ApkProvider.getAuthority(), new ApkProvider());
        assertInvalidUri(resolver, ApkProvider.getAuthority());
        assertInvalidUri(resolver, "blah");
    }

    @Test
    public void validApkProviderUris() {
        ShadowContentResolver.registerProvider(ApkProvider.getAuthority(), new ApkProvider());
        String[] projection = new String[] {Schema.ApkTable.Cols._ID};

        List<Apk> apks = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            apks.add(new MockApk("com.example." + i, i));
        }

        assertValidUri(resolver, ApkProvider.getContentUri(), "content://org.fdroid.fdroid.data.ApkProvider", projection);
        assertValidUri(resolver, ApkProvider.getAppUri("org.fdroid.fdroid"), "content://org.fdroid.fdroid.data.ApkProvider/app/org.fdroid.fdroid", projection);
        assertValidUri(resolver, ApkProvider.getContentUri(new MockApk("org.fdroid.fdroid", 100)), "content://org.fdroid.fdroid.data.ApkProvider/apk/100/org.fdroid.fdroid", projection);
        assertValidUri(resolver, ApkProvider.getContentUri(apks), projection);
        assertValidUri(resolver, ApkProvider.getContentUri("org.fdroid.fdroid", 100), "content://org.fdroid.fdroid.data.ApkProvider/apk/100/org.fdroid.fdroid", projection);
        assertValidUri(resolver, ApkProvider.getRepoUri(1000), "content://org.fdroid.fdroid.data.ApkProvider/repo/1000", projection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidApkUrisWithTooManyApks() {
        String[] projection = Schema.ApkTable.Cols.ALL;

        List<Apk> manyApks = new ArrayList<>(ApkProvider.MAX_APKS_TO_QUERY - 5);
        for (int i = 0; i < ApkProvider.MAX_APKS_TO_QUERY - 1; i++) {
            manyApks.add(new MockApk("com.example." + i, i));
        }
        assertValidUri(resolver, ApkProvider.getContentUri(manyApks), projection);

        manyApks.add(new MockApk("org.fdroid.fdroid.1", 1));
        manyApks.add(new MockApk("org.fdroid.fdroid.2", 2));

        // Technically, it is a valid URI, because it doesn't
        // throw an UnsupportedOperationException. However it
        // is still not okay (we run out of bindable parameters
        // in the sqlite query.
        assertValidUri(resolver, ApkProvider.getContentUri(manyApks), projection);
    }

}
