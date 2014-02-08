package org.fdroid.fdroid;

import android.annotation.TargetApi;
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

public abstract class FDroidProviderTest<T extends FDroidProvider> extends ProviderTestCase2MockContext<T> {

    private MockContextSwappableComponents swappableContext;

    public FDroidProviderTest(Class<T> providerClass, String providerAuthority) {
        super(providerClass, providerAuthority);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Utils.setupInstalledApkCache(new MockInstalledApkCache());
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

    protected void assertInvalidUri(String uri) {
        assertInvalidUri(Uri.parse(uri));
    }

    protected void assertValidUri(String uri) {
        assertValidUri(Uri.parse(uri));
    }

    protected void assertInvalidUri(Uri uri) {
        try {
            getProvider().query(uri, getMinimalProjection(), null, null, null);
            fail();
        } catch (UnsupportedOperationException e) {}
    }

    protected void assertValidUri(Uri uri) {
        Cursor cursor = getProvider().query(uri, getMinimalProjection(), null, null, null);
        assertNotNull(cursor);
    }

    /**
     * Many queries need at least some sort of projection in order to produce
     * valid SQL. As such, we also need to know about that, so we can provide
     * helper functions that revolve around the contnet provider under test.
     */
    protected abstract String[] getMinimalProjection();

}
