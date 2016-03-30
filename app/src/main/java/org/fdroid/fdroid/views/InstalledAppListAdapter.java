package org.fdroid.fdroid.views;

import android.content.Context;
import android.database.Cursor;
import android.os.Build;

public class InstalledAppListAdapter extends AppListAdapter {

    public static InstalledAppListAdapter create(Context context, Cursor cursor, int flags)  {
        if (Build.VERSION.SDK_INT >= 11) {
            return new InstalledAppListAdapter(context, cursor, flags);
        } else {
            return new InstalledAppListAdapter(context, cursor);
        }
    }

    private InstalledAppListAdapter(Context context, Cursor c) {
        super(context, c);
    }

    public InstalledAppListAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    private InstalledAppListAdapter(Context context, Cursor c, int flags) {
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
