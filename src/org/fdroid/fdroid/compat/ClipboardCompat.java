package org.fdroid.fdroid.compat;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public abstract class ClipboardCompat extends Compatibility {

    public abstract String getText();

    public static ClipboardCompat create(Context context) {
        if (hasApi(11)) {
            return new HoneycombClipboard(context);
        } else {
            return new OldClipboard();
        }
    }

}

@TargetApi(11)
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
