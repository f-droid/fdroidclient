package org.fdroid.fdroid.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.ContextWrapper;
import org.fdroid.fdroid.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowContentResolver;

public abstract class FDroidProviderTest { // NOPMD This abstract class does not have any abstract methods

    protected ContentResolver contentResolver;
    protected ContextWrapper context;

    @Before
    public final void setupBase() {
        contentResolver = RuntimeEnvironment.application.getContentResolver();
        context = TestUtils.createContextWithContentResolver(Shadows.shadowOf(contentResolver));
        TestUtils.registerContentProvider(AppProvider.getAuthority(), AppProvider.class);
    }

    @After
    public final void tearDownBase() {
        CategoryProvider.Helper.clearCategoryIdCache();
        DBHelper.clearDbHelperSingleton();
    }

    protected Repo setEnabled(Repo repo, boolean enabled) {
        ContentValues enable = new ContentValues(1);
        enable.put(Schema.RepoTable.Cols.IN_USE, enabled);
        RepoProvider.Helper.update(context, repo, enable);
        return RepoProvider.Helper.findByAddress(context, repo.address);
    }
}
