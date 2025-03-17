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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.app.TaskStackBuilder;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.fdroid.database.Repository;
import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UiUtils;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.UtilsKt;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.work.RepoUpdateWorker;
import org.fdroid.index.RepoManager;

import java.util.ArrayList;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class ManageReposActivity extends AppCompatActivity implements RepoAdapter.RepoItemListener {

    private RepoManager repoManager;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final RepoAdapter repoAdapter = new RepoAdapter(this);
    private boolean isItemReorderingEnabled = false;
    private final ItemTouchHelper.Callback itemTouchCallback =
            new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

                private int lastFromPos = -1;
                private int lastToPos = -1;

                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                      @NonNull RecyclerView.ViewHolder target) {
                    final int fromPos = viewHolder.getBindingAdapterPosition();
                    final int toPos = target.getBindingAdapterPosition();
                    repoAdapter.notifyItemMoved(fromPos, toPos);
                    if (lastFromPos == -1) lastFromPos = fromPos;
                    lastToPos = toPos;
                    return true;
                }

                @Override
                public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                    super.onSelectedChanged(viewHolder, actionState);
                    if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                        if (lastFromPos != lastToPos) {
                            Repository repoToMove = repoAdapter.getItem(lastFromPos);
                            Repository repoDropped = repoAdapter.getItem(lastToPos);
                            if (repoToMove != null && repoDropped != null) {
                                // don't allow more re-orderings until this one was completed
                                isItemReorderingEnabled = false;
                                repoManager.reorderRepositories(repoToMove, repoDropped);
                            } else {
                                Log.w("ManageReposActivity",
                                        "Could not find one of the repos: " + lastFromPos + " to " + lastToPos);
                            }
                        }
                        lastFromPos = -1;
                        lastToPos = -1;
                    }
                }

                @Override
                public boolean isLongPressDragEnabled() {
                    return isItemReorderingEnabled;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    // noop
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FDroidApp fdroidApp = (FDroidApp) getApplication();
        fdroidApp.setSecureWindow(this);

        fdroidApp.applyPureBlackBackgroundInDarkTheme(this);
        EdgeToEdge.enable(this);
        repoManager = FDroidApp.getRepoManager(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.repo_list_activity);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        long lastUpdate = Preferences.get().getLastUpdateCheck();
        CharSequence lastUpdateStr = lastUpdate < 0 ?
                getString(R.string.repositories_last_update_never) : UtilsKt.asRelativeTimeString(lastUpdate);
        getSupportActionBar().setSubtitle(getString(R.string.repositories_last_update, lastUpdateStr));
        View fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent i = new Intent(this, AddRepoActivity.class);
            startActivity(i);
        });
        ViewCompat.setOnApplyWindowInsetsListener(fab, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.leftMargin += insets.left;
            mlp.bottomMargin += insets.bottom;
            mlp.rightMargin += insets.right;
            v.setLayoutParams(mlp);
            return WindowInsetsCompat.CONSUMED;
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
        final ItemTouchHelper touchHelper = new ItemTouchHelper(itemTouchCallback);
        touchHelper.attachToRecyclerView(repoList);
        repoList.setAdapter(repoAdapter);
        FDroidApp.getRepoManager(this).getLiveRepositories().observe(this, items -> {
            repoAdapter.updateItems(new ArrayList<>(items)); // copy list, so we don't modify original in adapter
            isItemReorderingEnabled = true;
        });
        UiUtils.setupEdgeToEdge(repoList, false, true);
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
        RepoDetailsActivity.Companion.launch(this, repo.getRepoId());
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
    public void onToggleEnabled(Repository repo) {
        if (repo.getEnabled()) {
            new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.repo_disable_warning)
                    .setPositiveButton(R.string.repo_disable_warning_button, (dialog, id) -> {
                        disableRepo(repo);
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel())
                    .setOnCancelListener(dialog -> repoAdapter.updateRepoItem(repo)) // reset toggle
                    .show();
        } else {
            Utils.runOffUiThread(() -> {
                repoManager.setRepositoryEnabled(repo.getRepoId(), true);
                return true;
            }, result -> RepoUpdateWorker.updateNow(getApplication(), repo.getRepoId()));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.repo_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_info) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.repo_list_info_title))
                    .setMessage(getString(R.string.repo_list_info_text))
                    .setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss())
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void disableRepo(Repository repo) {
        Utils.runOffUiThread(() -> repoManager.setRepositoryEnabled(repo.getRepoId(), false));
        AppUpdateStatusManager.getInstance(this).removeAllByRepo(repo.getRepoId());
        String notification = getString(R.string.repo_disabled_notification, repo.getName(App.getLocales()));
        Snackbar.make(findViewById(R.id.list), notification, Snackbar.LENGTH_LONG).setTextMaxLines(3).show();
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
