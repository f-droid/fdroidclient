package org.fdroid.fdroid.views.fragments;

import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import org.fdroid.fdroid.AppListAdapter;
import org.fdroid.fdroid.views.AppListView;

abstract class AppListFragment extends Fragment implements AdapterView.OnItemClickListener {

    private AppListAdapter appListAdapter;

    public AppListAdapter getAppListAdapter() {
        return appListAdapter;
    }

    public AppListFragment setAppListAdapter(AppListAdapter adapter) {
        appListAdapter = adapter;
        return this;
    }

    protected AppListView createPlainAppList(AppListAdapter adapter) {
        AppListView view = new AppListView(getActivity());
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
        ListView list = new ListView(getActivity());
        list.setFastScrollEnabled(true);
        list.setOnItemClickListener(this);
        list.setAdapter(adapter);
        return list;
    }
}
