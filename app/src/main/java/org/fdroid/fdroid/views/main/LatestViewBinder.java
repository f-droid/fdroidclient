package org.fdroid.fdroid.views.main;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema.AppMetadataTable;
import org.fdroid.fdroid.panic.HidingManager;
import org.fdroid.fdroid.views.apps.AppListActivity;

import java.util.Date;

/**
 * Loads a list of newly added or recently updated apps and displays them to the user.
 */
class LatestViewBinder implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = 978015789;

    private final LatestAdapter latestAdapter;
    private final AppCompatActivity activity;
    private final TextView emptyState;
    private final RecyclerView appList;

    private ProgressBar progressBar;

    LatestViewBinder(final AppCompatActivity activity, FrameLayout parent) {
        this.activity = activity;

        View latestView = activity.getLayoutInflater().inflate(R.layout.main_tab_latest, parent, true);

        latestAdapter = new LatestAdapter(activity);

        GridLayoutManager layoutManager = new GridLayoutManager(activity, 2);
        layoutManager.setSpanSizeLookup(new LatestAdapter.SpanSizeLookup());

        emptyState = (TextView) latestView.findViewById(R.id.empty_state);

        appList = (RecyclerView) latestView.findViewById(R.id.app_list);
        appList.setHasFixedSize(true);
        appList.setLayoutManager(layoutManager);
        appList.setAdapter(latestAdapter);

        final SwipeRefreshLayout swipeToRefresh = (SwipeRefreshLayout) latestView
                .findViewById(R.id.swipe_to_refresh);
        Utils.applySwipeLayoutColors(swipeToRefresh);
        swipeToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeToRefresh.setRefreshing(false);
                UpdateService.updateNow(activity);
            }
        });

        FloatingActionButton searchFab = (FloatingActionButton) latestView.findViewById(R.id.fab_search);
        searchFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startActivity(new Intent(activity, AppListActivity.class));
            }
        });
        searchFab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (Preferences.get().hideOnLongPressSearch()) {
                    HidingManager.showHideDialog(activity);
                    return true;
                } else {
                    return false;
                }
            }
        });

        activity.getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    /**
     * @see AppProvider#getLatestTabUri()
     */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id != LOADER_ID) {
            return null;
        }
        return new CursorLoader(
                activity,
                AppProvider.getLatestTabUri(),
                AppMetadataTable.Cols.ALL,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() != LOADER_ID) {
            return;
        }

        latestAdapter.setAppsCursor(cursor);

        if (latestAdapter.getItemCount() == 0) {
            emptyState.setVisibility(View.VISIBLE);
            appList.setVisibility(View.GONE);
            explainEmptyStateToUser();
        } else {
            emptyState.setVisibility(View.GONE);
            appList.setVisibility(View.VISIBLE);
        }
    }

    private void explainEmptyStateToUser() {
        if (Preferences.get().isIndexNeverUpdated() && UpdateService.isUpdating()) {
            if (progressBar != null) {
                return;
            }
            LinearLayout linearLayout = (LinearLayout) appList.getParent();
            progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleLarge);
            progressBar.setId(R.id.progress_bar);
            linearLayout.addView(progressBar);
            emptyState.setVisibility(View.GONE);
            appList.setVisibility(View.GONE);
            return;
        }

        StringBuilder emptyStateText = new StringBuilder();
        emptyStateText.append(activity.getString(R.string.latest__empty_state__no_recent_apps));
        emptyStateText.append("\n\n");

        int repoCount = RepoProvider.Helper.countEnabledRepos(activity);
        if (repoCount == 0) {
            emptyStateText.append(activity.getString(R.string.latest__empty_state__no_enabled_repos));
        } else {
            Date lastUpdate = RepoProvider.Helper.lastUpdate(activity);
            if (lastUpdate == null) {
                emptyStateText.append(activity.getString(R.string.latest__empty_state__never_updated));
            } else {
                emptyStateText.append(Utils.formatLastUpdated(activity.getResources(), lastUpdate));
            }
        }

        emptyState.setText(emptyStateText.toString());
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        if (loader.getId() != LOADER_ID) {
            return;
        }

        latestAdapter.setAppsCursor(null);
    }
}
