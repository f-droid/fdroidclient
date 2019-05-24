package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
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
import org.fdroid.fdroid.data.Schema.InstalledAppTable;
import org.fdroid.fdroid.localrepo.LocalRepoService;
import org.fdroid.fdroid.localrepo.SwapService;
import org.fdroid.fdroid.localrepo.SwapView;

public class SelectAppsView extends SwapView implements LoaderManager.LoaderCallbacks<Cursor> {

    public SelectAppsView(Context context) {
        super(context);
    }

    public SelectAppsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectAppsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public SelectAppsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private ListView listView;
    private AppListAdapter adapter;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        listView = findViewById(R.id.list);
        adapter = new AppListAdapter(listView, getContext(),
                getContext().getContentResolver().query(InstalledAppProvider.getContentUri(),
                        InstalledAppTable.Cols.ALL, null, null, null));

        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // either reconnect with an existing loader or start a new one
        getActivity().getSupportLoaderManager().initLoader(R.layout.swap_select_apps, null, this);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                toggleAppSelected(position);
            }
        });
    }

    private void toggleAppSelected(int position) {
        Cursor c = (Cursor) adapter.getItem(position);
        String packageName = c.getString(c.getColumnIndex(InstalledAppTable.Cols.Package.NAME));
        if (getActivity().getSwapService().hasSelectedPackage(packageName)) {
            getActivity().getSwapService().deselectPackage(packageName);
            adapter.updateCheckedIndicatorView(position, false);
        } else {
            getActivity().getSwapService().selectPackage(packageName);
            adapter.updateCheckedIndicatorView(position, true);
        }
        LocalRepoService.create(getContext(), getActivity().getSwapService().getAppsToSwap());
    }

    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        Uri uri;
        if (TextUtils.isEmpty(currentFilterString)) {
            uri = InstalledAppProvider.getContentUri();
        } else {
            uri = InstalledAppProvider.getSearchUri(currentFilterString);
        }
        return new CursorLoader(
                getActivity(),
                uri,
                InstalledAppTable.Cols.ALL,
                null,
                null,
                InstalledAppTable.Cols.APPLICATION_LABEL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        adapter.swapCursor(cursor);

        for (int i = 0; i < listView.getCount(); i++) {
            Cursor c = (Cursor) listView.getItemAtPosition(i);
            String packageName = c.getString(c.getColumnIndex(InstalledAppTable.Cols.Package.NAME));
            getActivity().getSwapService().ensureFDroidSelected();
            for (String selected : getActivity().getSwapService().getAppsToSwap()) {
                if (TextUtils.equals(packageName, selected)) {
                    listView.setItemChecked(i, true);
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    private class AppListAdapter extends CursorAdapter {

        @Nullable
        private LayoutInflater inflater;

        @Nullable
        private Drawable defaultAppIcon;

        @NonNull
        private final ListView listView;

        AppListAdapter(@NonNull ListView listView, @NonNull Context context, @Nullable Cursor c) {
            super(context, c, FLAG_REGISTER_CONTENT_OBSERVER);
            this.listView = listView;
        }

        @NonNull
        private LayoutInflater getInflater(Context context) {
            if (inflater == null) {
                Context themedContext = new ContextThemeWrapper(context, R.style.SwapTheme_AppList_ListItem);
                inflater = (LayoutInflater) themedContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

            TextView packageView = (TextView) view.findViewById(R.id.package_name);
            TextView labelView = (TextView) view.findViewById(R.id.application_label);
            ImageView iconView = (ImageView) view.findViewById(android.R.id.icon);

            String packageName = cursor.getString(cursor.getColumnIndex(InstalledAppTable.Cols.Package.NAME));
            String appLabel = cursor.getString(cursor.getColumnIndex(InstalledAppTable.Cols.APPLICATION_LABEL));

            Drawable icon;
            try {
                icon = context.getPackageManager().getApplicationIcon(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                icon = getDefaultAppIcon(context);
            }

            packageView.setText(packageName);
            labelView.setText(appLabel);
            iconView.setImageDrawable(icon);

            final int listPosition = cursor.getPosition();

            // Since v11, the Android SDK provided the ability to show selected list items
            // by highlighting their background. Prior to this, we need to handle this ourselves
            // by adding a checkbox which can toggle selected items.
            View checkBoxView = view.findViewById(R.id.checkbox);
            if (checkBoxView != null) {
                CheckBox checkBox = (CheckBox) checkBoxView;
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

            if (position >= firstListItemPosition && position <= lastListItemPosition) {
                final int childIndex = position - firstListItemPosition;
                updateCheckedIndicatorView(listView.getChildAt(childIndex), checked);
            }
        }

        private void updateCheckedIndicatorView(View view, boolean checked) {
            ImageView imageView = (ImageView) view.findViewById(R.id.checked);
            if (imageView != null) {
                int resource;
                int colour;
                if (checked) {
                    resource = R.drawable.ic_check_circle_white;
                    colour = getResources().getColor(R.color.swap_bright_blue);
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
