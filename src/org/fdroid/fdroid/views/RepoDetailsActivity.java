package org.fdroid.fdroid.views;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import org.fdroid.fdroid.DB;
import org.fdroid.fdroid.compat.ActionBarCompat;
import org.fdroid.fdroid.views.fragments.RepoDetailsFragment;

public class RepoDetailsActivity extends FragmentActivity implements RepoDetailsFragment.OnRepoChangeListener {

    public static final String ACTION_IS_DELETED = "isDeleted";
    public static final String ACTION_IS_ENABLED = "isEnabled";
    public static final String ACTION_IS_DISABLED = "isDisabled";
    public static final String ACTION_IS_CHANGED = "isChanged";

    public static final String DATA_REPO_ID     = "repoId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            RepoDetailsFragment fragment = new RepoDetailsFragment();
            fragment.setRepoChangeListener(this);
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment)
                .commit();
        }

        ActionBarCompat.create(this).setDisplayHomeAsUpEnabled(true);
    }

    private void finishWithAction(String actionName) {
        Intent data = new Intent();
        data.putExtra(actionName, true);
        data.putExtra(DATA_REPO_ID, getIntent().getIntExtra(RepoDetailsFragment.ARG_REPO_ID, -1));
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onDeleteRepo(DB.Repo repo) {
        finishWithAction(ACTION_IS_DELETED);
    }

    @Override
    public void onRepoDetailsChanged(DB.Repo repo) {
        // Do nothing...
    }

    @Override
    public void onEnableRepo(DB.Repo repo) {
        finishWithAction(ACTION_IS_ENABLED);
    }

    @Override
    public void onDisableRepo(DB.Repo repo) {
        finishWithAction(ACTION_IS_DISABLED);
    }

    @Override
    public void onUpdatePerformed(DB.Repo repo) {
        // do nothing - the actual update is done by the repo fragment...
    }

}
