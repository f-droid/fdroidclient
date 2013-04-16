package org.fdroid.fdroid.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.ListView;

/**
 * There are three main app-lists in the UI:
 *  - Available
 *  - Installed
 *  - Apps which can be updated
 * This class provides a View which knows about these app lists, but can have
 * different contents (e.g. a drop down list of categories). It allows us to
 * get a reference to the selected item in the FDroid Activity, without having
 * to know which list we are actually looking at.
 */
public class AppListView extends LinearLayout {

    private ListView appList;

    public AppListView(Context context) {
        super(context);
    }

    public AppListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAppList(ListView appList) {
        this.appList = appList;
    }

    public ListView getAppList() {
        return appList;
    }
}
