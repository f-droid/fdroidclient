package org.fdroid.fdroid.data;

import android.content.ContentResolver;
import android.content.ContextWrapper;

import org.fdroid.fdroid.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.mockito.AdditionalAnswers;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowContentResolver;

import static org.mockito.Mockito.mock;

public abstract class FDroidProviderTest {

    protected ShadowContentResolver contentResolver;
    protected ContextWrapper context;

    @Before
    public final void setupBase() {
        contentResolver = Shadows.shadowOf(RuntimeEnvironment.application.getContentResolver());
        context = TestUtils.createContextWithContentResolver(contentResolver);
        ShadowContentResolver.registerProvider(AppProvider.getAuthority(), new AppProvider());
    }

    @After
    public final void tearDownBase() {
        FDroidProvider.clearDbHelperSingleton();
    }

}
