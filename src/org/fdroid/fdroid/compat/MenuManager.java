package org.fdroid.fdroid.compat;

import android.app.Activity;

abstract public class MenuManager extends Compatibility {

    public static MenuManager create(Activity activity) {
        if (hasApi(11)) {
            return new HoneycombMenuManagerImpl(activity);
        } else {
            return new OldMenuManagerImpl(activity);
        }
    }

    protected final Activity activity;

    protected MenuManager(Activity activity) {
        this.activity = activity;
    }

    abstract public void invalidateOptionsMenu();

}

class OldMenuManagerImpl extends MenuManager {

    protected OldMenuManagerImpl(Activity activity) {
        super(activity);
    }

    @Override
    public void invalidateOptionsMenu() {
    }

}

class HoneycombMenuManagerImpl extends MenuManager {

    protected HoneycombMenuManagerImpl(Activity activity) {
        super(activity);
    }

    @Override
    public void invalidateOptionsMenu() {
        activity.invalidateOptionsMenu();
    }
}
