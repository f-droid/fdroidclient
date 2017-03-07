package org.fdroid.fdroid.views.myapps;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Schema;

public class MyAppsViewBinder implements LoaderManager.LoaderCallbacks<Cursor> {

    private final MyAppsAdapter adapter;

    private final Activity activity;

    public MyAppsViewBinder(AppCompatActivity activity, FrameLayout parent) {
        this.activity = activity;

        View myAppsView = activity.getLayoutInflater().inflate(R.layout.main_tabs, parent, true);

        adapter = new MyAppsAdapter(activity);

        RecyclerView list = (RecyclerView) myAppsView.findViewById(R.id.list);
        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(activity));
        list.setAdapter(adapter);

        LoaderManager loaderManager = activity.getSupportLoaderManager();
        loaderManager.initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                activity,
                AppProvider.getCanUpdateUri(),
                new String[]{
                        Schema.AppMetadataTable.Cols._ID, // Required for cursor loader to work.
                        Schema.AppMetadataTable.Cols.Package.PACKAGE_NAME,
                        Schema.AppMetadataTable.Cols.NAME,
                        Schema.AppMetadataTable.Cols.SUMMARY,
                        Schema.AppMetadataTable.Cols.IS_COMPATIBLE,
                        Schema.AppMetadataTable.Cols.LICENSE,
                        Schema.AppMetadataTable.Cols.ICON,
                        Schema.AppMetadataTable.Cols.ICON_URL,
                        Schema.AppMetadataTable.Cols.InstalledApp.VERSION_CODE,
                        Schema.AppMetadataTable.Cols.InstalledApp.VERSION_NAME,
                        Schema.AppMetadataTable.Cols.SuggestedApk.VERSION_NAME,
                        Schema.AppMetadataTable.Cols.SUGGESTED_VERSION_CODE,
                        Schema.AppMetadataTable.Cols.REQUIREMENTS, // Needed for filtering apps that require root.
                        Schema.AppMetadataTable.Cols.ANTI_FEATURES, // Needed for filtering apps that require anti-features.
                },
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        adapter.setApps(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.setApps(null);
    }
}
