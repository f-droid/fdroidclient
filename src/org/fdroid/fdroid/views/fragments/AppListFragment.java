package org.fdroid.fdroid.views.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import android.support.v4.app.Fragment;

import org.fdroid.fdroid.*;
import org.fdroid.fdroid.views.AppListAdapter;
import org.fdroid.fdroid.views.AppListView;

abstract class AppListFragment extends Fragment implements AdapterView.OnItemClickListener, Preferences.ChangeListener {

    protected FDroid parent;

    protected abstract AppListAdapter getAppListAdapter();

    protected abstract String getFromTitle();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Preferences.get().registerCompactLayoutChangeListener(this);
        Preferences.get().registerSmallDensityChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Preferences.get().unregisterCompactLayoutChangeListener(this);
        Preferences.get().unregisterSmallDensityChangeListener(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            parent = (FDroid)activity;
        } catch (ClassCastException e) {
            // I know fragments are meant to be activity agnostic, but I can't
            // think of a better way to share the one application list between
            // all three app list fragments.
            throw new RuntimeException(
                "AppListFragment can only be attached to FDroid activity. " +
                "Here it was attached to a " + activity.getClass() );
        }
    }

    public AppListManager getAppListManager() {
        return parent.getManager();
    }

    protected AppListView createPlainAppList() {
        AppListView view = new AppListView(getActivity());
        ListView list = createAppListView();
        view.addView(
                list,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        view.setAppList(list);
        return view;
    }

    protected ListView createAppListView() {
        ListView list = new ListView(getActivity());
        list.setFastScrollEnabled(true);
        list.setOnItemClickListener(this);
        list.setAdapter(getAppListAdapter());
        return list;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final DB.App app = (DB.App)getAppListAdapter().getItem(position);
        Intent intent = new Intent(getActivity(), AppDetails.class);
        intent.putExtra("appid", app.id);
        intent.putExtra("from", getFromTitle());
        startActivityForResult(intent, FDroid.REQUEST_APPDETAILS);
    }

    @Override
    public void onPreferenceChange() {
        getAppListAdapter().notifyDataSetChanged();
    }
}
