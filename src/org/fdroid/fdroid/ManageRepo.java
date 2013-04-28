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
import java.util.Map;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class ManageRepo extends ListActivity {

    private final int ADD_REPO = 1;
    private final int REM_REPO = 2;

    private boolean changed = false;

    private List<DB.Repo> repos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.repolist);

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());

        TextView tv_lastCheck = (TextView)findViewById(R.id.lastUpdateCheck);
        long lastUpdate = prefs.getLong("lastUpdateCheck", 0);
        String s_lastUpdateCheck = "";
        if(lastUpdate == 0) {
        	s_lastUpdateCheck = getString(R.string.never);
        } else {
        	Date d = new Date(lastUpdate);
        	s_lastUpdateCheck = DateFormat.getDateFormat(this).format(d) + 
        			" " + DateFormat.getTimeFormat(this).format(d);
        }
        tv_lastCheck.setText(getString(R.string.last_update_check,s_lastUpdateCheck));
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
            db.changeServerStatus(repos.get(position).address);
        } finally {
            DB.releaseDB();
        }
        changed = true;
        redraw();
    }

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

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        super.onMenuItemSelected(featureId, item);
        LayoutInflater li = LayoutInflater.from(this);

        switch (item.getItemId()) {
        case ADD_REPO:
            View view = li.inflate(R.layout.addrepo, null);
            Builder p = new AlertDialog.Builder(this).setView(view);
            final AlertDialog alrt = p.create();

            alrt.setIcon(android.R.drawable.ic_menu_add);
            alrt.setTitle(getString(R.string.repo_add_title));
            alrt.setButton(DialogInterface.BUTTON_POSITIVE,
                    getString(R.string.repo_add_add),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            EditText uri = (EditText) alrt
                                    .findViewById(R.id.edit_uri);
                            String uri_str = uri.getText().toString();
                            try {
                                DB db = DB.getDB();
                                db.addRepo(uri_str, 10, null, true);
                            } finally {
                                DB.releaseDB();
                            }
                            changed = true;
                            redraw();
                        }
                    });

            alrt.setButton(DialogInterface.BUTTON_NEGATIVE,
                    getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            return;
                        }
                    });
            alrt.show();
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
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            try {
                                DB db = DB.getDB();
                                db.removeRepos(rem_lst);
                            } finally {
                                DB.releaseDB();
                            }
                            changed = true;
                            redraw();
                        }
                    });
            builder.setNegativeButton(getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
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
        Intent ret = new Intent();
        if (changed)
            ret.putExtra("update", true);
        this.setResult(RESULT_OK, ret);
        super.finish();
    }

}
