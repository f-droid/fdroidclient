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

package org.fdroid.fdroid.views.repos;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.app.TaskStackBuilder;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import org.fdroid.database.Repository;
import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;
import org.fdroid.index.RepoManager;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class ManageReposActivity extends AppCompatActivity implements RepoAdapter.RepoItemListener {

    public static final String EXTRA_FINISH_AFTER_ADDING_REPO = "finishAfterAddingRepo";
    private RepoManager repoManager;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FDroidApp fdroidApp = (FDroidApp) getApplication();
        fdroidApp.setSecureWindow(this);

        fdroidApp.applyPureBlackBackgroundInDarkTheme(this);
        repoManager = FDroidApp.getRepoManager(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.repo_list_activity);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        findViewById(R.id.fab).setOnClickListener(view -> {
            Intent i = new Intent(this, AddRepoActivity.class);
            startActivity(i);
        });
        toolbar.setNavigationOnClickListener(v -> {
            Intent upIntent = NavUtils.getParentActivityIntent(ManageReposActivity.this);
            if (NavUtils.shouldUpRecreateTask(ManageReposActivity.this, upIntent) || isTaskRoot()) {
                TaskStackBuilder.create(ManageReposActivity.this).addNextIntentWithParentStack(upIntent)
                        .startActivities();
            } else {
                NavUtils.navigateUpTo(ManageReposActivity.this, upIntent);
            }
        });

        final RecyclerView repoList = findViewById(R.id.list);
        RepoAdapter repoAdapter = new RepoAdapter(this);
        repoList.setAdapter(repoAdapter);
        FDroidApp.getRepoManager(this).getLiveRepositories().observe(this, repoAdapter::updateItems);
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onClicked(Repository repo) {
        editRepo(repo);
    }

    /**
     * NOTE: If somebody toggles a repo off then on again, it will have
     * removed all apps from the index when it was toggled off, so when it
     * is toggled on again, then it will require a updateViews. Previously, I
     * toyed with the idea of remembering whether they had toggled on or
     * off, and then only actually performing the function when the activity
     * stopped, but I think that will be problematic. What about when they
     * press the home button, or edit a repos details? It will start to
     * become somewhat-random as to when the actual enabling, disabling is
     * performed. So now, it just does the disable as soon as the user
     * clicks "Off" and then removes the apps. To compensate for the removal
     * of apps from index, it notifies the user via a toast that the apps
     * have been removed. Also, as before, it will still prompt the user to
     * update the repos if you toggled on on.
     */
    @Override
    public void onSetEnabled(Repository repo, boolean isEnabled) {
        if (repo.getEnabled() != isEnabled) {
            Utils.runOffUiThread(() -> repoManager.setRepositoryEnabled(repo.getRepoId(), isEnabled));

            if (isEnabled) {
                UpdateService.updateRepoNow(this, repo.getAddress());
            } else {
                AppUpdateStatusManager.getInstance(this).removeAllByRepo(repo.getRepoId());
                // RepoProvider.Helper.purgeApps(this, repo);
                String notification = getString(R.string.repo_disabled_notification, repo.getName(App.getLocales()));
                Toast.makeText(this, notification, Toast.LENGTH_LONG).show();
            }
        }
    }

    private static final int SHOW_REPO_DETAILS = 1;

    private void editRepo(Repository repo) {
        Intent intent = new Intent(this, RepoDetailsActivity.class);
        intent.putExtra(RepoDetailsActivity.ARG_REPO_ID, repo.getRepoId());
        startActivityForResult(intent, SHOW_REPO_DETAILS);
    }

    public static String getDisallowInstallUnknownSourcesErrorMessage(Context context) {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        if (Build.VERSION.SDK_INT >= 29
                && userManager.hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY)) {
            return context.getString(R.string.has_disallow_install_unknown_sources_globally);
        }
        return context.getString(R.string.has_disallow_install_unknown_sources);
    }
}
