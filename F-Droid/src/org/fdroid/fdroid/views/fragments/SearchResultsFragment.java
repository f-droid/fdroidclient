package org.fdroid.fdroid.views.fragments;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import org.fdroid.fdroid.AppDetails;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.views.AppListAdapter;
import org.fdroid.fdroid.views.AvailableAppListAdapter;

public class SearchResultsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "SearchResultsFragment";

    private static final int REQUEST_APPDETAILS = 0;

    private AppListAdapter adapter;

    protected String getQuery() {
        Intent intent = getActivity().getIntent();
        String query = null;
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
        }
        if (query == null) {
            return "";
        }
        return query;
    }

    @Override
    public void onResume() {
        super.onResume();

        //Starts a new or restarts an existing Loader in this manager
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle data) {

        adapter = new AvailableAppListAdapter(getActivity(), null);
        setListAdapter(adapter);

        View view = inflater.inflate(R.layout.searchresults, null);
        updateSummary(view);

        return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = AppProvider.getSearchUri(getQuery());
        return new CursorLoader(
            getActivity(),
            uri,
            AppListFragment.APP_PROJECTION,
            null,
            null,
            AppListFragment.APP_SORT
        );
    }

    private void updateSummary() {
        updateSummary(getView());
    }

    private void updateSummary(View view) {

        String query = getQuery();

        if (query != null)
            query = query.trim();

        if (TextUtils.isEmpty(query))
            getActivity().finish();

        TextView tv = (TextView) view.findViewById(R.id.description);
        String headerText;
        int count = adapter.getCount();
        if (count == 0) {
            headerText = getString(R.string.searchres_noapps, query);
        } else if (count == 1) {
            headerText = getString(R.string.searchres_oneapp, query);
        } else {
            headerText = getString(R.string.searchres_napps, count, query);
        }
        tv.setText(headerText);
        Utils.debugLog(TAG, "Search for '" + query + "' returned " + count + " results");
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final App app = new App((Cursor) adapter.getItem(position));

        Intent intent = new Intent(getActivity(), AppDetails.class);
        intent.putExtra(AppDetails.EXTRA_APPID, app.id);
        startActivityForResult(intent, REQUEST_APPDETAILS);
        super.onListItemClick(l, v, position, id);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
        updateSummary();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }
}
