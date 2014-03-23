package mock;

import android.content.Context;
import android.test.mock.*;
import org.fdroid.fdroid.*;

public class MockCategoryResources extends MockFDroidResources {

    public MockCategoryResources(Context getStringDelegatingContext) {
        super(getStringDelegatingContext);
    }

    @Override
    public String getString(int id) {
        if (id == R.string.category_all) {
            return "All";
        } else if (id == R.string.category_recentlyupdated) {
            return "Recently Updated";
        } else if (id == R.string.category_whatsnew) {
            return "Whats New";
        } else {
            return "";
        }
}

}
