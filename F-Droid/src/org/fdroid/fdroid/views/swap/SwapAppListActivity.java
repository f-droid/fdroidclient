package org.fdroid.fdroid.views.swap;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import org.fdroid.fdroid.AppDetails;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.views.AppListAdapter;
import org.fdroid.fdroid.views.AvailableAppListAdapter;
import org.fdroid.fdroid.views.fragments.AppListFragment;

public class SwapAppListActivity extends ActionBarActivity {

    private static final String TAG = "SwapAppListActivity";

    public static final String EXTRA_REPO_ID = "repoId";

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

        long repoAddress = getIntent().getLongExtra(EXTRA_REPO_ID, -1);
        repo = RepoProvider.Helper.findById(this, repoAddress);
        if (repo == null) {
            Log.e(TAG, "Couldn't show swap app list for repo " + repoAddress);
            finish();
        }
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

        protected Intent getAppDetailsIntent() {
            Intent intent = new Intent(getActivity(), SwapAppDetails.class);
            intent.putExtra(EXTRA_REPO_ID, repo.getId());
            return intent;
        }

    }

    /**
     * Only difference from base class is that it navigates up to a different task.
     * It will go to the {@link org.fdroid.fdroid.views.swap.SwapAppListActivity}
     * whereas the baseclass will go back to the main list of apps. Need to juggle
     * the repoId in order to be able to return to an appropriately configured swap
     * list (see {@link org.fdroid.fdroid.views.swap.SwapAppListActivity.SwapAppListFragment#getAppDetailsIntent()}).
     */
    public static class SwapAppDetails extends AppDetails {

        private long repoId;

        @Override
        protected void onResume() {
            super.onResume();
            repoId = getIntent().getLongExtra(EXTRA_REPO_ID, -1);
        }

        @Override
        protected void navigateUp() {
            Intent parentIntent = NavUtils.getParentActivityIntent(this);
            parentIntent.putExtra(EXTRA_REPO_ID, repoId);
            NavUtils.navigateUpTo(this, parentIntent);
        }

    }

}
