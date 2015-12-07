package org.fdroid.fdroid.views.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;

import org.fdroid.fdroid.AppDetails;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.views.AppListAdapter;

public abstract class AppListFragment extends ListFragment implements
        AdapterView.OnItemClickListener,
        Preferences.ChangeListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "AppListFragment";

    private static final int REQUEST_APPDETAILS = 0;

    public static final String[] APP_PROJECTION = {
        AppProvider.DataColumns._ID, // Required for cursor loader to work.
        AppProvider.DataColumns.APP_ID,
        AppProvider.DataColumns.NAME,
        AppProvider.DataColumns.SUMMARY,
        AppProvider.DataColumns.IS_COMPATIBLE,
        AppProvider.DataColumns.LICENSE,
        AppProvider.DataColumns.ICON,
        AppProvider.DataColumns.ICON_URL,
        AppProvider.DataColumns.InstalledApp.VERSION_CODE,
        AppProvider.DataColumns.InstalledApp.VERSION_NAME,
        AppProvider.DataColumns.SuggestedApk.VERSION,
        AppProvider.DataColumns.SUGGESTED_VERSION_CODE,
        AppProvider.DataColumns.IGNORE_ALLUPDATES,
        AppProvider.DataColumns.IGNORE_THISUPDATE,
        AppProvider.DataColumns.REQUIREMENTS, // Needed for filtering apps that require root.
    };

    public static final String APP_SORT = AppProvider.DataColumns.NAME;

    protected AppListAdapter appAdapter;

    @Nullable private String searchQuery;

    protected abstract AppListAdapter getAppListAdapter();

    protected abstract String getFromTitle();

    protected abstract Uri getDataUri();

    protected abstract Uri getDataUri(String query);

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Can't do this in the onCreate view, because "onCreateView" which
        // returns the list view is "called between onCreate and
        // onActivityCreated" according to the docs.
        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        //Starts a new or restarts an existing Loader in this manager
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appAdapter = getAppListAdapter();

        if (appAdapter.getCount() == 0) {
            updateEmptyRepos();
        }

        setListAdapter(appAdapter);
    }

    /**
     * The first time the app is run, we will have an empty app list.
     * If this is the case, we will attempt to update with the default repo.
     * However, if we have tried this at least once, then don't try to do
     * it automatically again, because the repos or internet connection may
     * be bad.
     */
    public boolean updateEmptyRepos() {
        final String triedEmptyUpdate = "triedEmptyUpdate";
        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        boolean hasTriedEmptyUpdate = prefs.getBoolean(triedEmptyUpdate, false);
        if (!hasTriedEmptyUpdate) {
            Utils.debugLog(TAG, "Empty app list, and we haven't done an update yet. Forcing repo update.");
            prefs.edit().putBoolean(triedEmptyUpdate, true).commit();
            UpdateService.updateNow(getActivity());
            return true;
        }
        Utils.debugLog(TAG, "Empty app list, but it looks like we've had an update previously. Will not force repo update.");
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Cursor is null in the swap list when touching the first item.
        Cursor cursor = (Cursor) getListView().getItemAtPosition(position);
        if (cursor != null) {
            final App app = new App(cursor);
            Intent intent = getAppDetailsIntent();
            intent.putExtra(AppDetails.EXTRA_APPID, app.id);
            intent.putExtra(AppDetails.EXTRA_FROM, getFromTitle());
            startActivityForResult(intent, REQUEST_APPDETAILS);
        }
    }

    protected Intent getAppDetailsIntent() {
        return new Intent(getActivity(), AppDetails.class);
    }

    @Override
    public void onPreferenceChange() {
        getAppListAdapter().notifyDataSetChanged();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        appAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        appAdapter.swapCursor(null);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = TextUtils.isEmpty(searchQuery) ? getDataUri() : getDataUri(searchQuery);
        return new CursorLoader(
                getActivity(), uri, APP_PROJECTION, null, null, APP_SORT);
    }

    public void updateSearchQuery(@Nullable String query) {
        searchQuery = query;
        if (isAdded()) {
            getLoaderManager().restartLoader(0, null, this);
        }
    }
}
