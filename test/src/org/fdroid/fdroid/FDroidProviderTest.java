package org.fdroid.fdroid;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.test.ProviderTestCase2MockContext;
import mock.MockContextEmptyComponents;
import mock.MockContextSwappableComponents;
import org.fdroid.fdroid.data.FDroidProvider;
import org.fdroid.fdroid.mock.MockInstalledApkCache;

import java.util.List;

public abstract class FDroidProviderTest<T extends FDroidProvider> extends ProviderTestCase2MockContext<T> {

    private MockContextSwappableComponents swappableContext;

    public FDroidProviderTest(Class<T> providerClass, String providerAuthority) {
        super(providerClass, providerAuthority);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Utils.setupInstalledApkCache(new MockInstalledApkCache());

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
        } catch (UnsupportedOperationException e) {}
    }

    protected void assertValidUri(Uri uri) {
        Cursor cursor = getMockContentResolver().query(uri, getMinimalProjection(), null, null, null);
        assertNotNull(cursor);
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
    }

    protected void assertResultCount(int expectedCount, List items) {
        assertNotNull(items);
        assertEquals(expectedCount, items.size());
    }

    protected void assertResultCount(int expectedCount, Cursor result) {
        assertNotNull(result);
        assertEquals(expectedCount, result.getCount());
    }
}
