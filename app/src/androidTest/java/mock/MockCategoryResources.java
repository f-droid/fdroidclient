package mock;

import android.content.Context;

import org.fdroid.fdroid.R;

public class MockCategoryResources extends MockFDroidResources {

    public MockCategoryResources(Context getStringDelegatingContext) {
        super(getStringDelegatingContext);
    }

    @Override
    public String getString(int id) {
        switch (id) {
            case R.string.category_All:
                return "All";
            case R.string.category_Recently_Updated:
                return "Recently Updated";
            case R.string.category_Whats_New:
                return "Whats New";
            default:
                return "";
        }
    }

}
