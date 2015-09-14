package org.fdroid.fdroid.views.fragments;

import android.net.Uri;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.views.AppListAdapter;
import org.fdroid.fdroid.views.InstalledAppListAdapter;

public class InstalledAppsFragment extends AppListFragment {

    @Override
    protected AppListAdapter getAppListAdapter() {
        return new InstalledAppListAdapter(getActivity(), null);
    }

    @Override
    protected String getEmptyMessage() {
        return getActivity().getString(R.string.empty_installed_app_list);
    }

    @Override
    protected String getFromTitle() {
        return getString(R.string.tab_installed_apps);
    }

    @Override
    protected Uri getDataUri() {
        return AppProvider.getInstalledUri();
    }

}
