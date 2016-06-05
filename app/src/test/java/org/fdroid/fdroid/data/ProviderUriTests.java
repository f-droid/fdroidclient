package org.fdroid.fdroid.data;

import org.fdroid.fdroid.BuildConfig;
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

import static org.fdroid.fdroid.data.ProviderTestUtils.assertInvalidUri;
import static org.fdroid.fdroid.data.ProviderTestUtils.assertValidUri;

@Config(constants = BuildConfig.class)
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
        String[] projection = new String[] { InstalledAppProvider.DataColumns._ID };
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
        String[] projection = new String[] { RepoProvider.DataColumns._ID };
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
        String[] projection = new String[] { AppProvider.DataColumns._ID };
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

        List<App> apps = new ArrayList<>(1);
        apps.add(app);

        assertValidUri(resolver, AppProvider.getContentUri(app), "content://org.fdroid.fdroid.data.AppProvider/org.fdroid.fdroid", projection);
        assertValidUri(resolver, AppProvider.getContentUri(apps), "content://org.fdroid.fdroid.data.AppProvider/apps/org.fdroid.fdroid", projection);
        assertValidUri(resolver, AppProvider.getContentUri("org.fdroid.fdroid"), "content://org.fdroid.fdroid.data.AppProvider/org.fdroid.fdroid", projection);
    }

}
