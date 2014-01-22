package org.fdroid.fdroid.compat;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;

public abstract class ActionBarCompat extends Compatibility {

    public static ActionBarCompat create(Activity activity) {
        if (hasApi(11)) {
            return new HoneycombActionBarCompatImpl(activity);
        } else {
            return new OldActionBarCompatImpl(activity);
        }
    }

    protected final Activity activity;

    public ActionBarCompat(Activity activity) {
        this.activity = activity;
    }

    public abstract void setDisplayHomeAsUpEnabled(boolean value);

}

class OldActionBarCompatImpl extends ActionBarCompat {

    public OldActionBarCompatImpl(Activity activity) {
        super(activity);
    }

    @Override
    public void setDisplayHomeAsUpEnabled(boolean value) {
        // Do nothing...
    }

}

@TargetApi(11)
class HoneycombActionBarCompatImpl extends ActionBarCompat {

    private final ActionBar actionBar;

    public HoneycombActionBarCompatImpl(Activity activity) {
        super(activity);
        this.actionBar = activity.getActionBar();
    }

    @Override
    public void setDisplayHomeAsUpEnabled(boolean value) {
        actionBar.setDisplayHomeAsUpEnabled(value);
    }

}
