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
    }

}
