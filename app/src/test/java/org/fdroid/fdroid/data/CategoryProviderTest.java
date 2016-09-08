package org.fdroid.fdroid.data;

import android.app.Application;
import android.content.ContentValues;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.Schema.AppMetadataTable.Cols;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.List;

import static org.fdroid.fdroid.Assert.assertContainsOnly;

// TODO: Use sdk=24 when Robolectric supports this
@Config(constants = BuildConfig.class, application = Application.class, sdk = 23)
@RunWith(RobolectricGradleTestRunner.class)
public class CategoryProviderTest extends FDroidProviderTest {

    @Before
    public void setup() {
        ShadowContentResolver.registerProvider(AppProvider.getAuthority(), new AppProvider());
    }

    // ========================================================================
    //  "Categories"
    //  (at this point) not an additional table, but we treat them sort of
    //  like they are. That means that if we change the implementation to
    //  use a separate table in the future, these should still pass.
    // ========================================================================

    @Test
    public void testCategoriesSingle() {
        insertAppWithCategory("com.dog", "Dog", "Animal");
        insertAppWithCategory("com.rock", "Rock", "Mineral");
        insertAppWithCategory("com.banana", "Banana", "Vegetable");

        List<String> categories = AppProvider.Helper.categories(context);
        String[] expected = new String[] {
                context.getResources().getString(R.string.category_Whats_New),
                context.getResources().getString(R.string.category_Recently_Updated),
                context.getResources().getString(R.string.category_All),
                "Animal",
                "Mineral",
                "Vegetable",
        };
        assertContainsOnly(categories, expected);
    }

    @Test
    public void testCategoriesMultiple() {
        insertAppWithCategory("com.rock.dog", "Rock-Dog", "Mineral,Animal");
        insertAppWithCategory("com.dog.rock.apple", "Dog-Rock-Apple", "Animal,Mineral,Vegetable");
        insertAppWithCategory("com.banana.apple", "Banana", "Vegetable,Vegetable");

        List<String> categories = AppProvider.Helper.categories(context);
        String[] expected = new String[] {
                context.getResources().getString(R.string.category_Whats_New),
                context.getResources().getString(R.string.category_Recently_Updated),
                context.getResources().getString(R.string.category_All),

                "Animal",
                "Mineral",
                "Vegetable",
        };
        assertContainsOnly(categories, expected);

        insertAppWithCategory("com.example.game", "Game",
                "Running,Shooting,Jumping,Bleh,Sneh,Pleh,Blah,Test category," +
                "The quick brown fox jumps over the lazy dog,With apostrophe's");

        List<String> categoriesLonger = AppProvider.Helper.categories(context);
        String[] expectedLonger = new String[] {
                context.getResources().getString(R.string.category_Whats_New),
                context.getResources().getString(R.string.category_Recently_Updated),
                context.getResources().getString(R.string.category_All),

                "Animal",
                "Mineral",
                "Vegetable",

                "Running",
                "Shooting",
                "Jumping",
                "Bleh",
                "Sneh",
                "Pleh",
                "Blah",
                "Test category",
                "The quick brown fox jumps over the lazy dog",
                "With apostrophe's",
        };

        assertContainsOnly(categoriesLonger, expectedLonger);
    }

    private void insertAppWithCategory(String id, String name, String categories) {
        ContentValues values = new ContentValues(1);
        values.put(Cols.CATEGORIES, categories);
        AppProviderTest.insertApp(contentResolver, context, id, name, values);
    }

}
