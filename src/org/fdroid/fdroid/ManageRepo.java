/*
 * Copyright (C) 2009  Roberto Jacinto, roberto.jacinto@caixamagica.pt
 * Copyright (C) 2010  Ciaran Gultnieks, ciaran@ciarang.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.fdroid.fdroid.R;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class ManageRepo extends ListActivity {

    private DB db = null;

    private final int ADD_REPO = 1;
    private final int REM_REPO = 2;

    private boolean changed = false;

    private Vector<DB.Repo> repos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.repolist);

        db = new DB(this);

    }

    @Override
    protected void onResume() {

        super.onResume();
        redraw();
    }

    private void redraw() {
        repos = db.getRepos();

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
            result.add(server_line);
        }
        SimpleAdapter show_out = new SimpleAdapter(this, result,
                R.layout.repolisticons, new String[] { "address", "inuse" },
                new int[] { R.id.uri, R.id.img });

        setListAdapter(show_out);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        super.onListItemClick(l, v, position, id);
        db.changeServerStatus(repos.get(position).address);
        changed = true;
        redraw();
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, ADD_REPO, 1, R.string.menu_add_repo).setIcon(
                android.R.drawable.ic_menu_add);
        menu.add(Menu.NONE, REM_REPO, 2, R.string.menu_rem_repo).setIcon(
                android.R.drawable.ic_menu_close_clear_cancel);
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
            alrt.setButton(getString(R.string.repo_add_add),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            EditText uri = (EditText) alrt
                                    .findViewById(R.id.edit_uri);
                            String uri_str = uri.getText().toString();
                            db.addServer(uri_str, 10);
                            changed = true;
                            redraw();
                        }
                    });

            alrt.setButton2(getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            return;
                        }
                    });
            alrt.show();
            return true;

        case REM_REPO:
            final Vector<String> rem_lst = new Vector<String>();
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
                                rem_lst
                                        .addElement(repos.get(whichButton).address);
                            } else {
                                rem_lst
                                        .removeElement(repos.get(whichButton).address);
                            }
                        }
                    });
            builder.setPositiveButton(getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            db.removeServers(rem_lst);
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
        db.close();
        super.finish();
    }

}
