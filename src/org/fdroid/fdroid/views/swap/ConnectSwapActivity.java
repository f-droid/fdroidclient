package org.fdroid.fdroid.views.swap;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public class ConnectSwapActivity extends FragmentActivity {

    private static final String STATE_CONFIRM = "startSwap";

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
        FragmentManager.BackStackEntry lastFragment = getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1);
        return lastFragment.getName();
    }

}
