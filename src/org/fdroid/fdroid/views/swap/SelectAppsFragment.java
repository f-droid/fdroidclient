package org.fdroid.fdroid.views.swap;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.InstalledAppProvider;
import org.fdroid.fdroid.localrepo.LocalRepoManager;
import org.fdroid.fdroid.views.fragments.ThemeableListFragment;

import java.util.HashSet;
import java.util.Set;

public class SelectAppsFragment extends ThemeableListFragment
    implements LoaderManager.LoaderCallbacks<Cursor>, SearchView.OnQueryTextListener {

    private PackageManager packageManager;
    private Drawable defaultAppIcon;
    private ActionMode mActionMode = null;
    private String mCurrentFilterString;
    private Set<String> previouslySelectedApps = new HashSet<String>();

    public Set<String> getSelectedApps() {
        return FDroidApp.selectedApps;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.swap_next, menu);
        MenuItem nextMenuItem = menu.findItem(R.id.action_next);
        int flags = MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT;
        MenuItemCompat.setShowAsAction(nextMenuItem, flags);
    }

    @Override
    public void onResume() {
        super.onResume();
        previouslySelectedApps.clear();
        if (FDroidApp.selectedApps != null) {
            previouslySelectedApps.addAll(FDroidApp.selectedApps);
        }
    }

    public boolean hasSelectionChanged() {

        Set<String> currentlySelected = getSelectedApps();
        if (currentlySelected.size() != previouslySelectedApps.size()) {
            return true;
        }

        for (String current : currentlySelected) {
            boolean found = false;
            for (String previous : previouslySelectedApps) {
                if (current.equals(previous)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(R.string.no_applications_found));

        packageManager = getActivity().getPackageManager();
        defaultAppIcon = getResources().getDrawable(android.R.drawable.sym_def_app_icon);

        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                new ContextThemeWrapper(getActivity(), R.style.SwapTheme_AppList_ListItem),
                R.layout.select_local_apps_list_item,
                null,
                new String[] {
                        InstalledAppProvider.DataColumns.APPLICATION_LABEL,
                        InstalledAppProvider.DataColumns.APP_ID,
                },
                new int[] {
                        R.id.application_label,
                        R.id.package_name,
                });
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

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
            FDroidApp.selectedApps = new HashSet<String>();
            for (String filename : LocalRepoManager.get(getActivity()).repoDir.list()) {
                if (filename.matches(".*\\.apk")) {
                    String packageName = filename.substring(0, filename.indexOf("_"));
                    FDroidApp.selectedApps.add(packageName);
                }
            }
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Cursor c = (Cursor) l.getAdapter().getItem(position);
        String packageName = c.getString(c.getColumnIndex(InstalledAppProvider.DataColumns.APP_ID));
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
        ((SimpleCursorAdapter) this.getListAdapter()).swapCursor(cursor);

        ListView listView = getListView();
        String fdroid = loader.getContext().getPackageName();
        for (int i = 0; i < listView.getCount() - 1; i++) {
            Cursor c = ((Cursor) listView.getItemAtPosition(i + 1));
            String packageName = c.getString(c.getColumnIndex(InstalledAppProvider.DataColumns.APP_ID));
            if (TextUtils.equals(packageName, fdroid)) {
                listView.setItemChecked(i, true); // always include FDroid
            } else {
                for (String selected : FDroidApp.selectedApps) {
                    if (TextUtils.equals(packageName, selected)) {
                        listView.setItemChecked(i + 1, true);
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

    @Override
    protected int getThemeStyle() {
        return R.style.SwapTheme_StartSwap;
    }

    @Override
    protected int getHeaderLayout() {
        return R.layout.swap_create_header;
    }
}
