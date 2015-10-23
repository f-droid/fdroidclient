package org.fdroid.fdroid;

import org.fdroid.fdroid.data.AppProvider;

/**
 * Class that makes available all ContentProviders that F-Droid owns.
 */
public abstract class FDroidTestWithAllProviders extends FDroidProviderTest<AppProvider> {

    public FDroidTestWithAllProviders(Class<AppProvider> providerClass, String providerAuthority) {
        super(providerClass, providerAuthority);
    }

    @Override
    protected String[] getMinimalProjection() {
        return new String[] {
                AppProvider.DataColumns._ID,
                AppProvider.DataColumns.APP_ID,
                AppProvider.DataColumns.NAME,
        };
    }

}
