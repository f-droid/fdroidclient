package org.fdroid.fdroid.views.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.fdroid.fdroid.AppListAdapter;
import org.fdroid.fdroid.AppListManager;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.views.AppListView;

public class AvailableAppsFragment extends AppListFragment implements AdapterView.OnItemSelectedListener {

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        AppListView view = new AppListView(getActivity());
        view.setOrientation(LinearLayout.VERTICAL);

        Spinner spinner = new Spinner(getActivity());
        // Giving it an ID lets the default save/restore state
        // functionality do its stuff.
        spinner.setId(R.id.categorySpinner);
        spinner.setAdapter(getAppListManager().getCategoriesAdapter());
        spinner.setOnItemSelectedListener(this);

        view.addView(
                spinner,
                new ViewGroup.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        ListView list = createAppListView();
        view.setAppList(list);
        view.addView(
                list,
                new ViewGroup.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT));

        return view;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos,
            long id) {
        getAppListManager().setCurrentCategory(parent.getItemAtPosition(pos).toString());
        getAppListManager().repopulateLists();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    protected AppListAdapter getAppListAdapter() {
        return getAppListManager().getAvailableAdapter();
    }
}
