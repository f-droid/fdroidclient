package org.fdroid.fdroid.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import org.fdroid.fdroid.views.AppListAdapter;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.views.AppListView;

public class AvailableAppsFragment extends AppListFragment implements AdapterView.OnItemSelectedListener {

    @Override
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos,
            long id) {
        String category = parent.getItemAtPosition(pos).toString();
        getAppListManager().setCurrentCategory(category);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    protected AppListAdapter getAppListAdapter() {
        return getAppListManager().getAvailableAdapter();
    }

    @Override
    protected String getFromTitle() {
        return getAppListManager().getCurrentCategory();
    }
}
