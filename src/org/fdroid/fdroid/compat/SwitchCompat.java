package org.fdroid.fdroid.compat;

import android.os.Build;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.ToggleButton;
import org.fdroid.fdroid.ManageRepo;

public abstract class SwitchCompat {

    protected final ManageRepo activity;

    protected SwitchCompat(ManageRepo activity) {
        this.activity = activity;
    }

    public abstract CompoundButton createSwitch();

    public static SwitchCompat create(ManageRepo activity) {
        if (Build.VERSION.SDK_INT >= 11) {
                return new HoneycombSwitch(activity);
        } else {
            return new OldSwitch(activity);
        }
    }

}

class HoneycombSwitch extends SwitchCompat {

    protected HoneycombSwitch(ManageRepo activity) {
        super(activity);
    }

    @Override
    public CompoundButton createSwitch() {
        return new Switch(activity);
    }
}

class OldSwitch extends SwitchCompat {

    protected OldSwitch(ManageRepo activity) {
        super(activity);
    }

    @Override
    public CompoundButton createSwitch() {
        return new ToggleButton(activity);
    }
}