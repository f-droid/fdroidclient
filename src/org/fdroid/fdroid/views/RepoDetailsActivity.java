package org.fdroid.fdroid.views;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import org.fdroid.fdroid.compat.ActionBarCompat;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.views.fragments.RepoDetailsFragment;

public class RepoDetailsActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long repoId = getIntent().getLongExtra(RepoDetailsFragment.ARG_REPO_ID, 0);

        if (savedInstanceState == null) {
            RepoDetailsFragment fragment = new RepoDetailsFragment();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment)
                .commit();
        }

        String[] projection = new String[] { RepoProvider.DataColumns.NAME };
        Repo repo = RepoProvider.Helper.findById(getContentResolver(), repoId, projection);

        ActionBarCompat.create(this).setDisplayHomeAsUpEnabled(true);
        setTitle(repo.getName());
    }

}
