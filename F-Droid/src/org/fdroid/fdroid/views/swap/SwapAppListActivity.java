package org.fdroid.fdroid.views.swap;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;

import org.fdroid.fdroid.AppDetails;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.views.AppListAdapter;
import org.fdroid.fdroid.views.AvailableAppListAdapter;
import org.fdroid.fdroid.views.fragments.AppListFragment;

public class SwapAppListActivity extends ActionBarActivity {

    public static String EXTRA_REPO_ADDRESS = "repoAddress";

    private Repo repo;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {

            // Necessary to run on an Android 2.3.[something] device.
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    getSupportFragmentManager()
                        .beginTransaction()
                        .add(android.R.id.content, new SwapAppListFragment())
                        .commit();
                }
            });
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        String repoAddress = getIntent().getStringExtra(EXTRA_REPO_ADDRESS);
        repo = RepoProvider.Helper.findByAddress(this, repoAddress);
    }

    public Repo getRepo() {
        return repo;
    }

    public static class SwapAppListFragment extends AppListFragment {

        private Repo repo;

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            repo = ((SwapAppListActivity)activity).getRepo();
        }

        @Override
        protected int getHeaderLayout() {
            return R.layout.swap_success_header;
        }

        @Override
        protected AppListAdapter getAppListAdapter() {
            return new AvailableAppListAdapter(getActivity(), null);
        }

        @Nullable
        @Override
        protected String getEmptyMessage() {
            return getActivity().getString(R.string.empty_swap_app_list);
        }

        @Override
        protected String getFromTitle() {
            return getString(R.string.swap);
        }

        @Override
        protected Uri getDataUri() {
            return AppProvider.getRepoUri(repo);
        }

    }

    /**
     * Here so that the AndroidManifest.xml can specify the "parent" activity from this
     * can be different form the regular AppDetails. That is - the AppDetails goes back
     * to the main app list, but the SwapAppDetails will go back to the "Swap app list"
     * activity.
     */
    public static class SwapAppDetails extends AppDetails {}

}
