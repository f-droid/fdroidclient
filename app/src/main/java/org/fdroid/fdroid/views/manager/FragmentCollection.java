package org.fdroid.fdroid.views.manager;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.CollectionProvider;
import org.fdroid.fdroid.data.Schema;


public class FragmentCollection extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "FragmentCollection";
    protected FragmentActivity activity;
    protected Context context;

    private CollectionAppListAdapter adapter;
    private RecyclerView appList;
    private TextView emptyState;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_manager_layout, container, false);

        this.activity = getActivity();
        this.context = getContext();


        adapter = new CollectionAppListAdapter(activity, this);

        appList = view.findViewById(R.id.app_list);
        appList.setHasFixedSize(true);
        appList.setLayoutManager(new LinearLayoutManager(context));
        appList.setAdapter(adapter);

        emptyState = view.findViewById(R.id.empty_state);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Starts a new or restarts an existing Loader in this manager
        if (this.activity != null) {
            this.activity.getSupportLoaderManager().restartLoader(1, null, this);
        } else {
            Log.e(TAG, "activity is gone");
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                context,
                CollectionProvider.getCollectionUri(),
                Schema.AppMetadataTable.Cols.ALL_COLLECTION,
                null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        adapter.setApps(cursor);

        if (adapter.getItemCount() == 0) {
            appList.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText(R.string.empty_collection_app_list);
        } else {
            appList.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        adapter.setApps(null);
    }

}


