package mock;

import android.test.mock.MockResources;

public class MockEmptyResources extends MockResources {

    @Override
    public String getString(int id) {
        return "";
    }

}
