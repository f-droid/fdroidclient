
package org.fdroid.fdroid.views;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.SearchView;

import org.fdroid.fdroid.PreferencesActivity;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.views.fragments.SelectLocalAppsFragment;

@TargetApi(11)
// TODO replace with appcompat-v7
public class SelectLocalAppsActivity extends Activity {
    private static final String TAG = "SelectLocalAppsActivity";
    private SelectLocalAppsFragment selectLocalAppsFragment = null;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_local_apps_activity);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (selectLocalAppsFragment == null)
            selectLocalAppsFragment = (SelectLocalAppsFragment) getFragmentManager()
                    .findFragmentById(R.id.fragment_app_list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.select_local_apps_activity, menu);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setOnQueryTextListener(selectLocalAppsFragment);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
            case R.id.action_search:
                SearchView searchView = (SearchView) item.getActionView();
                searchView.setIconified(false);
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.select_local_apps_action_mode, menu);
            menu.findItem(R.id.action_search).setActionView(searchView);
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
