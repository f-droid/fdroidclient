package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.Schema.AppMetadataTable;
import org.fdroid.fdroid.localrepo.SwapService;
import org.fdroid.fdroid.net.Downloader;
import org.fdroid.fdroid.net.DownloaderService;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SwapAppsView extends ListView implements
        SwapWorkflowActivity.InnerView,
        LoaderManager.LoaderCallbacks<Cursor>,
        SearchView.OnQueryTextListener {

    public SwapAppsView(Context context) {
        super(context);
    }

    public SwapAppsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwapAppsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public SwapAppsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private SwapWorkflowActivity getActivity() {
        return (SwapWorkflowActivity) getContext();
    }

    private static final int LOADER_SWAPABLE_APPS = 759283741;
    private static final String TAG = "SwapAppsView";

    private Repo repo;
    private AppListAdapter adapter;
    private String currentFilterString;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        repo = getActivity().getState().getPeerRepo();

        /*
        if (repo == null) {
            TODO: Uh oh, something stuffed up for this to happen.
            TODO: What is the best course of action from here?
        }
        */

        adapter = new AppListAdapter(getContext(), getContext().getContentResolver().query(
                AppProvider.getRepoUri(repo), AppMetadataTable.Cols.ALL, null, null, null));

        setAdapter(adapter);

        // either reconnect with an existing loader or start a new one
        getActivity().getSupportLoaderManager().initLoader(LOADER_SWAPABLE_APPS, null, this);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                pollForUpdatesReceiver, new IntentFilter(UpdateService.LOCAL_ACTION_STATUS));

        schedulePollForUpdates();
    }

    /**
     * Remove relevant listeners/receivers/etc so that they do not receive and process events
     * when this view is not in use.
     */
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(pollForUpdatesReceiver);
    }

    private void pollForUpdates() {
        if (adapter.getCount() > 1 ||
                (adapter.getCount() == 1 && !new App((Cursor) adapter.getItem(0)).packageName.equals(BuildConfig.APPLICATION_ID))) { // NOCHECKSTYLE LineLength
            Utils.debugLog(TAG, "Not polling for new apps from swap repo, because we already have more than one.");
            return;
        }

        Utils.debugLog(TAG, "Polling swap repo to see if it has any updates.");
        getActivity().getService().refreshSwap();
    }

    private void schedulePollForUpdates() {
        Utils.debugLog(TAG, "Scheduling poll for updated swap repo in 5 seconds.");
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Looper.prepare();
                pollForUpdates();
                Looper.loop();
            }
        }, 5000);
    }

    @Override
    public boolean buildMenu(Menu menu, @NonNull MenuInflater inflater) {

        inflater.inflate(R.menu.swap_search, menu);

        SearchView searchView = new SearchView(getActivity());

        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        MenuItemCompat.setActionView(searchMenuItem, searchView);
        MenuItemCompat.setShowAsAction(searchMenuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public int getStep() {
        return SwapService.STEP_SUCCESS;
    }

    @Override
    public int getPreviousStep() {
        return SwapService.STEP_INTRO;
    }

    @ColorRes
    public int getToolbarColour() {
        return R.color.swap_bright_blue;
    }

    @Override
    public String getToolbarTitle() {
        return getResources().getString(R.string.swap_success);
    }

    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        Uri uri = TextUtils.isEmpty(currentFilterString)
                ? AppProvider.getRepoUri(repo)
                : AppProvider.getSearchUri(repo, currentFilterString);

        return new CursorLoader(getActivity(), uri, AppMetadataTable.Cols.ALL,
                null, null, AppMetadataTable.Cols.NAME);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        adapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
        if (currentFilterString == null && newFilter == null) {
            return true;
        }
        if (currentFilterString != null && currentFilterString.equals(newFilter)) {
            return true;
        }
        currentFilterString = newFilter;
        getActivity().getSupportLoaderManager().restartLoader(LOADER_SWAPABLE_APPS, null, this);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // this is not needed since we respond to every change in text
        return true;
    }

    private class AppListAdapter extends CursorAdapter {

        private class ViewHolder {

            private final LocalBroadcastManager localBroadcastManager;

            @Nullable
            private App app;

            @Nullable
            private Apk apk;

            ProgressBar progressView;
            TextView nameView;
            ImageView iconView;
            Button btnInstall;
            TextView statusInstalled;
            TextView statusIncompatible;

            private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()) {
                        case Downloader.ACTION_STARTED:
                            resetView();
                            break;
                        case Downloader.ACTION_PROGRESS:
                            if (progressView.getVisibility() != View.VISIBLE) {
                                showProgress();
                            }
                            long read = intent.getLongExtra(Downloader.EXTRA_BYTES_READ, 0);
                            long total = intent.getLongExtra(Downloader.EXTRA_TOTAL_BYTES, 0);
                            if (total > 0) {
                                progressView.setIndeterminate(false);
                                progressView.setMax(100);
                                progressView.setProgress(Utils.getPercent(read, total));
                            } else {
                                progressView.setIndeterminate(true);
                            }
                            break;
                        case Downloader.ACTION_COMPLETE:
                            resetView();
                            break;
                        case Downloader.ACTION_INTERRUPTED:
                            if (intent.hasExtra(Downloader.EXTRA_ERROR_MESSAGE)) {
                                String msg = intent.getStringExtra(Downloader.EXTRA_ERROR_MESSAGE)
                                        + " " + intent.getDataString();
                                Toast.makeText(context, R.string.download_error, Toast.LENGTH_SHORT).show();
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                            } else { // user canceled
                                Toast.makeText(context, R.string.details_notinstalled, Toast.LENGTH_LONG).show();
                            }
                            resetView();
                            break;
                        default:
                            throw new RuntimeException("intent action not handled!");
                    }

                }
            };

            private final ContentObserver appObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    Activity activity = getActivity();
                    if (activity != null) {
                        app = AppProvider.Helper.findSpecificApp(
                                getActivity().getContentResolver(),
                                app.packageName,
                                app.repoId,
                                AppMetadataTable.Cols.ALL);
                        resetView();
                    }
                }
            };

            ViewHolder() {
                localBroadcastManager = LocalBroadcastManager.getInstance(getContext());
            }

            public void setApp(@NonNull App app) {
                if (this.app == null || !this.app.packageName.equals(app.packageName)) {
                    this.app = app;

                    List<Apk> availableApks = ApkProvider.Helper.findAppVersionsByRepo(getActivity(), app, repo);
                    if (availableApks.size() > 0) {
                        // Swap repos only add one version of an app, so we will just ask for the first apk.
                        this.apk = availableApks.get(0);
                    }

                    if (apk != null) {
                        String urlString = apk.getUrl();

                        // TODO unregister receivers? or will they just die with this instance
                        IntentFilter downloadFilter = DownloaderService.getIntentFilter(urlString);
                        localBroadcastManager.registerReceiver(downloadReceiver, downloadFilter);
                    }

                    // NOTE: Instead of continually unregistering and re-registering the observer
                    // (with a different URI), this could equally be done by only having one
                    // registration in the constructor, and using the ContentObserver.onChange(boolean, URI)
                    // method and inspecting the URI to see if it matches. However, this was only
                    // implemented on API-16, so leaving like this for now.
                    getActivity().getContentResolver().unregisterContentObserver(appObserver);
                    getActivity().getContentResolver().registerContentObserver(
                            AppProvider.getSpecificAppUri(this.app.packageName, this.app.repoId), true, appObserver);
                }
                resetView();
            }

            private void resetView() {

                if (app == null) {
                    return;
                }

                progressView.setVisibility(View.GONE);
                progressView.setIndeterminate(true);

                if (app.name != null) {
                    nameView.setText(app.name);
                }

                ImageLoader.getInstance().displayImage(app.iconUrl, iconView, Utils.getRepoAppDisplayImageOptions());

                if (app.hasUpdates()) {
                    btnInstall.setText(R.string.menu_upgrade);
                    btnInstall.setVisibility(View.VISIBLE);
                    statusIncompatible.setVisibility(View.GONE);
                    statusInstalled.setVisibility(View.GONE);
                } else if (app.isInstalled(getContext())) {
                    btnInstall.setVisibility(View.GONE);
                    statusIncompatible.setVisibility(View.GONE);
                    statusInstalled.setVisibility(View.VISIBLE);
                } else if (!app.compatible) {
                    btnInstall.setVisibility(View.GONE);
                    statusIncompatible.setVisibility(View.VISIBLE);
                    statusInstalled.setVisibility(View.GONE);
                } else {
                    btnInstall.setText(R.string.menu_install);
                    btnInstall.setVisibility(View.VISIBLE);
                    statusIncompatible.setVisibility(View.GONE);
                    statusInstalled.setVisibility(View.GONE);
                }

                OnClickListener installListener = new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (apk != null && (app.hasUpdates() || app.compatible)) {
                            getActivity().install(app, apk);
                            showProgress();
                        }
                    }
                };

                btnInstall.setOnClickListener(installListener);
            }

            private void showProgress() {
                progressView.setVisibility(View.VISIBLE);
                btnInstall.setVisibility(View.GONE);
                statusInstalled.setVisibility(View.GONE);
                statusIncompatible.setVisibility(View.GONE);
            }
        }

        @Nullable
        private LayoutInflater inflater;

        AppListAdapter(@NonNull Context context, @Nullable Cursor c) {
            super(context, c, FLAG_REGISTER_CONTENT_OBSERVER);
        }

        @NonNull
        private LayoutInflater getInflater(Context context) {
            if (inflater == null) {
                inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }
            return inflater;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = getInflater(context).inflate(R.layout.swap_app_list_item, parent, false);

            ViewHolder holder = new ViewHolder();

            holder.progressView = (ProgressBar) view.findViewById(R.id.progress);
            holder.nameView = (TextView) view.findViewById(R.id.name);
            holder.iconView = (ImageView) view.findViewById(android.R.id.icon);
            holder.btnInstall = (Button) view.findViewById(R.id.btn_install);
            holder.statusInstalled = (TextView) view.findViewById(R.id.status_installed);
            holder.statusIncompatible = (TextView) view.findViewById(R.id.status_incompatible);

            view.setTag(holder);
            bindView(view, context, cursor);
            return view;
        }

        @Override
        public void bindView(final View view, final Context context, final Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            final App app = new App(cursor);
            holder.setApp(app);
        }
    }

    private final BroadcastReceiver pollForUpdatesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int statusCode = intent.getIntExtra(UpdateService.EXTRA_STATUS_CODE, -1);
            switch (statusCode) {
                case UpdateService.STATUS_COMPLETE_WITH_CHANGES:
                    Utils.debugLog(TAG, "Swap repo has updates, notifying the list adapter.");
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
                    break;

                case UpdateService.STATUS_ERROR_GLOBAL:
                    // TODO: Well, if we can't get the index, we probably can't swapp apps.
                    // Tell the user something helpful?
                    break;

                case UpdateService.STATUS_COMPLETE_AND_SAME:
                    schedulePollForUpdates();
                    break;
            }
        }
    };

}
