package org.belmarket.shop.compat;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;

public abstract class ClipboardCompat {

    public abstract String getText();

    public static ClipboardCompat create(Context context) {
        if (Build.VERSION.SDK_INT >= 11) {
            return new HoneycombClipboard(context);
        }
        return new OldClipboard();
    }

    @TargetApi(11)
    private static class HoneycombClipboard extends ClipboardCompat {

        private final ClipboardManager manager;

        HoneycombClipboard(Context context) {
            this.manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
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

    private static class OldClipboard extends ClipboardCompat {

        @Override
        public String getText() {
            return null;
        }
    }
}
