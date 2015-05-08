package org.fdroid.fdroid.views.swap;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
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

    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = "SwapAppsList";

    private String mCurrentFilterString;

    @NonNull
    private final Set<String> previouslySelectedApps = new HashSet<>();

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
        menuInflater.inflate(R.menu.swap_next_search, menu);
        MenuItem nextMenuItem = menu.findItem(R.id.action_next);
        int flags = MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT;
        MenuItemCompat.setShowAsAction(nextMenuItem, flags);

        SearchView searchView = new SearchView(getActivity());

        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        MenuItemCompat.setActionView(searchMenuItem, searchView);
        MenuItemCompat.setShowAsAction(searchMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

        searchView.setOnQueryTextListener(this);
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

        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        setListAdapter(new AppListAdapter(listView, getActivity(), null));
        setListShown(false); // start out with a progress indicator

        // either reconnect with an existing loader or start a new one
        getLoaderManager().initLoader(0, null, this);

        // build list of existing apps from what is on the file system
        if (FDroidApp.selectedApps == null) {
            FDroidApp.selectedApps = new HashSet<>();
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
        // Ignore the headerView at position 0.
        if (position > 0) {
            toggleAppSelected(position);
        }
    }

    private void toggleAppSelected(int position) {
        Cursor c = (Cursor) getListAdapter().getItem(position - 1);
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
        ((AppListAdapter)getListAdapter()).swapCursor(cursor);

        ListView listView = getListView();
        String fdroid = loader.getContext().getPackageName();
        for (int i = 0; i < listView.getCount() - 1; i++) {
            Cursor c = ((Cursor) listView.getItemAtPosition(i + 1));
            String packageName = c.getString(c.getColumnIndex(InstalledAppProvider.DataColumns.APP_ID));
            if (TextUtils.equals(packageName, fdroid)) {
                listView.setItemChecked(i + 1, true); // always include FDroid
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
        ((AppListAdapter)getListAdapter()).swapCursor(null);
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

    @Override
    protected int getThemeStyle() {
        return R.style.SwapTheme_StartSwap;
    }

    @Override
    protected int getHeaderLayout() {
        return R.layout.swap_create_header;
    }

    private class AppListAdapter extends CursorAdapter {

        @SuppressWarnings("UnusedDeclaration")
        private static final String TAG = "AppListAdapter";

        @Nullable
        private LayoutInflater inflater;

        @Nullable
        private Drawable defaultAppIcon;

        @NonNull
        private final ListView listView;

        public AppListAdapter(@NonNull ListView listView, @NonNull Context context, @Nullable Cursor c) {
            super(context, c, FLAG_REGISTER_CONTENT_OBSERVER);
            this.listView = listView;
        }

        @NonNull
        private LayoutInflater getInflater(Context context) {
            if (inflater == null) {
                Context themedContext = new ContextThemeWrapper(context, R.style.SwapTheme_AppList_ListItem);
                inflater = (LayoutInflater)themedContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }
            return inflater;
        }

        @NonNull
        private Drawable getDefaultAppIcon(Context context) {
            if (defaultAppIcon == null) {
                defaultAppIcon = context.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
            }
            return defaultAppIcon;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = getInflater(context).inflate(R.layout.select_local_apps_list_item, parent, false);
            bindView(view, context, cursor);
            return view;
        }

        @Override
        public void bindView(final View view, final Context context, final Cursor cursor) {

            TextView packageView = (TextView)view.findViewById(R.id.package_name);
            TextView labelView = (TextView)view.findViewById(R.id.application_label);
            ImageView iconView = (ImageView)view.findViewById(android.R.id.icon);

            String packageName = cursor.getString(cursor.getColumnIndex(InstalledAppProvider.DataColumns.APP_ID));
            String appLabel = cursor.getString(cursor.getColumnIndex(InstalledAppProvider.DataColumns.APPLICATION_LABEL));

            Drawable icon;
            try {
                icon = context.getPackageManager().getApplicationIcon(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                icon = getDefaultAppIcon(context);
            }

            packageView.setText(packageName);
            labelView.setText(appLabel);
            iconView.setImageDrawable(icon);

            // Since v11, the Android SDK provided the ability to show selected list items
            // by highlighting their background. Prior to this, we need to handle this ourselves
            // by adding a checkbox which can toggle selected items.
            View checkBoxView = view.findViewById(R.id.checkbox);
            if (checkBoxView != null) {
                CheckBox checkBox = (CheckBox)checkBoxView;
                checkBox.setOnCheckedChangeListener(null);

                final int listPosition = cursor.getPosition() + 1; // To account for the header view.

                checkBox.setChecked(listView.isItemChecked(listPosition));
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        listView.setItemChecked(listPosition, isChecked);
                        toggleAppSelected(listPosition);
                    }
                });
            }
        }
    }

}
