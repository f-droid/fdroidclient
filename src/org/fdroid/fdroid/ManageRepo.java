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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import org.fdroid.fdroid.DB.Repo;
import org.fdroid.fdroid.compat.ActionBarCompat;
import org.fdroid.fdroid.compat.ClipboardCompat;
import org.fdroid.fdroid.views.RepoAdapter;
import org.fdroid.fdroid.views.RepoDetailsActivity;
import org.fdroid.fdroid.views.fragments.RepoDetailsFragment;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class ManageRepo extends ListActivity {

    private static final String DEFAULT_NEW_REPO_TEXT = "https://";
    private final int ADD_REPO     = 1;
    private final int UPDATE_REPOS = 2;

    /**
     * If we have a new repo added, or the address of a repo has changed, then
     * we when we're finished, we'll set this boolean to true in the intent
     * that we finish with, to signify that we want the main list of apps
     * updated.
     */
    public static final String REQUEST_UPDATE = "update";

    private enum PositiveAction {
        ADD_NEW, ENABLE, IGNORE
    }
    private PositiveAction positiveAction;

    private boolean changed = false;

    private RepoAdapter repoAdapter;

    /**
     * True if activity started with an intent such as from QR code. False if
     * opened from, e.g. the main menu.
     */
    private boolean isImportingRepo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ((FDroidApp) getApplication()).applyTheme(this);

        super.onCreate(savedInstanceState);

        ActionBarCompat abCompat = ActionBarCompat.create(this);
        abCompat.setDisplayHomeAsUpEnabled(true);

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

        /* let's see if someone is trying to send us a new repo */
        Intent intent = getIntent();
        /* an URL from a click or a QRCode scan */
        Uri uri = intent.getData();
        if (uri != null) {
            // scheme should only ever be pure ASCII aka Locale.ENGLISH
            String scheme = intent.getScheme().toLowerCase(Locale.ENGLISH);
            String fingerprint = uri.getUserInfo();
            String host = uri.getHost().toLowerCase(Locale.ENGLISH);
            if (scheme.equals("fdroidrepos") || scheme.equals("fdroidrepo")
                    || scheme.equals("https") || scheme.equals("http")) {

                isImportingRepo = true;

                // QRCode are more efficient in all upper case, so some incoming
                // URLs might be encoded in all upper case. Therefore, we allow
                // the standard paths to be encoded all upper case, then they'll
                // be forced to lower case. The scheme and host are downcased
                // just to make them more readable in the dialog.
                String uriString = uri.toString()
                        .replace(fingerprint + "@", "") // remove fingerprint
                        .replaceAll("/*$", "") // remove all trailing slashes
                        .replaceAll("/FDROID/REPO$", "/fdroid/repo")
                        .replaceAll("/FDROID/ARCHIVE$", "/fdroid/archive")
                        .replace(uri.getHost(), host) // downcase host name
                        .replace(intent.getScheme(), scheme) // downcase scheme
                        .replace("fdroidrepo", "http"); // make proper URL
                showAddRepo(uriString, fingerprint);
                Log.i("ManageRepo", uriString + " fingerprint: " + fingerprint);
            }
        }
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
	}

    private void refreshList() {
        repoAdapter.refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem updateItem = menu.add(Menu.NONE, UPDATE_REPOS, 1,
                R.string.menu_update_repo).setIcon(R.drawable.ic_menu_refresh);
        MenuItemCompat.setShowAsAction(updateItem,
                MenuItemCompat.SHOW_AS_ACTION_ALWAYS |
                MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);

        MenuItem addItem = menu.add(Menu.NONE, ADD_REPO, 1, R.string.menu_add_repo).setIcon(
                android.R.drawable.ic_menu_add);
        MenuItemCompat.setShowAsAction(addItem,
                MenuItemCompat.SHOW_AS_ACTION_ALWAYS |
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
                int repoId = data.getIntExtra(RepoDetailsActivity.DATA_REPO_ID, 0);
                remove(repoId);
            } else if (wasEnabled || wasDisabled || wasChanged) {
                changed = true;
            }
        }
    }

    private DB.Repo getRepoById(int repoId) {
        for (int i = 0; i < getListAdapter().getCount(); i ++) {
            DB.Repo repo = (DB.Repo)getListAdapter().getItem(i);
            if (repo.id == repoId) {
                return repo;
            }
        }
        return null;
    }

    private void remove(int repoId) {
        DB.Repo repo = getRepoById(repoId);
        if (repo == null) {
            return;
        }

        List<DB.Repo> reposToRemove = new ArrayList<DB.Repo>(1);
        reposToRemove.add(repo);
        try {
            DB db = DB.getDB();
            db.doDisableRepos(reposToRemove, true);
        } finally {
            DB.releaseDB();
        }
        refreshList();
    }

    protected List<Repo> getRepos() {
        List<Repo> repos = null;
        try {
            DB db = DB.getDB();
            repos = db.getRepos();
        } finally {
            DB.releaseDB();
        }
        return repos;
    }

    protected Repo getRepoByAddress(String address, List<Repo> repos) {
        if (address != null)
            for (Repo repo : repos)
                if (address.equals(repo.address))
                    return repo;
        return null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateRepos() {
        UpdateService.updateNow(this);
    }

    private void showAddRepo() {
        showAddRepo(getNewRepoUri(), null);
    }

    private void showAddRepo(String newAddress, String newFingerprint) {

        View view = getLayoutInflater().inflate(R.layout.addrepo, null);
        final AlertDialog alrt = new AlertDialog.Builder(this).setView(view).create();
        final EditText uriEditText = (EditText) view.findViewById(R.id.edit_uri);
        final EditText fingerprintEditText = (EditText) view.findViewById(R.id.edit_fingerprint);

        List<Repo> repos = getRepos();
        final Repo repo = newAddress != null && isImportingRepo ? getRepoByAddress(newAddress, repos) : null;

        alrt.setIcon(android.R.drawable.ic_menu_add);
        alrt.setTitle(getString(R.string.repo_add_title));
        alrt.setButton(DialogInterface.BUTTON_POSITIVE,
                getString(R.string.repo_add_add),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String fp = fingerprintEditText.getText().toString();

                        // the DB uses null for no fingerprint but the above
                        // code returns "" rather than null if its blank
                        if (fp.equals(""))
                            fp = null;

                        if (positiveAction == PositiveAction.ADD_NEW)
                            createNewRepo(uriEditText.getText().toString(), fp);
                        else if (positiveAction == PositiveAction.ENABLE)
                            createNewRepo(repo);
                    }
                });

        alrt.setButton(DialogInterface.BUTTON_NEGATIVE,
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setResult(Activity.RESULT_CANCELED);
                        if (isImportingRepo) {
                            finish();
                        }
                    }
                });
        alrt.show();

        final TextView overwriteMessage = (TextView) view.findViewById(R.id.overwrite_message);
        overwriteMessage.setVisibility(View.GONE);
        if (repo == null) {
            // no existing repo, add based on what we have
            positiveAction = PositiveAction.ADD_NEW;
        } else {
            // found the address in the DB of existing repos
            final Button addButton = alrt.getButton(DialogInterface.BUTTON_POSITIVE);
            alrt.setTitle(R.string.repo_exists);
            overwriteMessage.setVisibility(View.VISIBLE);
            if (repo.fingerprint == null && newFingerprint != null) {
                // we're upgrading from unsigned to signed repo
                overwriteMessage.setText(R.string.repo_exists_add_fingerprint);
                addButton.setText(R.string.add_key);
                positiveAction = PositiveAction.ADD_NEW;
            } else if (newFingerprint == null || newFingerprint.equals(repo.fingerprint)) {
                // this entry already exists and is not enabled, offer to enable it
                if (repo.inuse) {
                    alrt.dismiss();
                    Toast.makeText(this, R.string.repo_exists_and_enabled, Toast.LENGTH_LONG).show();
                    return;
                } else {
                    overwriteMessage.setText(R.string.repo_exists_enable);
                    addButton.setText(R.string.enable);
                    positiveAction = PositiveAction.ENABLE;
                }
            } else {
                // same address with different fingerprint, this could be
                // malicious, so force the user to manually delete the repo
                // before adding this one
                overwriteMessage.setTextColor(getResources().getColor(R.color.red));
                overwriteMessage.setText(R.string.repo_delete_to_overwrite);
                addButton.setText(R.string.overwrite);
                addButton.setEnabled(false);
                positiveAction = PositiveAction.IGNORE;
            }
        }

        if (newFingerprint != null)
            fingerprintEditText.setText(newFingerprint);

        if (newAddress != null) {
            // This trick of emptying text then appending,
            // rather than just setting in the first place,
            // is neccesary to move the cursor to the end of the input.
            uriEditText.setText("");
            uriEditText.append(newAddress);
        }
    }

    /**
     * Adds a new repo to the database.
     */
    private void createNewRepo(String address, String fingerprint) {
        try {
            DB db = DB.getDB();
            db.addRepo(address, null, null, 10, null, fingerprint, 0, true);
        } finally {
            DB.releaseDB();
        }
        finishedAddingRepo();
    }

    /**
     * Seeing as this repo already exists, we will force it to be enabled again.
     */
    private void createNewRepo(Repo repo) {
        repo.inuse = true;
        try {
            DB db = DB.getDB();
            db.updateRepoByAddress(repo);
        } finally {
            DB.releaseDB();
        }
        finishedAddingRepo();
    }

    /**
     * If started by an intent that expects a result (e.g. QR codes) then we
     * will set a result and finish. Otherwise, we'll refresh the list of
     * repos to reflect the newly created repo.
     */
    private void finishedAddingRepo() {
        changed = true;
        if (isImportingRepo) {
            setResult(Activity.RESULT_OK);
            finish();
        } else {
            refreshList();
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        super.onMenuItemSelected(featureId, item);

        if (item.getItemId() == ADD_REPO) {
            showAddRepo();
            return true;
        } else if (item.getItemId() == UPDATE_REPOS) {
            updateRepos();
            return true;
        }

        return false;
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
            text = DEFAULT_NEW_REPO_TEXT;
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
