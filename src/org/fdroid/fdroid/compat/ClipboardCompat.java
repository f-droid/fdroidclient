package org.fdroid.fdroid.compat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.ToggleButton;
import org.fdroid.fdroid.ManageRepo;

public abstract class ClipboardCompat {

    public abstract String getText();

    public static ClipboardCompat create(Context context) {
        if (Build.VERSION.SDK_INT >= 11) {
                return new HoneycombClipboard(context);
        } else {
            return new OldClipboard();
        }
    }

}

class HoneycombClipboard extends ClipboardCompat {

    private final ClipboardManager manager;

    protected HoneycombClipboard(Context context) {
        this.manager =
            (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Override
    public String getText() {
        CharSequence text = null;
        if (manager.hasPrimaryClip()) {
            ClipData data = manager.getPrimaryClip();
            if (data.getItemCount() > 0) {
                text = data.getItemAt(0).getText();
            }
        }
        return text != null ? text.toString() : null;
    }
}

class OldClipboard extends ClipboardCompat {

    @Override
    public String getText() {
        return null;
    }
}