package org.fdroid.fdroid.compat;

import android.annotation.TargetApi;
import android.content.Context;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.ToggleButton;

public abstract class SwitchCompat extends Compatibility {

    protected final Context context;

    protected SwitchCompat(Context context) {
        this.context = context;
    }

    public abstract CompoundButton createSwitch();

    public static SwitchCompat create(Context context) {
        if (hasApi(14)) {
            return new IceCreamSwitch(context);
        } else {
            return new OldSwitch(context);
        }
    }

}

@TargetApi(14)
class IceCreamSwitch extends SwitchCompat {

    protected IceCreamSwitch(Context context) {
        super(context);
    }

    @Override
    public CompoundButton createSwitch() {
        return new Switch(context);
    }
}

class OldSwitch extends SwitchCompat {

    protected OldSwitch(Context context) {
        super(context);
    }

    @Override
    public CompoundButton createSwitch() {
        return new ToggleButton(context);
    }
}
