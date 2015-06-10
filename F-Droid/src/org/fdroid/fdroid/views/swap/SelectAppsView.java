package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
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
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.InstalledAppProvider;
import org.fdroid.fdroid.localrepo.SwapManager;

public class SelectAppsView extends ListView implements
        SwapWorkflowActivity.InnerView,
        LoaderManager.LoaderCallbacks<Cursor>,
        SearchView.OnQueryTextListener {

    public SelectAppsView(Context context) {
        super(context);
    }

    public SelectAppsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectAppsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SelectAppsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private SwapWorkflowActivity getActivity() {
        return (SwapWorkflowActivity)getContext();
    }

    private SwapManager getState() {
        return getActivity().getState();
    }

    private static final int LOADER_INSTALLED_APPS = 253341534;

    private AppListAdapter adapter;
    private String mCurrentFilterString;
    private final Presenter presenter = new Presenter();

    public static class Presenter {

        private SelectAppsView view;

        public void setView(@NonNull SelectAppsView view) {
            this.view = view;
        }

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        adapter = new AppListAdapter(this, getContext(),
                getContext().getContentResolver().query(InstalledAppProvider.getContentUri(), InstalledAppProvider.DataColumns.ALL, null, null, null));

        // Has to be _before_ "setAdapter()", as per the API docs.
        addHeaderView(inflate(getContext(), R.layout.swap_create_header, null), null, false);

        setAdapter(adapter);
        setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // either reconnect with an existing loader or start a new one
        getActivity().getSupportLoaderManager().initLoader(LOADER_INSTALLED_APPS, null, this);

        setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                if (position > 0) {
                    // Ignore the headerView at position 0.
                    toggleAppSelected(position);
                }
            }
        });

        presenter.setView(this);
    }

    @Override
    public boolean buildMenu(Menu menu, @NonNull MenuInflater inflater) {

        inflater.inflate(R.menu.swap_next_search, menu);
        MenuItem nextMenuItem = menu.findItem(R.id.action_next);
        int flags = MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT;
        MenuItemCompat.setShowAsAction(nextMenuItem, flags);
        nextMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                getActivity().onAppsSelected();
                return true;
            }
        });

        SearchView searchView = new SearchView(getActivity());

        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        MenuItemCompat.setActionView(searchMenuItem, searchView);
        MenuItemCompat.setShowAsAction(searchMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public int getStep() {
        return SwapManager.STEP_SELECT_APPS;
    }

    @Override
    public int getPreviousStep() {
        return SwapManager.STEP_INTRO;
    }

    private void toggleAppSelected(int position) {
        Cursor c = (Cursor) adapter.getItem(position - 1);
        String packageName = c.getString(c.getColumnIndex(InstalledAppProvider.DataColumns.APP_ID));
        if (getState().hasSelectedPackage(packageName)) {
            getState().deselectPackage(packageName);
            adapter.updateCheckedIndicatorView(position, false);
        } else {
            getState().selectPackage(packageName);
            adapter.updateCheckedIndicatorView(position, true);
        }

    }

    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        Uri uri;
        if (TextUtils.isEmpty(mCurrentFilterString)) {
            uri = InstalledAppProvider.getContentUri();
        } else {
            uri = InstalledAppProvider.getSearchUri(mCurrentFilterString);
        }
        return new CursorLoader(
                getActivity(),
                uri,
                InstalledAppProvider.DataColumns.ALL,
                null,
                null,
                InstalledAppProvider.DataColumns.APPLICATION_LABEL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        adapter.swapCursor(cursor);

        for (int i = 0; i < getCount() - 1; i++) {
            Cursor c = ((Cursor) getItemAtPosition(i + 1));
            String packageName = c.getString(c.getColumnIndex(InstalledAppProvider.DataColumns.APP_ID));
            getState().ensureFDroidSelected();
            for (String selected : getState().getAppsToSwap()) {
                if (TextUtils.equals(packageName, selected)) {
                    setItemChecked(i + 1, true);
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
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
        getActivity().getSupportLoaderManager().restartLoader(LOADER_INSTALLED_APPS, null, this);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // this is not needed since we respond to every change in text
        return true;
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

            final int listPosition = cursor.getPosition() + 1; // To account for the header view.

            // Since v11, the Android SDK provided the ability to show selected list items
            // by highlighting their background. Prior to this, we need to handle this ourselves
            // by adding a checkbox which can toggle selected items.
            View checkBoxView = view.findViewById(R.id.checkbox);
            if (checkBoxView != null) {
                CheckBox checkBox = (CheckBox)checkBoxView;
                checkBox.setOnCheckedChangeListener(null);

                checkBox.setChecked(listView.isItemChecked(listPosition));
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        listView.setItemChecked(listPosition, isChecked);
                        toggleAppSelected(listPosition);
                    }
                });
            }

            updateCheckedIndicatorView(view, listView.isItemChecked(listPosition));
        }

        public void updateCheckedIndicatorView(int position, boolean checked) {
            final int firstListItemPosition = listView.getFirstVisiblePosition();
            final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

            if (position >= firstListItemPosition && position <= lastListItemPosition ) {
                final int childIndex = position - firstListItemPosition;
                updateCheckedIndicatorView(listView.getChildAt(childIndex), checked);
            }
        }

        private void updateCheckedIndicatorView(View view, boolean checked) {
            ImageView imageView = (ImageView)view.findViewById(R.id.checked);
            if (imageView != null) {
                int resource;
                int colour;
                if (checked) {
                    resource = R.drawable.ic_check_circle_white;
                    colour = getResources().getColor(R.color.fdroid_blue);
                } else {
                    resource = R.drawable.ic_add_circle_outline_white;
                    colour = 0xFFD0D0D4;
                }
                imageView.setImageDrawable(getResources().getDrawable(resource));
                imageView.setColorFilter(colour, PorterDuff.Mode.MULTIPLY);
            }
        }
    }

}
