package mock;

import android.content.Context;
import android.test.mock.MockResources;

import org.fdroid.fdroid.R;

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
        switch (id) {
            case R.integer.fdroid_repo_inuse:
                return 1;
            case R.integer.fdroid_archive_inuse:
                return 0;
            case R.integer.fdroid_repo_priority:
                return 10;
            case R.integer.fdroid_archive_priority:
                return 20;
            default:
                return 0;
        }
    }

}
