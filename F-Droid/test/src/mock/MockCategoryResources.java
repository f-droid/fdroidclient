package mock;

import android.content.Context;

import org.fdroid.fdroid.R;

public class MockCategoryResources extends MockFDroidResources {

    public MockCategoryResources(Context getStringDelegatingContext) {
        super(getStringDelegatingContext);
    }

    @Override
    public String getString(int id) {
        if (id == R.string.category_All) {
            return "All";
        } else if (id == R.string.category_Recently_Updated) {
            return "Recently Updated";
        } else if (id == R.string.category_Whats_New) {
            return "Whats New";
        } else {
            return "";
        }
}

}
