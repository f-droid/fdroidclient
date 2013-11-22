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

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.fdroid.fdroid.DB.Repo;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;

import org.fdroid.fdroid.compat.ActionBarCompat;

public class ManageRepo extends ListActivity {

    private final int ADD_REPO = 1;
    private final int REM_REPO = 2;

    private boolean changed = false;

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
            // scheme should only ever be pure ASCII:
            String scheme = intent.getScheme().toLowerCase(Locale.ENGLISH);
            String fingerprint = uri.getUserInfo();
            if (scheme.equals("fdroidrepos") || scheme.equals("fdroidrepo")
                    || scheme.equals("https") || scheme.equals("http")) {
                String uriString = uri.toString().replace("fdroidrepo", "http").
                        replace(fingerprint + "@", "");
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
            if (repo.pubkey != null) {
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-1");
                    digest.update(Hasher.unhex(repo.pubkey));
                    byte[] fingerprint = digest.digest();
                    Formatter formatter = new Formatter(new StringBuilder());
                    formatter.format("%02X", fingerprint[0]);
                    for (int i = 1; i < fingerprint.length; i++) {
                        formatter.format(i % 5 == 0 ? " %02X" : ":%02X",
                                fingerprint[i]);
                    }
                    server_line.put("fingerprint", formatter.toString());
                    formatter.close();
                } catch (Exception e) {
                    Log.w("FDroid", "Unable to get certificate fingerprint.\n"
                            + Log.getStackTraceString(e));
                }
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

    protected void addRepo(String repoUri) {
        try {
            DB db = DB.getDB();
            db.addRepo(repoUri, null, null, 10, null, true);
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

    protected Repo getRepo(String repoUri, List<Repo> repos) {
        if (repoUri != null)
            for (Repo repo : repos)
                if (repoUri.equals(repo.address))
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

    private void showAddRepo(String uriString, String fingerprint) {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.addrepo, null);
        Builder p = new AlertDialog.Builder(this).setView(view);
        final AlertDialog alrt = p.create();
        final EditText uriEditText = (EditText) view.findViewById(R.id.edit_uri);
        final EditText fingerprintEditText = (EditText) view.findViewById(R.id.edit_fingerprint);

        alrt.setIcon(android.R.drawable.ic_menu_add);
        alrt.setTitle(getString(R.string.repo_add_title));
        alrt.setButton(DialogInterface.BUTTON_POSITIVE,
                getString(R.string.repo_add_add),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addRepo(uriEditText.getText().toString());
                        changed = true;
                        redraw();
                    }
                });

        alrt.setButton(DialogInterface.BUTTON_NEGATIVE,
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });
        alrt.show();

        List<Repo> repos = getRepos();
        Repo repo = getRepo(uriString, repos);
        if (repo != null) {
            TextView tv = (TextView) view.findViewById(R.id.repo_alert);
            tv.setVisibility(0);
            tv.setText(R.string.repo_exists);
            final Button addButton = alrt.getButton(DialogInterface.BUTTON_POSITIVE);
            addButton.setEnabled(false);
            final CheckBox overwriteCheckBox = (CheckBox) view.findViewById(R.id.overwrite_repo);
            overwriteCheckBox.setVisibility(0);
            overwriteCheckBox.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    addButton.setEnabled(overwriteCheckBox.isChecked());
                }
            });
            // TODO if address and fingerprint match, then enable existing repo
            // TODO if address matches but fingerprint doesn't, handle this with extra widgets
        }

        if (uriString != null)
            uriEditText.setText(uriString);
        if (fingerprint != null)
            fingerprintEditText.setText(fingerprint);
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
                            return;
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
