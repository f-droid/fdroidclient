package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.Schema.AppMetadataTable;
import org.fdroid.fdroid.installer.InstallManagerService;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.fdroid.localrepo.SwapView;
import org.fdroid.fdroid.net.Downloader;
import org.fdroid.fdroid.net.DownloaderService;

import java.util.List;

/**
 * This is a view that shows a listing of all apps in the swap repo that this
 * just connected to.  The app listing and search should be replaced by
 * {@link org.fdroid.fdroid.views.apps.AppListActivity}'s plumbing.
 */
// TODO merge this with AppListActivity, perhaps there could be AppListView?
public class SwapSuccessView extends SwapView implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "SwapAppsView";

    public SwapSuccessView(Context context) {
        super(context);
    }

    public SwapSuccessView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwapSuccessView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public SwapSuccessView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private Repo repo;
    private AppListAdapter adapter;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        repo = getActivity().getSwapService().getPeerRepo();

        adapter = new AppListAdapter(getContext(), getContext().getContentResolver().query(
                AppProvider.getRepoUri(repo), AppMetadataTable.Cols.ALL, null, null, null));
        ListView listView = findViewById(R.id.list);
        listView.setAdapter(adapter);

        // either reconnect with an existing loader or start a new one
        getActivity().getSupportLoaderManager().initLoader(R.layout.swap_success, null, this);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                pollForUpdatesReceiver, new IntentFilter(UpdateService.LOCAL_ACTION_STATUS));
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

    @NonNull
    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        Uri uri = TextUtils.isEmpty(currentFilterString)
                ? AppProvider.getRepoUri(repo)
                : AppProvider.getSearchUri(repo, currentFilterString);

        return new CursorLoader(getActivity(), uri, AppMetadataTable.Cols.ALL,
                null, null, AppMetadataTable.Cols.NAME);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        adapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        adapter.swapCursor(null);
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

            private class DownloadReceiver extends BroadcastReceiver {
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
                            localBroadcastManager.unregisterReceiver(this);
                            resetView();
                            statusInstalled.setText(R.string.installing);
                            statusInstalled.setVisibility(View.VISIBLE);
                            btnInstall.setVisibility(View.GONE);
                            break;
                        case Downloader.ACTION_INTERRUPTED:
                            localBroadcastManager.unregisterReceiver(this);
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
            }

            private final ContentObserver appObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    Activity activity = getActivity();
                    if (activity != null && app != null) {
                        app = AppProvider.Helper.findSpecificApp(
                                activity.getContentResolver(),
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
                        localBroadcastManager.registerReceiver(new DownloadReceiver(),
                                DownloaderService.getIntentFilter(apk.getCanonicalUrl()));
                        localBroadcastManager.registerReceiver(new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                switch (intent.getAction()) {
                                    case Installer.ACTION_INSTALL_STARTED:
                                        statusInstalled.setText(R.string.installing);
                                        statusInstalled.setVisibility(View.VISIBLE);
                                        btnInstall.setVisibility(View.GONE);
                                        progressView.setIndeterminate(true);
                                        progressView.setVisibility(View.VISIBLE);
                                        break;
                                    case Installer.ACTION_INSTALL_USER_INTERACTION:
                                        PendingIntent installPendingIntent =
                                                intent.getParcelableExtra(Installer.EXTRA_USER_INTERACTION_PI);
                                        try {
                                            installPendingIntent.send();
                                        } catch (PendingIntent.CanceledException e) {
                                            Log.e(TAG, "PI canceled", e);
                                        }
                                        break;
                                    case Installer.ACTION_INSTALL_COMPLETE:
                                        localBroadcastManager.unregisterReceiver(this);
                                        statusInstalled.setText(R.string.app_installed);
                                        statusInstalled.setVisibility(View.VISIBLE);
                                        btnInstall.setVisibility(View.GONE);
                                        progressView.setVisibility(View.GONE);
                                        break;
                                    case Installer.ACTION_INSTALL_INTERRUPTED:
                                        localBroadcastManager.unregisterReceiver(this);
                                        statusInstalled.setVisibility(View.GONE);
                                        btnInstall.setVisibility(View.VISIBLE);
                                        progressView.setVisibility(View.GONE);
                                        String errorMessage = intent.getStringExtra(Installer.EXTRA_ERROR_MESSAGE);
                                        if (errorMessage != null) {
                                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                                        }
                                        break;
                                }
                            }
                        }, Installer.getInstallIntentFilter(apk.getCanonicalUrl()));
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

            private final OnClickListener cancelListener = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (apk != null) {
                        InstallManagerService.cancel(getContext(), apk.getCanonicalUrl());
                    }
                }
            };

            private final OnClickListener installListener = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (apk != null && (app.hasUpdates() || app.compatible)) {
                        showProgress();
                        InstallManagerService.queue(getContext(), app, apk);
                    }
                }
            };

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
                    btnInstall.setOnClickListener(installListener);
                    statusIncompatible.setVisibility(View.GONE);
                    statusInstalled.setVisibility(View.GONE);
                } else if (app.isInstalled(getContext())) {
                    btnInstall.setVisibility(View.GONE);
                    statusIncompatible.setVisibility(View.GONE);
                    statusInstalled.setVisibility(View.VISIBLE);
                    statusInstalled.setText(R.string.app_installed);
                } else if (!app.compatible) {
                    btnInstall.setVisibility(View.GONE);
                    statusIncompatible.setVisibility(View.VISIBLE);
                    statusInstalled.setVisibility(View.GONE);
                } else if (progressView.getVisibility() == View.VISIBLE) {
                    btnInstall.setText(R.string.cancel);
                    btnInstall.setVisibility(View.VISIBLE);
                    btnInstall.setOnClickListener(cancelListener);
                    statusIncompatible.setVisibility(View.GONE);
                    statusInstalled.setVisibility(View.GONE);
                } else {
                    btnInstall.setText(R.string.menu_install);
                    btnInstall.setVisibility(View.VISIBLE);
                    btnInstall.setOnClickListener(installListener);
                    statusIncompatible.setVisibility(View.GONE);
                    statusInstalled.setVisibility(View.GONE);
                }
            }

            private void showProgress() {
                btnInstall.setText(R.string.cancel);
                btnInstall.setVisibility(View.VISIBLE);
                btnInstall.setOnClickListener(cancelListener);
                progressView.setVisibility(View.VISIBLE);
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
            }
        }
    };
}
