package org.fdroid.fdroid.views.updates.items;

import android.app.Activity;

/**
 * Used as a common base class for all data types in the {@link
 * org.fdroid.fdroid.views.updates.UpdatesAdapter}. Doesn't have any
 * functionality of its own, but allows the {@link
 * org.fdroid.fdroid.views.updates.UpdatesAdapter#delegatesManager}
 * to specify a data type more specific than just {@link Object}.
 */
public abstract class AppUpdateData {
    public final Activity activity;

    public AppUpdateData(Activity activity) {
        this.activity = activity;
    }
}
