package org.fdroid.fdroid.views;

import android.content.Context;
import android.database.Cursor;

public class CanUpdateAppListAdapter extends AppListAdapter {

    public CanUpdateAppListAdapter(Context context, Cursor c) {
        super(context, c);
    }

    public CanUpdateAppListAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    public CanUpdateAppListAdapter(Context context, Cursor c, int flags) {
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
