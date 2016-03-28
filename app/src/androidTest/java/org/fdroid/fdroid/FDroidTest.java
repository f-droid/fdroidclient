package org.fdroid.fdroid;

import android.annotation.TargetApi;
import android.os.Build;
import android.test.ActivityInstrumentationTestCase2;

@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class FDroidTest extends ActivityInstrumentationTestCase2<FDroid> {

    public FDroidTest() {
        super("org.fdroid.fdroid", FDroid.class);
    }

}
