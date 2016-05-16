package org.fdroid.fdroid.views;

import android.content.Context;
import android.database.Cursor;

public class InstalledAppListAdapter extends AppListAdapter {

    public InstalledAppListAdapter(Context context, Cursor c) {
        super(context, c);
    }

    public InstalledAppListAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    public InstalledAppListAdapter(Context context, Cursor c, int flags) {
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
