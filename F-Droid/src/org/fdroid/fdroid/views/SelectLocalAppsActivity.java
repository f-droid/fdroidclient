package org.fdroid.fdroid.views;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.PreferencesActivity;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.views.fragments.SelectLocalAppsFragment;

public class SelectLocalAppsActivity extends ActionBarActivity {

    private static final String TAG = "fdroid.SelectLocalAppsActivity";
    private SelectLocalAppsFragment selectLocalAppsFragment = null;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_local_apps_activity);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (selectLocalAppsFragment == null)
            selectLocalAppsFragment = (SelectLocalAppsFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_app_list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.select_local_apps_activity, menu);
        searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        searchView.setOnQueryTextListener(selectLocalAppsFragment);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.select_local_apps_action_mode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_update_repo:
                    setResult(RESULT_OK);
                    finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            setResult(RESULT_CANCELED);
            finish();
        }
    };
}
