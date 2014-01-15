package org.fdroid.fdroid.views;

import android.content.Context;

public class CanUpdateAppListAdapter extends AppListAdapter {
    public CanUpdateAppListAdapter(Context context) {
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
