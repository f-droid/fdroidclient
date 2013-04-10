package org.fdroid.fdroid.views;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import org.fdroid.fdroid.AppListAdapter;
import org.fdroid.fdroid.FDroid;
import org.fdroid.fdroid.R;

/**
 * Provides functionality to create the three lists of applications
 * required for the FDroid activity.
 */
public class AppListViewFactory {

    private FDroid parent;

    public AppListViewFactory(FDroid parent) {
        this.parent = parent;
    }

    public AppListView createAvailableView() {
        AppListView view = new AppListView(parent);
        view.setOrientation(LinearLayout.VERTICAL);

        Spinner spinner = new Spinner(parent);
        // Giving it an ID lets the default save/restore state
        // functionality do its stuff.
        spinner.setId(R.id.categorySpinner);
        spinner.setAdapter(parent.getCategoriesAdapter());
        spinner.setOnItemSelectedListener(parent);

        view.addView(
                spinner,
                new ViewGroup.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        ListView list = createAppListView(parent.getAvailableAdapter());
        view.setAppList(list);
        view.addView(
                list,
                new ViewGroup.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT));

        return view;
    }

    public AppListView createInstalledView() {
        return createPlainAppList(parent.getInstalledAdapter());
    }

    public AppListView createCanUpdateView() {
        return createPlainAppList(parent.getCanUpdateAdapter());
    }

    protected AppListView createPlainAppList(AppListAdapter adapter) {
        AppListView view = new AppListView(parent);
        ListView list = createAppListView(adapter);
        view.addView(
                list,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        view.setAppList(list);
        return view;
    }

    protected ListView createAppListView(AppListAdapter adapter) {
        ListView list = new ListView(parent);
        list.setFastScrollEnabled(true);
        list.setOnItemClickListener(parent);
        list.setAdapter(adapter);
        return list;
    }

}
