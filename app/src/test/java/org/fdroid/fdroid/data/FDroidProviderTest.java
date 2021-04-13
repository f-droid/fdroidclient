package org.fdroid.fdroid.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.ContextWrapper;

import org.fdroid.fdroid.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.robolectric.android.controller.ContentProviderController;

import androidx.test.core.app.ApplicationProvider;

public abstract class FDroidProviderTest { // NOPMD This abstract class does not have any abstract methods

    protected ContentResolver contentResolver;
    protected ContentProviderController<AppProvider> contentProviderController;
    protected ContextWrapper context;

    @Before
    public final void setupBase() {
        contentResolver = ApplicationProvider.getApplicationContext().getContentResolver();
        context = TestUtils.createContextWithContentResolver(contentResolver);
        contentProviderController = TestUtils.registerContentProvider(AppProvider.getAuthority(), AppProvider.class);
    }

    @After
    public final void tearDownBase() {
        contentProviderController.shutdown();
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
