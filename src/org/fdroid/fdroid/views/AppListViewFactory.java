package org.fdroid.fdroid.views;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import org.fdroid.fdroid.AppListAdapter;
import org.fdroid.fdroid.FDroid;
import org.fdroid.fdroid.R;

public class AppListViewFactory {

    private FDroid parent;

    public AppListViewFactory(FDroid parent) {
        this.parent = parent;
    }

    public View createAvailableView() {
        ListView list = createAppListView(parent.getAvailableAdapter());
        LinearLayout view = new LinearLayout(parent);
        view.setOrientation(LinearLayout.VERTICAL);
        Spinner cats = new Spinner(parent);

        // Giving it an ID lets the default save/restore state
        // functionality do its stuff.
        cats.setId(R.id.categorySpinner);
        cats.setAdapter(parent.getCategoriesAdapter());
        cats.setOnItemSelectedListener(parent);
        view.addView(cats, new ViewGroup.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        view.addView(list, new ViewGroup.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.FILL_PARENT));
        return view;
    }

    public View createInstalledView() {
        return createAppListView(parent.getInstalledAdapter());
    }

    public View createCanUpdateView() {
        return createAppListView(parent.getCanUpdateAdapter());
    }

    protected ListView createAppListView(AppListAdapter adapter) {
        ListView list = new ListView(parent);
        list.setFastScrollEnabled(true);
        list.setOnItemClickListener(parent);
        list.setAdapter(adapter);
        return list;
    }

}
