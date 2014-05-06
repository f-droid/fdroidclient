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

import android.annotation.TargetApi;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.View;
import android.widget.ListView;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.InstalledAppProvider;
import org.fdroid.fdroid.data.InstalledAppProvider.DataColumns;
import org.fdroid.fdroid.views.SelectLocalAppsActivity;

import java.util.HashSet;

public class SelectLocalAppsFragment extends ListFragment implements LoaderCallbacks<Cursor> {

    private SelectLocalAppsActivity selectLocalAppsActivity;
    private ActionMode mActionMode = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @TargetApi(11)
    // TODO replace with appcompat-v7
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(R.string.no_applications_found));

        selectLocalAppsActivity = (SelectLocalAppsActivity) getActivity();
        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_activated_1,
                null,
                new String[] {
                        InstalledAppProvider.DataColumns.APP_ID,
                },
                new int[] {
                        android.R.id.text1,
                },
                0);
        setListAdapter(adapter);
        setListShown(false);

        // either reconnect with an existing loader or start a new one
        getLoaderManager().initLoader(0, null, this);

        // build list of existing apps from what is on the file system
        if (FDroidApp.selectedApps == null) {
            FDroidApp.selectedApps = new HashSet<String>();
            for (String filename : FDroidApp.localRepo.repoDir.list()) {
                if (filename.matches(".*\\.apk")) {
                    String packageName = filename.substring(0, filename.indexOf("_"));
                    FDroidApp.selectedApps.add(packageName);
                }
            }
        }
    }

    @TargetApi(11)
    // TODO replace with appcompat-v7
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mActionMode == null)
            mActionMode = selectLocalAppsActivity
                    .startActionMode(selectLocalAppsActivity.mActionModeCallback);
        Cursor cursor = (Cursor) l.getAdapter().getItem(position);
        String packageName = cursor.getString(1);
        if (FDroidApp.selectedApps.contains(packageName)) {
            FDroidApp.selectedApps.remove(packageName);
        } else {
            FDroidApp.selectedApps.add(packageName);
        }
    }

    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(
                this.getActivity(),
                InstalledAppProvider.getContentUri(),
                InstalledAppProvider.DataColumns.ALL,
                null,
                null,
                InstalledAppProvider.DataColumns.APP_ID);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        ((SimpleCursorAdapter) this.getListAdapter()).swapCursor(cursor);

        ListView listView = getListView();
        int count = listView.getCount();
        String fdroid = loader.getContext().getPackageName();
        for (int i = 0; i < count; i++) {
            Cursor c = ((Cursor) listView.getItemAtPosition(i));
            String packageName = c.getString(c.getColumnIndex(DataColumns.APP_ID));
            if (TextUtils.equals(packageName, fdroid)) {
                listView.setItemChecked(i, true);
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
        ((SimpleCursorAdapter) this.getListAdapter()).swapCursor(null);
    }
}
