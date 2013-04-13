/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 * Copyright (C) 2009  Roberto Jacinto, roberto.jacinto@caixamagica.pt
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid;

import java.net.MalformedURLException;
import java.net.URL;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.fdroid.fdroid.compat.ClipboardCompat;
import org.fdroid.fdroid.views.RepoAdapter;
import org.fdroid.fdroid.views.RepoDetailsActivity;
import org.fdroid.fdroid.views.fragments.RepoDetailsFragment;

public class ManageRepo extends ListActivity {

    private final int ADD_REPO = 1;

    /**
     * If we have a new repo added, or the address of a repo has changed, then
     * we when we're finished, we'll set this boolean to true in the intent
     * that we finish with, to signify that we want the main list of apps
     * updated.
     */
    public static final String REQUEST_UPDATE = "update";

    private boolean changed = false;

    private RepoAdapter repoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        repoAdapter = new RepoAdapter(this);
        setListAdapter(repoAdapter);

        /*
        TODO: Find some other way to display this info, now that we use the ListView widgets...
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());

        TextView tv_lastCheck = (TextView)findViewById(R.id.lastUpdateCheck);
        long lastUpdate = prefs.getLong("lastUpdateCheck", 0);
        String s_lastUpdateCheck = "";
        if (lastUpdate == 0) {
        	s_lastUpdateCheck = getString(R.string.never);
        } else {
        	Date d = new Date(lastUpdate);
        	s_lastUpdateCheck = DateFormat.getDateFormat(this).format(d) + 
        			" " + DateFormat.getTimeFormat(this).format(d);
        }
        tv_lastCheck.setText(getString(R.string.last_update_check,s_lastUpdateCheck));

        */
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        super.onListItemClick(l, v, position, id);

        DB.Repo repo = (DB.Repo)getListView().getItemAtPosition(position);
        editRepo(repo);

        /*try {
            DB db = DB.getDB();
            String address = repos.get(position).address;
            db.changeServerStatus(address);
            // TODO: Disabling and re-enabling a repo will delete its apks too.
            disableRepo(address);
        } finally {
            DB.releaseDB();
        }
        changed = true;
        redraw();
        repoAdapter.refresh();*/
	}

    private void refreshList() {
        repoAdapter.refresh();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem addItem = menu.add(Menu.NONE, ADD_REPO, 1, R.string.menu_add_repo).setIcon(
                android.R.drawable.ic_menu_add);
        MenuItemCompat.setShowAsAction(addItem,
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM |
                MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }

    public static final int SHOW_REPO_DETAILS = 1;

    public void editRepo(DB.Repo repo) {
        Log.d("FDroid", "Showing details screen for repo: '" + repo + "'.");
        Intent intent = new Intent(this, RepoDetailsActivity.class);
        intent.putExtra(RepoDetailsFragment.ARG_REPO_ID, repo.id);
        startActivityForResult(intent, SHOW_REPO_DETAILS);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SHOW_REPO_DETAILS && resultCode == RESULT_OK) {

            boolean wasDeleted  = data.getBooleanExtra(RepoDetailsActivity.ACTION_IS_DELETED, false);
            boolean wasEnabled  = data.getBooleanExtra(RepoDetailsActivity.ACTION_IS_ENABLED, false);
            boolean wasDisabled = data.getBooleanExtra(RepoDetailsActivity.ACTION_IS_DISABLED, false);
            boolean wasChanged  = data.getBooleanExtra(RepoDetailsActivity.ACTION_IS_CHANGED, false);

            if (wasDeleted) {
                refreshList();
            } else if (wasEnabled || wasDisabled || wasChanged) {
                changed = true;
            }
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        super.onMenuItemSelected(featureId, item);

        switch (item.getItemId()) {
        case ADD_REPO:
            promptForNewRepo();
            return true;
        }
        return true;
    }

    private void createNewRepo(String uri) {
        try {
            DB db = DB.getDB();
            db.addRepo(uri, null, null, 10, null, true);
        } finally {
            DB.releaseDB();
        }
        changed = true;
        refreshList();
    }

    private void promptForNewRepo() {
        View view = getLayoutInflater().inflate(R.layout.addrepo, null);
        final EditText inputUri = (EditText)view.findViewById(R.id.edit_uri);
        inputUri.setText(getNewRepoUri());
        final AlertDialog alert = new AlertDialog.Builder(this).setView(view).create();

        alert.setIcon(android.R.drawable.ic_menu_add);
        alert.setTitle(getString(R.string.repo_add_title));
        alert.setButton(DialogInterface.BUTTON_POSITIVE,
                getString(R.string.repo_add_add),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        createNewRepo(inputUri.getText().toString());
                    }
                });

        alert.setButton(DialogInterface.BUTTON_NEGATIVE,
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing...
                    }
                });
        alert.show();
    }

    /**
     * If there is text in the clipboard, and it looks like a URL, use that.
     * Otherwise return "https://".
     */
    private String getNewRepoUri() {
        ClipboardCompat clipboard = ClipboardCompat.create(this);
        String text = clipboard.getText();
        if (text != null) {
            try {
                new URL(text);
            } catch (MalformedURLException e) {
                text = null;
            }
        }

        if (text == null) {
            text = "https://";
        }
        return text;
    }

    @Override
    public void finish() {
        Intent ret = new Intent();
        if (changed) {
            Log.i("FDroid", "Repo details have changed, prompting for update.");
            ret.putExtra(REQUEST_UPDATE, true);
        }
        setResult(RESULT_OK, ret);
        super.finish();
    }

    /**
     * NOTE: If somebody toggles a repo off then on again, it will have removed
     * all apps from the index when it was toggled off, so when it is toggled on
     * again, then it will require a refresh.
     *
     * Previously, I toyed with the idea of remembering whether they had
     * toggled on or off, and then only actually performing the function when
     * the activity stopped, but I think that will be problematic. What about
     * when they press the home button, or edit a repos details? It will start
     * to become somewhat-random as to when the actual enabling, disabling is
     * performed.
     *
     * So now, it just does the disable as soon as the user clicks "Off" and
     * then removes the apps. To compensate for the removal of apps from
     * index, it notifies the user via a toast that the apps have been removed.
     * Also, as before, it will still prompt the user to update the repos if
     * you toggled on on.
     */
    public void setRepoEnabled(DB.Repo repo, boolean enabled) {
        FDroidApp app = (FDroidApp)getApplication();
        if (enabled) {
            repo.enable(app);
            changed = true;
        } else {
            repo.disable(app);
            String notification = getString(R.string.repo_disabled_notification, repo.toString());
            Toast.makeText(this, notification, 3000).show();
        }
    }

}
