package org.fdroid.fdroid.views;

import android.content.Context;
import android.database.Cursor;

public class AvailableAppListAdapter extends AppListAdapter {

    public AvailableAppListAdapter(Context context, Cursor c) {
        super(context, c);
    }

    public AvailableAppListAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    public AvailableAppListAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    protected boolean showStatusUpdate() {
        return true;
    }

    @Override
    protected boolean showStatusInstalled() {
        return true;
    }
}
