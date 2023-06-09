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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;

import com.google.android.material.appbar.MaterialToolbar;

import org.apache.commons.io.IOUtils;
import org.fdroid.database.Repository;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.installer.InstallHistoryService;
import org.fdroid.fdroid.work.FDroidMetricsWorker;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class InstallHistoryActivity extends AppCompatActivity {
    public static final String TAG = "InstallHistoryActivity";

    static final String EXTRA_SHOW_FDROID_METRICS = "showFDroidMetrics";

    private boolean showingInstallHistory;
    private MaterialToolbar toolbar;
    private MenuItem showMenuItem;
    private TextView textView;
    private String appName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FDroidApp fdroidApp = (FDroidApp) getApplication();
        fdroidApp.setSecureWindow(this);

        fdroidApp.applyPureBlackBackgroundInDarkTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_history);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        textView = findViewById(R.id.text);
        appName = getString(R.string.app_name);

        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra(EXTRA_SHOW_FDROID_METRICS, false)) {
            showFDroidMetricsReport();
        } else {
            showInstallHistory();
        }
    }

    private void showInstallHistory() {
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
        toolbar.setTitle(getString(R.string.install_history));
        textView.setText(text);
        showingInstallHistory = true;
        if (showMenuItem != null) {
            showMenuItem.setVisible(Preferences.get().isSendingToFDroidMetrics());
            showMenuItem.setTitle(R.string.menu_show_fdroid_metrics_report);
        }
    }

    private void showFDroidMetricsReport() {
        toolbar.setTitle(getString(R.string.fdroid_metrics_report, appName));
        textView.setText(FDroidMetricsWorker.generateReport(this));
        showingInstallHistory = false;
        if (showMenuItem != null) {
            showMenuItem.setVisible(Preferences.get().isSendingToFDroidMetrics());
            showMenuItem.setTitle(R.string.menu_show_install_history);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.install_history, menu);
        showMenuItem = menu.findItem(R.id.menu_show);
        showMenuItem.setVisible(Preferences.get().isSendingToFDroidMetrics());
        if (showingInstallHistory) {
            showMenuItem.setTitle(R.string.menu_show_fdroid_metrics_report);
        } else {
            showMenuItem.setTitle(R.string.menu_show_install_history);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_share:
                ShareCompat.IntentBuilder intentBuilder = ShareCompat.IntentBuilder.from(this);
                if (showingInstallHistory) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Repos:\n");
                    for (Repository repo : FDroidApp.getRepoManager(this).getRepositories()) {
                        if (repo.getEnabled()) {
                            stringBuilder.append("* ");
                            stringBuilder.append(repo.getAddress());
                            stringBuilder.append('\n');
                        }
                    }
                    intentBuilder
                            .setText(stringBuilder.toString())
                            .setStream(InstallHistoryService.LOG_URI)
                            .setType("text/plain")
                            .setSubject(getString(R.string.send_history_csv, appName))
                            .setChooserTitle(R.string.send_install_history);
                } else {
                    intentBuilder
                            .setText(textView.getText())
                            .setType("application/json")
                            .setSubject(getString(R.string.send_fdroid_metrics_json, appName))
                            .setChooserTitle(R.string.send_fdroid_metrics_report);
                }
                Intent intent = intentBuilder.getIntent();
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
                break;
            case R.id.menu_delete:
                if (showingInstallHistory) {
                    getContentResolver().delete(InstallHistoryService.LOG_URI, null, null);
                }
                textView.setText("");
                break;
            case R.id.menu_show:
                if (showingInstallHistory) {
                    showFDroidMetricsReport();
                } else {
                    showInstallHistory();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
