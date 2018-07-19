/*
 * Copyright (C) 2016 Blue Jay Wireless
 * Copyright (C) 2018 Senecto Limited
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.fdroid.fdroid.views;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import org.apache.commons.io.IOUtils;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.installer.InstallHistoryService;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class InstallHistoryActivity extends AppCompatActivity {
    public static final String TAG = "InstallHistoryActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_history);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.install_history));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String text = "";
        try {
            ContentResolver resolver = getContentResolver();

            Cursor cursor = resolver.query(InstallHistoryService.LOG_URI, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                cursor.close();
            }

            ParcelFileDescriptor pfd = resolver.openFileDescriptor(InstallHistoryService.LOG_URI, "r");
            FileDescriptor fd = pfd.getFileDescriptor();
            FileInputStream fileInputStream = new FileInputStream(fd);
            text = IOUtils.toString(fileInputStream, Charset.defaultCharset());
        } catch (IOException | SecurityException | IllegalStateException e) {
            e.printStackTrace();
        }
        TextView textView = findViewById(R.id.text);
        textView.setText(text);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.install_history, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_share:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Repos:\n");
                for (Repo repo : RepoProvider.Helper.all(this)) {
                    if (repo.inuse) {
                        stringBuilder.append("* ");
                        stringBuilder.append(repo.address);
                        stringBuilder.append('\n');
                    }
                }
                ShareCompat.IntentBuilder intentBuilder = ShareCompat.IntentBuilder.from(this)
                        .setStream(InstallHistoryService.LOG_URI)
                        .setSubject(getString(R.string.send_history_csv, getString(R.string.app_name)))
                        .setChooserTitle(R.string.send_install_history)
                        .setText(stringBuilder.toString())
                        .setType("text/plain");
                Intent intent = intentBuilder.getIntent();
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
                break;
            case R.id.menu_delete:
                getContentResolver().delete(InstallHistoryService.LOG_URI, null, null);
                TextView textView = findViewById(R.id.text);
                textView.setText("");
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
