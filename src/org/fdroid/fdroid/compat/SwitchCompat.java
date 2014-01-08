package org.fdroid.fdroid.compat;

import android.annotation.TargetApi;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.ToggleButton;
import org.fdroid.fdroid.ManageRepo;

public abstract class SwitchCompat extends Compatibility {

    protected final ManageRepo activity;

    protected SwitchCompat(ManageRepo activity) {
        this.activity = activity;
    }

    public abstract CompoundButton createSwitch();

    public static SwitchCompat create(ManageRepo activity) {
        if (hasApi(11)) {
                return new HoneycombSwitch(activity);
        } else {
            return new OldSwitch(activity);
        }
    }

}

@TargetApi(11)
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
