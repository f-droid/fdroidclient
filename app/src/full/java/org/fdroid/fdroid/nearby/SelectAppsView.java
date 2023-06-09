package org.fdroid.fdroid.nearby;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SelectAppsView extends SwapView {

    public SelectAppsView(Context context) {
        super(context);
    }

    public SelectAppsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectAppsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SelectAppsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private ListView listView;
    private AppListAdapter adapter;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        listView = findViewById(R.id.list);
        List<PackageInfo> packages = getContext().getPackageManager().getInstalledPackages(0);
        adapter = new AppListAdapter(listView, packages);

        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        listView.setOnItemClickListener((parent, v, position, id) -> toggleAppSelected(position));
        afterAppsLoaded();
    }

    @Override
    public void setCurrentFilterString(String currentFilterString) {
        super.setCurrentFilterString(currentFilterString);
        adapter.setSearchTerm(currentFilterString);
    }

    private void toggleAppSelected(int position) {
        String packageName = adapter.getItem(position).packageName;
        if (getActivity().getSwapService().hasSelectedPackage(packageName)) {
            getActivity().getSwapService().deselectPackage(packageName);
        } else {
            getActivity().getSwapService().selectPackage(packageName);
        }
        LocalRepoService.create(getContext(), getActivity().getSwapService().getAppsToSwap());
    }

    public void afterAppsLoaded() {
        for (int i = 0; i < listView.getCount(); i++) {
            InstalledApp app = (InstalledApp) listView.getItemAtPosition(i);
            getActivity().getSwapService().ensureFDroidSelected();
            for (String selected : getActivity().getSwapService().getAppsToSwap()) {
                if (TextUtils.equals(app.packageName, selected)) {
                    listView.setItemChecked(i, true);
                }
            }
        }
    }

    private class AppListAdapter extends BaseAdapter {

        private final Context context = SelectAppsView.this.getContext();
        @Nullable
        private LayoutInflater inflater;

        @Nullable
        private Drawable defaultAppIcon;

        @NonNull
        private final ListView listView;

        private final List<InstalledApp> allPackages;
        private final List<InstalledApp> filteredPackages = new ArrayList<>();

        AppListAdapter(@NonNull ListView listView, List<PackageInfo> packageInfos) {
            this.listView = listView;
            allPackages = new ArrayList<>(packageInfos.size());
            for (PackageInfo packageInfo : packageInfos) {
                allPackages.add(new InstalledApp(context, packageInfo));
            }
            filteredPackages.addAll(allPackages);
        }

        void setSearchTerm(@Nullable String searchTerm) {
            filteredPackages.clear();
            if (TextUtils.isEmpty(searchTerm)) {
                filteredPackages.addAll(allPackages);
            } else {
                String query = requireNonNull(searchTerm).toLowerCase(Locale.US);
                for (InstalledApp app : allPackages) {
                    if (app.name.toLowerCase(Locale.US).contains(query)) {
                        filteredPackages.add(app);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        private LayoutInflater getInflater(Context context) {
            if (inflater == null) {
                Context themedContext = new ContextThemeWrapper(context, R.style.SwapTheme_AppList_ListItem);
                inflater = ContextCompat.getSystemService(themedContext, LayoutInflater.class);
            }
            return inflater;
        }

        private Drawable getDefaultAppIcon(Context context) {
            if (defaultAppIcon == null) {
                defaultAppIcon = ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon);
            }
            return defaultAppIcon;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView == null ?
                    getInflater(context).inflate(R.layout.select_local_apps_list_item, parent, false) :
                    convertView;
            bindView(view, context, position);
            return view;
        }

        public void bindView(final View view, final Context context, final int position) {
            InstalledApp app = getItem(position);

            TextView packageView = (TextView) view.findViewById(R.id.package_name);
            TextView labelView = (TextView) view.findViewById(R.id.application_label);
            ImageView iconView = (ImageView) view.findViewById(android.R.id.icon);

            Drawable icon;
            try {
                icon = context.getPackageManager().getApplicationIcon(app.packageName);
            } catch (PackageManager.NameNotFoundException e) {
                icon = getDefaultAppIcon(context);
            }

            packageView.setText(app.packageName);
            labelView.setText(app.name);
            iconView.setImageDrawable(icon);

            // Since v11, the Android SDK provided the ability to show selected list items
            // by highlighting their background. Prior to this, we need to handle this ourselves
            // by adding a checkbox which can toggle selected items.
            View checkBoxView = view.findViewById(R.id.checkbox);
            if (checkBoxView != null) {
                CheckBox checkBox = (CheckBox) checkBoxView;
                checkBox.setOnCheckedChangeListener(null);

                checkBox.setChecked(listView.isItemChecked(position));
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    listView.setItemChecked(position, isChecked);
                    toggleAppSelected(position);
                });
            }
        }

        @Override
        public int getCount() {
            return filteredPackages.size();
        }

        @Override
        public InstalledApp getItem(int position) {
            return filteredPackages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }
    }

    private static class InstalledApp {
        final String packageName;
        final String name;

        InstalledApp(String packageName, String name) {
            this.packageName = packageName;
            this.name = name;
        }

        InstalledApp(Context context, PackageInfo packageInfo) {
            this(packageInfo.packageName, Utils.getApplicationLabel(context, packageInfo.packageName));
        }
    }

}
