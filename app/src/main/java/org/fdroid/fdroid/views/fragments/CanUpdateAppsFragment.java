package org.fdroid.fdroid.views.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.compat.CursorAdapterCompat;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.views.AppListAdapter;
import org.fdroid.fdroid.views.CanUpdateAppListAdapter;

public class CanUpdateAppsFragment extends AppListFragment {

    @Override
    protected AppListAdapter getAppListAdapter() {
        return CanUpdateAppListAdapter.create(getActivity(), null, CursorAdapterCompat.FLAG_AUTO_REQUERY);
    }

    @Override
    protected String getFromTitle() {
        return getString(R.string.tab_updates);
    }

    @Override
    protected Uri getDataUri() {
        return AppProvider.getCanUpdateUri();
    }

    @Override
    protected Uri getDataUri(String query) {
        return AppProvider.getSearchCanUpdateUri(query);
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.empty_can_update_app_list;
    }

    @Override
    protected int getNoSearchResultsMessage() {
        return R.string.empty_search_can_update_app_list;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.can_update_app_list, container, false);
    }

}
