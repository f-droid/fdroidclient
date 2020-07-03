/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 * Copyright (C) 2009  Roberto Jacinto, roberto.jacinto@caixamagica.pt
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

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
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Schema;


public class FragmentInstalled extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "FragmentInstalled";
    protected FragmentActivity activity;
    protected Context context;

    private InstalledAppListAdapter adapter;
    private RecyclerView appList;
    private TextView emptyState;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_manager_layout, container, false);

        this.activity = getActivity();
        this.context = getContext();


        adapter = new InstalledAppListAdapter(activity, this);

        appList = view.findViewById(R.id.app_list);
        appList.setHasFixedSize(true);
        appList.setLayoutManager(new LinearLayoutManager(context));
        appList.setAdapter(adapter);

        emptyState = view.findViewById(R.id.empty_state);

        view.findViewById(R.id.helpText).setVisibility(View.GONE);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Starts a new or restarts an existing Loader in this manager
        if (this.activity != null) {
            this.activity.getSupportLoaderManager().restartLoader(0, null, this);
        } else {
            Log.e(TAG, "activity is gone");
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri;

        if (Preferences.get().installedShowHidden()) {
            uri = AppProvider.getInstalledUri();
        } else {
            uri = AppProvider.getInstalledHiddenUri();
        }

        return new CursorLoader(
                context,
                uri,
                Schema.AppMetadataTable.Cols.ALL_PLUS_COLLECTION,
                null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        adapter.setApps(cursor);

        if (adapter.getItemCount() == 0) {
            appList.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText(R.string.empty_installed_app_list);
        } else {
            appList.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        adapter.setApps(null);
    }

    public void closeActionMode() {
        if (adapter != null) {
            adapter.closeActionMode();
        }
    }

}