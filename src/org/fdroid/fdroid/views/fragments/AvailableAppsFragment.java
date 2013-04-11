package org.fdroid.fdroid.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.views.AppListView;

public class AvailableAppsFragment extends AppListFragment implements AdapterView.OnItemSelectedListener {

    private ArrayAdapter<String> categoriesAdapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        AppListView view = new AppListView(getActivity());
        view.setOrientation(LinearLayout.VERTICAL);

        Spinner spinner = new Spinner(getActivity());
        // Giving it an ID lets the default save/restore state
        // functionality do its stuff.
        spinner.setId(R.id.categorySpinner);
        spinner.setAdapter(getCategoriesAdapter());
        spinner.setOnItemSelectedListener(this);

        view.addView(
                spinner,
                new ViewGroup.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        ListView list = createAppListView(getAppListAdapter());
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

    public ArrayAdapter<String> getCategoriesAdapter() {
        return categoriesAdapter;
    }

    public void setCategoriesAdapter(ArrayAdapter<String> categoriesAdapter) {
        this.categoriesAdapter = categoriesAdapter;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
