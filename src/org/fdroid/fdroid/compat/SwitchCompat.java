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
        if (hasApi(14)) {
            return new IceCreamSwitch(activity);
        } else {
            return new OldSwitch(activity);
        }
    }

}

@TargetApi(14)
class IceCreamSwitch extends SwitchCompat {

    protected IceCreamSwitch(ManageRepo activity) {
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
