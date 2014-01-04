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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.fdroid.fdroid.DB.Repo;
import org.fdroid.fdroid.compat.ActionBarCompat;

public class ManageRepo extends ListActivity {

    private final int ADD_REPO = 1;
    private final int REM_REPO = 2;

    private boolean changed = false;

    private enum PositiveAction {
        ADD_NEW, ENABLE, IGNORE
    }
    private PositiveAction positiveAction;

    private List<DB.Repo> repos;

    private static List<String> reposToDisable;
    private static List<String> reposToRemove;

    public void disableRepo(String address) {
        if (reposToDisable.contains(address)) return;
        reposToDisable.add(address);
    }

    public void removeRepo(String address) {
        if (reposToRemove.contains(address)) return;
        reposToRemove.add(address);
    }

    public void removeRepos(List<String> addresses) {
        for (String address : addresses)
            removeRepo(address);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ((FDroidApp) getApplication()).applyTheme(this);

        super.onCreate(savedInstanceState);
        ActionBarCompat abCompat = ActionBarCompat.create(this);
        abCompat.setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.repolist);

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

        reposToRemove = new ArrayList<String>();
        reposToDisable = new ArrayList<String>();

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
        redraw();
    }

    private void redraw() {
        try {
            DB db = DB.getDB();
            repos = db.getRepos();
        } finally {
            DB.releaseDB();
        }

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        Map<String, Object> server_line;

        for (DB.Repo repo : repos) {
            server_line = new HashMap<String, Object>();
            server_line.put("address", repo.address);
            if (repo.inuse) {
                server_line.put("inuse", R.drawable.btn_check_on);
            } else {
                server_line.put("inuse", R.drawable.btn_check_off);
            }
            if (repo.fingerprint != null) {
                server_line.put("fingerprint", repo.fingerprint);
            }
            result.add(server_line);
        }
        SimpleAdapter show_out = new SimpleAdapter(this, result,
                R.layout.repolisticons, new String[] { "address", "inuse",
                        "fingerprint" }, new int[] { R.id.uri, R.id.img,
                        R.id.fingerprint });

        setListAdapter(show_out);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        super.onListItemClick(l, v, position, id);
        try {
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        MenuItem item = menu.add(Menu.NONE, ADD_REPO, 1, R.string.menu_add_repo).setIcon(
                android.R.drawable.ic_menu_add);
        menu.add(Menu.NONE, REM_REPO, 2, R.string.menu_rem_repo).setIcon(
                android.R.drawable.ic_menu_close_clear_cancel);
        MenuItemCompat.setShowAsAction(item,
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM |
                MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }

    protected void addRepo(String repoUri, String fingerprint) {
        try {
            DB db = DB.getDB();
            db.addRepo(repoUri, null, null, 0, 10, null, fingerprint, 0, true);
        } finally {
            DB.releaseDB();
        }
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

    private void showAddRepo(String newAddress, String newFingerprint) {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.addrepo, null);
        Builder p = new AlertDialog.Builder(this).setView(view);
        final AlertDialog alrt = p.create();
        final EditText uriEditText = (EditText) view.findViewById(R.id.edit_uri);
        final EditText fingerprintEditText = (EditText) view.findViewById(R.id.edit_fingerprint);

        List<Repo> repos = getRepos();
        final Repo repo = getRepoByAddress(newAddress, repos);

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
                            addRepoPositiveAction(uriEditText.getText().toString(), fp, null);
                        else if (positiveAction == PositiveAction.ENABLE)
                            addRepoPositiveAction(null, null, repo);
                    }
                });

        alrt.setButton(DialogInterface.BUTTON_NEGATIVE,
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setResult(Activity.RESULT_CANCELED);
                        finish();
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

        if (newAddress != null)
            uriEditText.setText(newAddress);
        if (newFingerprint != null)
            fingerprintEditText.setText(newFingerprint);
    }

    private void addRepoPositiveAction(String address, String fingerprint, Repo repo) {
        if (address != null) {
            addRepo(address, fingerprint);
        } else if (repo != null) {
            // force-enable an existing repo
            repo.inuse = true;
            try {
                DB db = DB.getDB();
                db.updateRepoByAddress(repo);
            } finally {
                DB.releaseDB();
            }
        }
        changed = true;
        redraw();
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        super.onMenuItemSelected(featureId, item);

        switch (item.getItemId()) {
        case ADD_REPO:
            showAddRepo(null, null);
            return true;

        case REM_REPO:
            final List<String> rem_lst = new ArrayList<String>();
            CharSequence[] b = new CharSequence[repos.size()];
            for (int i = 0; i < repos.size(); i++) {
                b[i] = repos.get(i).address;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.repo_delete_title));
            builder.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
            builder.setMultiChoiceItems(b, null,
                    new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                int whichButton, boolean isChecked) {
                            if (isChecked) {
                                rem_lst.add(repos.get(whichButton).address);
                            } else {
                                rem_lst.remove(repos.get(whichButton).address);
                            }
                        }
                    });
            builder.setPositiveButton(getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            try {
                                DB db = DB.getDB();
                                removeRepos(rem_lst);
                            } finally {
                                DB.releaseDB();
                            }
                            changed = true;
                            redraw();
                        }
                    });
            builder.setNegativeButton(getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
            return true;
        }
        return true;
    }

    @Override
    public void finish() {
        if (!reposToRemove.isEmpty()) {
            try {
                DB db = DB.getDB();
                db.doDisableRepos(reposToRemove, true);
            } finally {
                DB.releaseDB();
            }
            ((FDroidApp) getApplication()).invalidateAllApps();
        }

        if (!reposToDisable.isEmpty()) {
            try {
                DB db = DB.getDB();
                db.doDisableRepos(reposToDisable, false);
            } finally {
                DB.releaseDB();
            }
            ((FDroidApp) getApplication()).invalidateAllApps();
        }

        Intent ret = new Intent();
        if (changed)
            ret.putExtra("update", true);
        this.setResult(RESULT_OK, ret);
        super.finish();
    }

}
