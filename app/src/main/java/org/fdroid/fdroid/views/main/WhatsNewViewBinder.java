package org.fdroid.fdroid.views.main;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Schema;
import org.fdroid.fdroid.views.whatsnew.WhatsNewAdapter;

/**
 * Loads a list of newly added or recently updated apps and displays them to the user.
 */
class WhatsNewViewBinder implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = 978015789;

    private final WhatsNewAdapter whatsNewAdapter;
    private final AppCompatActivity activity;

    WhatsNewViewBinder(final AppCompatActivity activity, FrameLayout parent) {
        this.activity = activity;

        View whatsNewView = activity.getLayoutInflater().inflate(R.layout.main_tab_whats_new, parent, true);

        whatsNewAdapter = new WhatsNewAdapter(activity);

        GridLayoutManager layoutManager = new GridLayoutManager(activity, 2);
        layoutManager.setSpanSizeLookup(new WhatsNewAdapter.SpanSizeLookup());

        RecyclerView appList = (RecyclerView) whatsNewView.findViewById(R.id.app_list);
        appList.setHasFixedSize(true);
        appList.setLayoutManager(layoutManager);
        appList.setAdapter(whatsNewAdapter);

        final SwipeRefreshLayout swipeToRefresh = (SwipeRefreshLayout) whatsNewView.findViewById(R.id.swipe_to_refresh);
        swipeToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeToRefresh.setRefreshing(false);
                UpdateService.updateNow(activity);
            }
        });

        activity.getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id != LOADER_ID) {
            return null;
        }

        return new CursorLoader(
                activity,
                AppProvider.getRecentlyUpdatedUri(),
                Schema.AppMetadataTable.Cols.ALL,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() != LOADER_ID) {
            return;
        }

        whatsNewAdapter.setAppsCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() != LOADER_ID) {
            return;
        }

        whatsNewAdapter.setAppsCursor(null);
    }
}
