package org.fdroid.fdroid.views.swap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public class ConnectSwapActivity extends FragmentActivity {

    private static final String STATE_CONFIRM = "startSwap";
    private static final String STATE_APP_LIST = "swapAppList";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new ConfirmReceiveSwapFragment(), STATE_CONFIRM)
                    .addToBackStack(STATE_CONFIRM)
                    .commit();

        }

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

    public void onRepoUpdated() {

        Intent intent = new Intent(this, SwapAppListActivity.class);
        startActivity(intent);

    }
}
