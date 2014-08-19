package org.fdroid.fdroid.views.swap;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import org.fdroid.fdroid.R;

public class SwapActivity extends ActionBarActivity implements SwapProcessManager {

    private static final String STATE_JOIN_WIFI = "joinWifi";
    private static final String STATE_NFC = "nfc";
    private static final String STATE_WIFI_QR = "wifiQr";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.swap, menu);
        MenuItem next = menu.getItem(0);
        MenuItemCompat.setShowAsAction(next, MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_next) {
            moveToNext();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void moveToNext() {
        getSupportFragmentManager().popBackStack();
        FragmentManager.BackStackEntry lastFragment = getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1);
        String name = lastFragment.getName();
        switch (name) {
            case STATE_JOIN_WIFI:
                onAttemptNfc();
                break;
            case STATE_NFC:
                onWifiQr();
                break;
            case STATE_WIFI_QR:

                break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, new JoinWifiFragment(), STATE_JOIN_WIFI)
                    .addToBackStack(STATE_JOIN_WIFI)
                    .commit();

        }

    }

    @Override
    public void onAttemptNfc() {
        getSupportFragmentManager()
                .beginTransaction()
                .addToBackStack(STATE_NFC)
                .replace(android.R.id.content, new NfcSwapFragment(), STATE_NFC)
                .commit();
    }

    @Override
    public void onBluetooth() {

    }

    @Override
    public void onWifiQr() {
        getSupportFragmentManager()
                .beginTransaction()
                .addToBackStack(STATE_WIFI_QR)
                .replace(android.R.id.content, new WifiQrFragment(), STATE_WIFI_QR)
                .commit();
    }

}
