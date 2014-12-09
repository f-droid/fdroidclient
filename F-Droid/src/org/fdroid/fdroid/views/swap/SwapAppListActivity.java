package org.fdroid.fdroid.views.swap;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.views.AppListAdapter;
import org.fdroid.fdroid.views.AvailableAppListAdapter;
import org.fdroid.fdroid.views.fragments.AppListFragment;

public class SwapAppListActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, new SwapAppListFragment())
                    .commit();
        }

    }

    private static class SwapAppListFragment extends AppListFragment {

        @Override
        protected int getHeaderLayout() {
            return R.layout.swap_success_header;
        }

        @Override
        protected AppListAdapter getAppListAdapter() {
            return new AvailableAppListAdapter(getActivity(), null);
        }

        @Override
        protected String getFromTitle() {
            return getString(R.string.swap);
        }

        @Override
        protected Uri getDataUri() {
            return AppProvider.getCategoryUri("LocalRepo");
        }

    }

}
