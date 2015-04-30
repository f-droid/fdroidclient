/*
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.fdroid.fdroid.views.fragments;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.InstalledAppProvider;
import org.fdroid.fdroid.data.InstalledAppProvider.DataColumns;
import org.fdroid.fdroid.localrepo.LocalRepoManager;
import org.fdroid.fdroid.views.SelectLocalAppsActivity;

import java.util.HashSet;
import java.util.Set;

public class SelectLocalAppsFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor>, OnQueryTextListener {

    private PackageManager packageManager;
    private Drawable defaultAppIcon;
    private SelectLocalAppsActivity selectLocalAppsActivity;
    private ActionMode mActionMode = null;
    private String mCurrentFilterString;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(R.string.no_applications_found));

        packageManager = getActivity().getPackageManager();
        defaultAppIcon = getActivity().getResources()
                .getDrawable(android.R.drawable.sym_def_app_icon);

        selectLocalAppsActivity = (SelectLocalAppsActivity) getActivity();
        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
                R.layout.select_local_apps_list_item,
                null,
                new String[] {
                        InstalledAppProvider.DataColumns.APPLICATION_LABEL,
                        InstalledAppProvider.DataColumns.APP_ID,
                },
                new int[] {
                        R.id.application_label,
                        R.id.package_name,
                },
                0);
        adapter.setViewBinder(new ViewBinder() {

            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == cursor.getColumnIndex(InstalledAppProvider.DataColumns.APP_ID)) {
                    String packageName = cursor.getString(columnIndex);
                    TextView textView = (TextView) view.findViewById(R.id.package_name);
                    textView.setText(packageName);
                    LinearLayout ll = (LinearLayout) view.getParent().getParent();
                    ImageView iconView = (ImageView) ll.getChildAt(0);
                    Drawable icon;
                    try {
                        icon = packageManager.getApplicationIcon(packageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        icon = defaultAppIcon;
                    }
                    iconView.setImageDrawable(icon);
                    return true;
                }
                return false;
            }
        });
        setListAdapter(adapter);
        setListShown(false); // start out with a progress indicator

        // either reconnect with an existing loader or start a new one
        getLoaderManager().initLoader(0, null, this);

        // build list of existing apps from what is on the file system
        if (FDroidApp.selectedApps == null) {
            Set<String> selectedApps = new HashSet<>();
            for (String filename : LocalRepoManager.get(selectLocalAppsActivity).repoDir.list()) {
                if (filename.matches(".*\\.apk")) {
                    String packageName = filename.substring(0, filename.indexOf("_"));
                    selectedApps.add(packageName);
                }
            }
            FDroidApp.selectedApps = selectedApps;
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mActionMode == null)
            mActionMode = selectLocalAppsActivity
                    .startSupportActionMode(selectLocalAppsActivity.mActionModeCallback);
        Cursor c = (Cursor) getListAdapter().getItem(position);
        String packageName = c.getString(c.getColumnIndex(DataColumns.APP_ID));
        if (FDroidApp.selectedApps.contains(packageName)) {
            FDroidApp.selectedApps.remove(packageName);
        } else {
            FDroidApp.selectedApps.add(packageName);
        }
    }

    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        Uri baseUri;
        if (TextUtils.isEmpty(mCurrentFilterString)) {
            baseUri = InstalledAppProvider.getContentUri();
        } else {
            baseUri = InstalledAppProvider.getSearchUri(mCurrentFilterString);
        }
        return new CursorLoader(
                this.getActivity(),
                baseUri,
                InstalledAppProvider.DataColumns.ALL,
                null,
                null,
                InstalledAppProvider.DataColumns.APPLICATION_LABEL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();
        adapter.swapCursor(cursor);

        ListView listView = getListView();
        int count = listView.getCount();
        String fdroid = loader.getContext().getPackageName();
        for (int i = 0; i < count; i++) {
            Cursor c = ((Cursor) listView.getItemAtPosition(i));
            String packageName = c.getString(c.getColumnIndex(DataColumns.APP_ID));
            if (TextUtils.equals(packageName, fdroid)) {
                listView.setItemChecked(i, true); // always include FDroid
            } else {
                for (String selected : FDroidApp.selectedApps) {
                    if (TextUtils.equals(packageName, selected)) {
                        listView.setItemChecked(i, true);
                    }
                }
            }
        }

        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();
        adapter.swapCursor(null);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
        if (mCurrentFilterString == null && newFilter == null) {
            return true;
        }
        if (mCurrentFilterString != null && mCurrentFilterString.equals(newFilter)) {
            return true;
        }
        mCurrentFilterString = newFilter;
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // this is not needed since we respond to every change in text
        return true;
    }

    public String getCurrentFilterString() {
        return mCurrentFilterString;
    }
}
