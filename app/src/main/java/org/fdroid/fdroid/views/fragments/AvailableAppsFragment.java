package org.fdroid.fdroid.views.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.compat.ArrayAdapterCompat;
import org.fdroid.fdroid.compat.CursorAdapterCompat;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.CategoryProvider;
import org.fdroid.fdroid.views.AppListAdapter;
import org.fdroid.fdroid.views.AvailableAppListAdapter;

import java.util.ArrayList;
import java.util.List;

public class AvailableAppsFragment extends AppListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "AvailableAppsFragment";

    private static final String PREFERENCES_FILE = "CategorySpinnerPosition";
    private static final String CATEGORY_KEY = "Selection";
    private static String defaultCategory;

    private List<String> categories;

    @Nullable
    private View categoryWrapper;

    @Nullable
    private Spinner categorySpinner;
    private String currentCategory;
    private AppListAdapter adapter;

    @Override
    protected String getFromTitle() {
        return getString(R.string.tab_available_apps);
    }

    @Override
    protected AppListAdapter getAppListAdapter() {
        if (adapter == null) {
            final AppListAdapter a = AvailableAppListAdapter.create(getActivity(), null, CursorAdapterCompat.FLAG_AUTO_REQUERY);
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

        private final ArrayAdapter<String> adapter;

        CategoryObserver(ArrayAdapter<String> adapter) {
            // Using Looper.getMainLooper() ensures that the onChange method is run on the main thread.
            super(new Handler(Looper.getMainLooper()));
            this.adapter = adapter;
        }

        @Override
        public void onChange(boolean selfChange) {
            final Activity activity = getActivity();
            if (!isAdded() || adapter == null || activity == null) {
                return;
            }

            // Because onChange is always invoked on the main thread (see constructor), we want to
            // run the database query on a background thread. Afterwards, the UI is updated
            // on a foreground thread.
            new AsyncTask<Void, Void, List<String>>() {
                @Override
                protected List<String> doInBackground(Void... params) {
                    return CategoryProvider.Helper.categories(activity);
                }

                @Override
                protected void onPostExecute(List<String> loadedCategories) {
                    adapter.clear();
                    categories = loadedCategories;
                    ArrayAdapterCompat.addAll(adapter, translateCategories(activity, loadedCategories));
                }
            }.execute();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            onChange(selfChange);
        }
    }

    /**
     * Attempt to translate category names with fallback to default name if no translation available
     */
    private static List<String> translateCategories(Context context, List<String> categories) {
        List<String> translatedCategories = new ArrayList<>(categories.size());
        Resources res = context.getResources();
        String pkgName = context.getPackageName();
        for (String category : categories) {
            String resId = category.replace(" & ", "_").replace(" ", "_").replace("'", "");
            int id = res.getIdentifier("category_" + resId, "string", pkgName);
            translatedCategories.add(id == 0 ? category : context.getString(id));
        }
        return translatedCategories;
    }

    private Spinner setupCategorySpinner(Spinner spinner) {

        categorySpinner = spinner;
        categorySpinner.setId(R.id.category_spinner);

        categories = CategoryProvider.Helper.categories(getActivity());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getActivity(), android.R.layout.simple_spinner_item, translateCategories(getActivity(), categories));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
        View view = inflater.inflate(R.layout.available_app_list, container, false);

        categoryWrapper = view.findViewById(R.id.category_wrapper);
        setupCategorySpinner((Spinner) view.findViewById(R.id.category_spinner));
        defaultCategory = CategoryProvider.Helper.getCategoryWhatsNew(getActivity());

        return view;
    }

    @Override
    protected Uri getDataUri() {
        if (currentCategory == null || currentCategory.equals(CategoryProvider.Helper.getCategoryAll(getActivity()))) {
            return AppProvider.getContentUri();
        }
        if (currentCategory.equals(CategoryProvider.Helper.getCategoryRecentlyUpdated(getActivity()))) {
            return AppProvider.getRecentlyUpdatedUri();
        }
        if (currentCategory.equals(CategoryProvider.Helper.getCategoryWhatsNew(getActivity()))) {
            return AppProvider.getNewlyAddedUri();
        }
        return AppProvider.getCategoryUri(currentCategory);
    }

    @Override
    protected Uri getDataUri(String query) {
        return AppProvider.getSearchUri(query, null);
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.empty_available_app_list;
    }

    @Override
    protected int getNoSearchResultsMessage() {
        return R.string.empty_search_available_app_list;
    }

    private void setCurrentCategory(String category) {
        currentCategory = category;
        Utils.debugLog(TAG, "Category '" + currentCategory + "' selected.");
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onResume() {
        /* restore the saved Category Spinner position */
        Activity activity = getActivity();
        SharedPreferences p = activity.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        currentCategory = p.getString(CATEGORY_KEY, defaultCategory);

        if (categorySpinner != null) {
            for (int i = 0; i < categorySpinner.getCount(); i++) {
                if (currentCategory.equals(categorySpinner.getItemAtPosition(i).toString())) {
                    categorySpinner.setSelection(i);
                    break;
                }
            }
        }

        setCurrentCategory(currentCategory);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        /* store the Category Spinner position for when we come back */
        SharedPreferences p = getActivity().getSharedPreferences(PREFERENCES_FILE,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor e = p.edit();
        e.putString(CATEGORY_KEY, currentCategory);
        e.apply();
    }

    @Override
    protected void onSearch() {
        if (categoryWrapper != null) {
            categoryWrapper.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSearchStopped() {
        if (categoryWrapper != null) {
            categoryWrapper.setVisibility(View.VISIBLE);
        }
    }
}
