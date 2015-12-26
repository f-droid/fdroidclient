package org.fdroid.fdroid;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.test.ProviderTestCase2MockContext;

import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.FDroidProvider;
import org.fdroid.fdroid.data.InstalledAppProvider;
import org.fdroid.fdroid.data.RepoProvider;

import java.util.List;

import mock.MockContextEmptyComponents;
import mock.MockContextSwappableComponents;
import mock.MockFDroidResources;

public abstract class FDroidProviderTest<T extends FDroidProvider> extends ProviderTestCase2MockContext<T> {

    private FDroidProvider[] allProviders = {
        new AppProvider(),
        new RepoProvider(),
        new ApkProvider(),
        new InstalledAppProvider(),
    };

    private MockContextSwappableComponents swappableContext;

    public FDroidProviderTest(Class<T> providerClass, String providerAuthority) {
        super(providerClass, providerAuthority);
    }

    protected Resources getMockResources() {
        return new MockFDroidResources(getContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Instantiate all providers other than the one which was already created by the base class.
        // This is because F-Droid providers tend to perform joins onto tables managed by other
        // providers, and so we need to be able to insert into those other providers for these
        // joins to be tested correctly.
        for (FDroidProvider provider : allProviders) {
            if (!provider.getName().equals(getProvider().getName())) {
                provider.attachInfo(getMockContext(), null);
                getMockContentResolver().addProvider(provider.getName(), provider);
            }
        }

        getSwappableContext().setResources(getMockResources());

        // The *Provider.Helper.* functions tend to take a Context as their
        // first parameter. This context is used to connect to the relevant
        // content provider. Thus, we need a context that is able to connect
        // to the mock content resolver, in order to reach the provider
        // under test.
        getSwappableContext().setContentResolver(getMockContentResolver());

    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    public void testObviouslyInvalidUris() {
        assertInvalidUri("http://www.google.com");
        assertInvalidUri(ContactsContract.AUTHORITY_URI);
        assertInvalidUri("junk");
    }

    @Override
    protected Context createMockContext(Context delegate) {
        swappableContext = new MockContextEmptyComponents();
        return swappableContext;
    }

    public MockContextSwappableComponents getSwappableContext() {
        return swappableContext;
    }

    protected void assertCantDelete(Uri uri) {
        try {
            getMockContentResolver().delete(uri, null, null);
            fail();
        } catch (UnsupportedOperationException e) {
        } catch (Exception e) {
            fail();
        }
    }

    protected void assertCantUpdate(Uri uri) {
        try {
            getMockContentResolver().update(uri, new ContentValues(), null, null);
            fail();
        } catch (UnsupportedOperationException e) {
        } catch (Exception e) {
            fail();
        }
    }

    protected void assertInvalidUri(String uri) {
        assertInvalidUri(Uri.parse(uri));
    }

    protected void assertValidUri(String uri) {
        assertValidUri(Uri.parse(uri));
    }

    protected void assertInvalidUri(Uri uri) {
        try {
            // Use getProvdider instead of getContentResolver, because the mock
            // content resolver wont result in the provider we are testing, and
            // hence we don't get to see how our provider responds to invalid
            // uris.
            getProvider().query(uri, getMinimalProjection(), null, null, null);
            fail();
        } catch (UnsupportedOperationException e) { }
    }

    protected void assertValidUri(Uri uri) {
        Cursor cursor = getMockContentResolver().query(uri, getMinimalProjection(), null, null, null);
        assertNotNull(cursor);
        cursor.close();
    }

    /**
     * Many queries need at least some sort of projection in order to produce
     * valid SQL. As such, we also need to know about that, so we can provide
     * helper functions that revolve around the contnet provider under test.
     */
    protected abstract String[] getMinimalProjection();

    protected void assertResultCount(int expectedCount, Uri uri) {
        Cursor cursor = getMockContentResolver().query(uri, getMinimalProjection(), null, null, null);
        assertResultCount(expectedCount, cursor);
        cursor.close();
    }

    protected void assertResultCount(int expectedCount, List items) {
        assertNotNull(items);
        assertEquals(expectedCount, items.size());
    }

    protected void assertResultCount(int expectedCount, Cursor result) {
        assertNotNull(result);
        assertEquals(expectedCount, result.getCount());
    }

    protected void assertIsInstalledVersionInDb(String appId, int versionCode, String versionName) {
        Uri uri = InstalledAppProvider.getAppUri(appId);

        String[] projection = {
            InstalledAppProvider.DataColumns.PACKAGE_NAME,
            InstalledAppProvider.DataColumns.VERSION_CODE,
            InstalledAppProvider.DataColumns.VERSION_NAME,
            InstalledAppProvider.DataColumns.APPLICATION_LABEL,
        };

        Cursor cursor = getMockContentResolver().query(uri, projection, null, null, null);

        assertNotNull(cursor);
        assertEquals("App \"" + appId + "\" not installed", 1, cursor.getCount());

        cursor.moveToFirst();

        assertEquals(appId, cursor.getString(cursor.getColumnIndex(InstalledAppProvider.DataColumns.PACKAGE_NAME)));
        assertEquals(versionCode, cursor.getInt(cursor.getColumnIndex(InstalledAppProvider.DataColumns.VERSION_CODE)));
        assertEquals(versionName, cursor.getString(cursor.getColumnIndex(InstalledAppProvider.DataColumns.VERSION_NAME)));
        cursor.close();
    }

}
