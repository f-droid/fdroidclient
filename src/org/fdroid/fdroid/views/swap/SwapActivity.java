package org.fdroid.fdroid.views.swap;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;

public class SwapActivity extends ActionBarActivity implements SwapProcessManager {

    private static final String STATE_START_SWAP = "startSwap";
    private static final String STATE_SELECT_APPS = "selectApps";
    private static final String STATE_JOIN_WIFI = "joinWifi";
    private static final String STATE_NFC = "nfc";
    private static final String STATE_WIFI_QR = "wifiQr";

    private MenuItem nextMenuItem;
    private String nextMenuItemLabel;

    @Override
    public void onBackPressed() {
        if (currentState().equals(STATE_START_SWAP)) {
            finish();
        } else {
            super.onBackPressed();
        }
    }

    private String currentState() {
        FragmentManager.BackStackEntry lastFragment = getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1);
        return lastFragment.getName();
    }

    public void nextStep() {
        String current = currentState();
        if (current.equals(STATE_START_SWAP)) {
            onSelectApps();
        } else if (current.equals(STATE_SELECT_APPS)) {
            onJoinWifi();
        } else if (current.equals(STATE_JOIN_WIFI)) {
            onAttemptNfc();
        } else if (current.equals(STATE_NFC)) {
            onWifiQr();
        } else if (current.equals(STATE_WIFI_QR)) {
        }
        supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.swap_next, menu);
        nextMenuItem = menu.getItem(0);
        nextMenuItem.setVisible(false);
        MenuItemCompat.setShowAsAction(nextMenuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }

    private void hideNextButton() {
        nextMenuItemLabel = null;
        supportInvalidateOptionsMenu();
    }

    private void showNextButton() {
        nextMenuItemLabel = getString(R.string.next);
        supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (nextMenuItemLabel == null) {
            nextMenuItem.setVisible(false);
            return false;
        } else {
            nextMenuItem.setVisible(true);
            nextMenuItem.setTitle(nextMenuItemLabel);
            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == nextMenuItem.getItemId()) {
            nextStep();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new StartSwapFragment(), STATE_START_SWAP)
                    .addToBackStack(STATE_START_SWAP)
                    .commit();
            hideNextButton();

        }

    }

    private void onSelectApps() {

        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SelectAppsFragment(), STATE_SELECT_APPS)
                .addToBackStack(STATE_SELECT_APPS)
                .commit();
        showNextButton();

    }

    private void onJoinWifi() {

        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new JoinWifiFragment(), STATE_JOIN_WIFI)
                .addToBackStack(STATE_JOIN_WIFI)
                .commit();
        showNextButton();

    }

    public void onAttemptNfc() {
        if (Preferences.get().showNfcDuringSwap() && NfcSwapFragment.isNfcSupported(this)) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new NfcSwapFragment(), STATE_NFC)
                    .addToBackStack(STATE_NFC)
                    .commit();
            showNextButton();
        } else {
            onWifiQr();
        }
    }

    public void onBluetooth() {

    }

    public void onWifiQr() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new WifiQrFragment(), STATE_WIFI_QR)
                .addToBackStack(STATE_WIFI_QR)
                .commit();
        showNextButton();
    }

}
