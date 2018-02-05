package org.fdroid.fdroid.data;

import android.content.ContextWrapper;
import org.fdroid.fdroid.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowContentResolver;

public abstract class FDroidProviderTest {

    protected ShadowContentResolver contentResolver;
    protected ContextWrapper context;

    @Before
    public final void setupBase() {
        contentResolver = Shadows.shadowOf(RuntimeEnvironment.application.getContentResolver());
        context = TestUtils.createContextWithContentResolver(contentResolver);
        TestUtils.registerContentProvider(AppProvider.getAuthority(), AppProvider.class);
    }

    @After
    public final void tearDownBase() {
        CategoryProvider.Helper.clearCategoryIdCache();
        DBHelper.clearDbHelperSingleton();
    }

}
