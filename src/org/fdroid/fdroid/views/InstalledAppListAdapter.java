package org.fdroid.fdroid.views;

import android.content.Context;

public class InstalledAppListAdapter extends AppListAdapter {
    public InstalledAppListAdapter(Context context) {
        super(context);
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
