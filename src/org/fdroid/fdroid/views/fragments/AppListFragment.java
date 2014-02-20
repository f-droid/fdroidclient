package org.fdroid.fdroid.views.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import org.fdroid.fdroid.*;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.views.AppListAdapter;

abstract public class AppListFragment extends ListFragment implements
        AdapterView.OnItemClickListener,
        Preferences.ChangeListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String[] APP_PROJECTION = {
            AppProvider.DataColumns._ID,
            AppProvider.DataColumns.APP_ID,
            AppProvider.DataColumns.NAME,
            AppProvider.DataColumns.SUMMARY,
            AppProvider.DataColumns.IS_COMPATIBLE,
            AppProvider.DataColumns.LICENSE,
            AppProvider.DataColumns.ICON,
            AppProvider.DataColumns.ICON_URL,
            AppProvider.DataColumns.CURRENT_VERSION,
            AppProvider.DataColumns.CURRENT_VERSION_CODE,
            AppProvider.DataColumns.REQUIREMENTS, // Needed for filtering apps that require root.
    };

    public static final String APP_SORT = AppProvider.DataColumns.NAME;

    protected AppListAdapter appAdapter;

    protected abstract AppListAdapter getAppListAdapter();

    protected abstract String getFromTitle();

    protected abstract Uri getDataUri();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Can't do this in the onCreate view, because "onCreateView" which
        // returns the list view is "called between onCreate and
        // onActivityCreated" according to the docs.
        getListView().setFastScrollEnabled(true);
        getListView().setOnItemClickListener(this);
        getListView().setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), false, true));
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
        Preferences.get().registerCompactLayoutChangeListener(this);

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
        final String TRIED_EMPTY_UPDATE = "triedEmptyUpdate";
        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        boolean hasTriedEmptyUpdate = prefs.getBoolean(TRIED_EMPTY_UPDATE, false);
        if (!hasTriedEmptyUpdate) {
            Log.d("FDroid", "Empty app list, and we haven't done an update yet. Forcing repo update.");
            prefs.edit().putBoolean(TRIED_EMPTY_UPDATE, true).commit();
            UpdateService.updateNow(getActivity());
            return true;
        } else {
            Log.d("FDroid", "Empty app list, but it looks like we've had an update previously. Will not force repo update.");
            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Preferences.get().unregisterCompactLayoutChangeListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final App app = new App((Cursor)getListView().getItemAtPosition(position));
        Intent intent = new Intent(getActivity(), AppDetails.class);
        intent.putExtra("appid", app.id);
        intent.putExtra("from", getFromTitle());
        startActivityForResult(intent, FDroid.REQUEST_APPDETAILS);
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
        Uri uri = getDataUri();
        return new CursorLoader(
                getActivity(), uri, APP_PROJECTION, null, null, APP_SORT);
    }

}
