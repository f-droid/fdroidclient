package org.fdroid.fdroid;

import android.os.Build;
import android.view.MenuItem;

public class CompatabilityUtils {

    protected static void showAsAction( MenuItem item ) {
        if ( Build.VERSION.SDK_INT >= 11 ) {
            item.setShowAsAction( MenuItem.SHOW_AS_ACTION_IF_ROOM );
        }
    }

}
