package org.fdroid.fdroid.data;

import static org.fdroid.fdroid.Assert.assertInvalidUri;
import static org.fdroid.fdroid.Assert.assertValidUri;

import android.content.ContentResolver;
import java.util.ArrayList;
import java.util.List;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.TestUtils;
import org.fdroid.fdroid.data.Schema.InstalledAppTable;
import org.fdroid.fdroid.mock.MockApk;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("LineLength")
public class ProviderUriTests {

    private static final String CONTENT_URI_BASE = "content://" + FDroidProvider.AUTHORITY;
    private static final String APK_PROVIDER_URI_BASE = CONTENT_URI_BASE + ".ApkProvider";
    private static final String APP_PROVIDER_URI_BASE = CONTENT_URI_BASE + ".AppProvider";
    private static final String TEMP_APP_PROVIDER_URI_BASE = CONTENT_URI_BASE + ".TempAppProvider";

    private ContentResolver resolver;

    @Before
    public void setup() {
        resolver = RuntimeEnvironment.application.getContentResolver();
    }

    @After
    public void teardown() {
        DBHelper.clearDbHelperSingleton();
    }

    @Test
    public void invalidInstalledAppProviderUris() {
        TestUtils.registerContentProvider(InstalledAppProvider.getAuthority(), InstalledAppProvider.class);
        assertInvalidUri(Shadows.shadowOf(resolver), InstalledAppProvider.getAuthority());
        assertInvalidUri(Shadows.shadowOf(resolver), "blah");
    }

    @Test
    public void validInstalledAppProviderUris() {
        TestUtils.registerContentProvider(InstalledAppProvider.getAuthority(), InstalledAppProvider.class);
        String[] projection = new String[]{InstalledAppTable.Cols._ID};
        assertValidUri(Shadows.shadowOf(resolver), InstalledAppProvider.getContentUri(), projection);
        assertValidUri(Shadows.shadowOf(resolver), InstalledAppProvider.getAppUri("org.example.app"), projection);
        assertValidUri(Shadows.shadowOf(resolver), InstalledAppProvider.getSearchUri("blah"), projection);
        assertValidUri(Shadows.shadowOf(resolver), InstalledAppProvider.getSearchUri("\"blah\""), projection);
        assertValidUri(Shadows.shadowOf(resolver), InstalledAppProvider.getSearchUri("blah & sneh"), projection);
        assertValidUri(Shadows.shadowOf(resolver), InstalledAppProvider.getSearchUri("http://blah.example.com?sneh=\"sneh\""), projection);
    }

    @Test
    public void invalidRepoProviderUris() {
        TestUtils.registerContentProvider(RepoProvider.getAuthority(), RepoProvider.class);
        assertInvalidUri(Shadows.shadowOf(resolver), RepoProvider.getAuthority());
        assertInvalidUri(Shadows.shadowOf(resolver), "blah");
    }

    @Test
    public void validRepoProviderUris() {
        TestUtils.registerContentProvider(RepoProvider.getAuthority(), RepoProvider.class);
        String[] projection = new String[]{Schema.RepoTable.Cols._ID};
        assertValidUri(Shadows.shadowOf(resolver), RepoProvider.getContentUri(), projection);
        assertValidUri(Shadows.shadowOf(resolver), RepoProvider.getContentUri(10000L), projection);
        assertValidUri(Shadows.shadowOf(resolver), RepoProvider.allExceptSwapUri(), projection);
    }

    @Test
    public void invalidAppProviderUris() {
        TestUtils.registerContentProvider(AppProvider.getAuthority(), AppProvider.class);
        assertInvalidUri(Shadows.shadowOf(resolver), AppProvider.getAuthority());
        assertInvalidUri(Shadows.shadowOf(resolver), "blah");
    }

    @Test
    public void validAppProviderUris() {
        TestUtils.registerContentProvider(AppProvider.getAuthority(), AppProvider.class);
        String[] projection = new String[]{Schema.AppMetadataTable.Cols._ID};
        assertValidUri(Shadows.shadowOf(resolver), AppProvider.getContentUri(), APP_PROVIDER_URI_BASE, projection);
        assertValidUri(Shadows.shadowOf(resolver), AppProvider.getSearchUri("'searching!'", null), APP_PROVIDER_URI_BASE + "/search/'searching!'", projection);
        assertValidUri(Shadows.shadowOf(resolver), AppProvider.getSearchUri("'searching!'", "Games"), APP_PROVIDER_URI_BASE + "/search/'searching!'/Games", projection);
        assertValidUri(Shadows.shadowOf(resolver), AppProvider.getSearchUri("/", null), APP_PROVIDER_URI_BASE + "/search/%2F", projection);
        assertValidUri(Shadows.shadowOf(resolver), AppProvider.getSearchUri("/", "Games"), APP_PROVIDER_URI_BASE + "/search/%2F/Games", projection);
        assertValidUri(Shadows.shadowOf(resolver), AppProvider.getSearchUri("", null), APP_PROVIDER_URI_BASE, projection);
        assertValidUri(Shadows.shadowOf(resolver), AppProvider.getSearchUri("", "Games"), APP_PROVIDER_URI_BASE + "/category/Games", projection);
        assertValidUri(Shadows.shadowOf(resolver), AppProvider.getCategoryUri("Games"), APP_PROVIDER_URI_BASE + "/category/Games", projection);
        assertValidUri(Shadows.shadowOf(resolver), AppProvider.getSearchUri((String) null, null), APP_PROVIDER_URI_BASE, projection);
        assertValidUri(Shadows.shadowOf(resolver), AppProvider.getSearchUri((String) null, "Games"), APP_PROVIDER_URI_BASE + "/category/Games", projection);
        assertValidUri(Shadows.shadowOf(resolver), AppProvider.getInstalledUri(), APP_PROVIDER_URI_BASE + "/installed", projection);
        assertValidUri(Shadows.shadowOf(resolver), AppProvider.getCanUpdateUri(), APP_PROVIDER_URI_BASE + "/canUpdate", projection);

        App app = new App();
        app.repoId = 1;
        app.packageName = "org.fdroid.fdroid";

        assertValidUri(Shadows.shadowOf(resolver), AppProvider.getSpecificAppUri(app.packageName, app.repoId),
                APP_PROVIDER_URI_BASE + "/app/1/org.fdroid.fdroid", projection);
    }

    @Test
    public void validTempAppProviderUris() {
        TestUtils.registerContentProvider(TempAppProvider.getAuthority(), TempAppProvider.class);
        String[] projection = new String[]{Schema.AppMetadataTable.Cols._ID};

        // Required so that the `assertValidUri` calls below will indeed have a real temp_fdroid_app
        // table to query.
        TempAppProvider.Helper.init(TestUtils.createContextWithContentResolver(Shadows.shadowOf(resolver)), 123);

        List<String> packageNames = new ArrayList<>(2);
        packageNames.add("org.fdroid.fdroid");
        packageNames.add("com.example.com");

        assertValidUri(Shadows.shadowOf(resolver), TempAppProvider.getAppsUri(packageNames, 1),
                TEMP_APP_PROVIDER_URI_BASE + "/apps/1/org.fdroid.fdroid%2Ccom.example.com", projection);
        assertValidUri(Shadows.shadowOf(resolver), TempAppProvider.getContentUri(), TEMP_APP_PROVIDER_URI_BASE, projection);
    }

    @Test
    public void invalidApkProviderUris() {
        TestUtils.registerContentProvider(ApkProvider.getAuthority(), ApkProvider.class);
        assertInvalidUri(Shadows.shadowOf(resolver), ApkProvider.getAuthority());
        assertInvalidUri(Shadows.shadowOf(resolver), "blah");
    }

    @Test
    public void validApkProviderUris() {
        TestUtils.registerContentProvider(ApkProvider.getAuthority(), ApkProvider.class);
        String[] projection = new String[]{Schema.ApkTable.Cols._ID};

        List<Apk> apks = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            apks.add(new MockApk("com.example." + i, i));
        }

        assertValidUri(Shadows.shadowOf(resolver), ApkProvider.getContentUri(),
                APK_PROVIDER_URI_BASE, projection);
        assertValidUri(Shadows.shadowOf(resolver), ApkProvider.getAppUri("org.fdroid.fdroid"),
                APK_PROVIDER_URI_BASE + "/app/org.fdroid.fdroid", projection);
        assertValidUri(Shadows.shadowOf(resolver), ApkProvider.getApkFromAnyRepoUri(new MockApk("org.fdroid.fdroid", 100)),
                APK_PROVIDER_URI_BASE + "/apk-any-repo/100/org.fdroid.fdroid", projection);
        assertValidUri(Shadows.shadowOf(resolver), ApkProvider.getApkFromAnyRepoUri("org.fdroid.fdroid", 100, null),
                APK_PROVIDER_URI_BASE + "/apk-any-repo/100/org.fdroid.fdroid", projection);
        assertValidUri(Shadows.shadowOf(resolver), ApkProvider.getRepoUri(1000),
                APK_PROVIDER_URI_BASE + "/repo/1000", projection);
    }
}
