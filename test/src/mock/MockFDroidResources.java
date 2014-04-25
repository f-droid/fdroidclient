package mock;

import android.content.Context;
import android.content.res.Resources;
import android.test.mock.*;
import org.fdroid.fdroid.*;

public class MockFDroidResources extends MockResources {

    private Context getStringDelegatingContext;

    public MockFDroidResources(Context getStringDelegatingContext) {
        this.getStringDelegatingContext = getStringDelegatingContext;
    }

    @Override
    public String getString(int id) {
        return getStringDelegatingContext.getString(id);
    }

    @Override
    public int getInteger(int id) {
        if (id == R.integer.default_repo_inuse1) {
            return 1;
        } else if (id == R.integer.default_repo_inuse2) {
            return 0;
        } else if (id == R.integer.default_repo_priority1) {
            return 10;
        } else if (id == R.integer.default_repo_priority2) {
            return 20;
        } else {
            return 0;
        }
}

}
