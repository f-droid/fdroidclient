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
    protected String getFromTitle() {
        return getString(R.string.inst);
    }

    @Override
    protected Uri getDataUri() {
        return AppProvider.getInstalledUri();
    }

}
