package org.fdroid.fdroid.views.swap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import org.fdroid.fdroid.data.NewRepoConfig;
import org.fdroid.fdroid.data.Repo;

public class ConnectSwapActivity extends FragmentActivity {

    private static final String STATE_CONFIRM = "startSwap";

    /**
     * When connecting to a swap, we then go and initiate a connection with that
     * device and ask if it would like to swap with us. Upon receiving that request
     * and agreeing, we don't then want to be asked whether we want to swap back.
     * This flag protects against two devices continually going back and forth
     * among each other offering swaps.
     */
    public static final String EXTRA_PREVENT_FURTHER_SWAP_REQUESTS = "preventFurtherSwap";

    private ConfirmReceiveSwapFragment fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {

            fragment = new ConfirmReceiveSwapFragment();

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, fragment, STATE_CONFIRM)
                    .addToBackStack(STATE_CONFIRM)
                    .commit();

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only confirm the action, and then return a result...
        NewRepoConfig config = new NewRepoConfig(this, getIntent());
        fragment.setRepoConfig(config);
    }

    @Override
    public void onBackPressed() {
        if (currentState().equals(STATE_CONFIRM)) {
            finish();
        } else {
            super.onBackPressed();
        }
    }

    private String currentState() {
        int index = getSupportFragmentManager().getBackStackEntryCount() - 1;
        FragmentManager.BackStackEntry lastFragment = getSupportFragmentManager().getBackStackEntryAt(index);
        return lastFragment.getName();
    }

    public void onRepoUpdated(Repo repo) {
        Intent intent = new Intent(this, SwapAppListActivity.class);
        intent.putExtra(SwapAppListActivity.EXTRA_REPO_ID, repo.getId());
        startActivity(intent);
    }
}
