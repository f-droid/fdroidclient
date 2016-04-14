package org.fdroid.fdroid.views;

import android.content.Context;
import android.database.Cursor;
import android.os.Build;

public class CanUpdateAppListAdapter extends AppListAdapter {

    public static CanUpdateAppListAdapter create(Context context, Cursor cursor, int flags) {
        if (Build.VERSION.SDK_INT >= 11) {
            return new CanUpdateAppListAdapter(context, cursor, flags);
        } else {
            return new CanUpdateAppListAdapter(context, cursor);
        }
    }

    private CanUpdateAppListAdapter(Context context, Cursor c) {
        super(context, c);
    }

    public CanUpdateAppListAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    private CanUpdateAppListAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    protected boolean showStatusUpdate() {
        return true;
    }

    @Override
    protected boolean showStatusInstalled() {
        return false;
    }
}
