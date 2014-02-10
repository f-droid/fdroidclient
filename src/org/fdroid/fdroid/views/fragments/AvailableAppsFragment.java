package org.fdroid.fdroid.views.fragments;

import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.compat.ArrayAdapterCompat;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.views.AppListAdapter;
import org.fdroid.fdroid.views.AvailableAppListAdapter;

import java.util.List;

public class AvailableAppsFragment extends AppListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private String currentCategory = null;
    private AppListAdapter adapter = null;

    @Override
    protected String getFromTitle() {
        return "Available";
    }

    protected AppListAdapter getAppListAdapter() {
        if (adapter == null) {
            final AppListAdapter a = new AvailableAppListAdapter(getActivity(), null);
            Preferences.get().registerUpdateHistoryListener(new Preferences.ChangeListener() {
                @Override
                public void onPreferenceChange() {
                    a.notifyDataSetChanged();
                }
            });
            adapter = a;
        }
        return adapter;
    }

    private class CategoryObserver extends ContentObserver {

        private ArrayAdapter adapter;

        public CategoryObserver(ArrayAdapter<String> adapter) {
            super(null);
            this.adapter = adapter;
        }

        @Override
        public void onChange(boolean selfChange) {
            // Wanted to just do this update here, but android tells
            // me that "Only the original thread that created a view
            // hierarchy can touch its views."
            getActivity().runOnUiThread( new Runnable() {
                @Override
                public void run() {
                    adapter.clear();
                    List<String> catList = AppProvider.Helper.categories(getActivity());
                    ArrayAdapterCompat.addAll(adapter, catList);
                }
            });
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            onChange(selfChange);
        }
    }

    private Spinner createCategorySpinner() {

        final List<String> categories = AppProvider.Helper.categories(getActivity());

        Spinner spinner = new Spinner(getActivity());
        // Giving it an ID lets the default save/restore state
        // functionality do its stuff.
        spinner.setId(R.id.categorySpinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            getActivity(), android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        getActivity().getContentResolver().registerContentObserver(
            AppProvider.getContentUri(), false, new CategoryObserver(adapter));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                currentCategory = categories.get(pos);
                Log.d("FDroid", "Category '" + currentCategory + "' selected.");
                getLoaderManager().restartLoader(0, null, AvailableAppsFragment.this);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentCategory = null;
                Log.d("FDroid", "Select empty category.");
                getLoaderManager().restartLoader(0, null, AvailableAppsFragment.this);
            }
        });
        return spinner;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout view = new LinearLayout(getActivity());
        view.setOrientation(LinearLayout.VERTICAL);

        view.addView(
                createCategorySpinner(),
                new ViewGroup.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        ListView list = new ListView(getActivity());
        list.setId(android.R.id.list);
        list.setFastScrollEnabled(true);
        list.setOnItemClickListener(this);
        view.addView(
                list,
                new ViewGroup.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT));

        return view;
    }

    @Override
    protected Uri getDataUri() {
        if (currentCategory == null || currentCategory.equals(AppProvider.Helper.getCategoryAll(getActivity())))
            return AppProvider.getContentUri();
        else if (currentCategory.equals(AppProvider.Helper.getCategoryRecentlyUpdated(getActivity())))
            return AppProvider.getRecentlyUpdatedUri();
        else if (currentCategory.equals(AppProvider.Helper.getCategoryWhatsNew(getActivity())))
            return AppProvider.getNewlyAddedUri();
        else
            return AppProvider.getCategoryUri(currentCategory);
    }
}
