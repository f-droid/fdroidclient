package org.fdroid.fdroid.views.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.compat.ArrayAdapterCompat;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.views.AppListAdapter;
import org.fdroid.fdroid.views.AvailableAppListAdapter;

import java.util.ArrayList;
import java.util.List;

public class AvailableAppsFragment extends AppListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    public static final String PREFERENCES_FILE = "CategorySpinnerPosition";
    public static final String CATEGORY_KEY = "Selection";
    public static String DEFAULT_CATEGORY;

    private Spinner categorySpinner;
    private String currentCategory = null;
    private AppListAdapter adapter = null;

    @Override
    protected String getFromTitle() {
        return "Available";
    }

    @Override
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

        private ArrayAdapter<String> adapter;

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

        // attempt to translate category names with fallback to default name
        List<String> translatedCategories = new ArrayList<String>(categories.size());
        Resources res = getResources();
        for (String category : categories) {
            int id = res.getIdentifier(category.replace(" & ", "_"), "string", getActivity().getPackageName());
            translatedCategories.add(id == 0 ? category : getString(id));
        }

        categorySpinner = new Spinner(getActivity());
        // Giving it an ID lets the default save/restore state
        // functionality do its stuff.
        categorySpinner.setId(R.id.categorySpinner);
        // with holo, the menu gets lost since it looks the same as an app list item
        if (Build.VERSION.SDK_INT >= 14) {
            Drawable menuButton = getResources().getDrawable(android.R.drawable.btn_dropdown);
            if (TextUtils.equals("dark",
                    PreferenceManager.getDefaultSharedPreferences(getActivity())
                            .getString(Preferences.PREF_THEME, "dark"))) {
                menuButton.setAlpha(32); // make it darker via alpha
            }
            categorySpinner.setBackgroundDrawable(menuButton);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            getActivity(), android.R.layout.simple_spinner_item, translatedCategories);
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        getActivity().getContentResolver().registerContentObserver(
            AppProvider.getContentUri(), false, new CategoryObserver(adapter));

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                getListView().setSelection(0);
                setCurrentCategory(categories.get(pos));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                setCurrentCategory(null);
            }
        });
        return categorySpinner;
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

        // R.string.category_whatsnew is the default set in AppListManager
        DEFAULT_CATEGORY = getActivity().getString(R.string.category_whatsnew);

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

    private void setCurrentCategory(String category) {
        currentCategory = category;
        Log.d("FDroid", "Category '" + currentCategory + "' selected.");
        getLoaderManager().restartLoader(0, null, AvailableAppsFragment.this);
    }

    @Override
    public void onResume() {
        super.onResume();
        /* restore the saved Category Spinner position */
        Activity activity = getActivity();
        SharedPreferences p = activity.getSharedPreferences(PREFERENCES_FILE,
                Context.MODE_PRIVATE);
        currentCategory = p.getString(CATEGORY_KEY, DEFAULT_CATEGORY);
        for (int i = 0; i < categorySpinner.getCount(); i++) {
            if (currentCategory.equals(categorySpinner.getItemAtPosition(i).toString())) {
                categorySpinner.setSelection(i);
                break;
            }
        }
        setCurrentCategory(currentCategory);
    }

    @Override
    public void onPause() {
        super.onPause();
        /* store the Category Spinner position for when we come back */
        SharedPreferences p = getActivity().getSharedPreferences(PREFERENCES_FILE,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor e = p.edit();
        e.putString(CATEGORY_KEY, currentCategory);
        e.commit();
    }
}
