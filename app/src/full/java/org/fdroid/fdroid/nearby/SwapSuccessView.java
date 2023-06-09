package org.fdroid.fdroid.nearby;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.fdroid.database.Repository;
import org.fdroid.fdroid.CompatibilityChecker;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.installer.InstallManagerService;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.fdroid.net.DownloaderService;
import org.fdroid.index.v1.AppV1;
import org.fdroid.index.v1.IndexV1;
import org.fdroid.index.v1.PackageV1;
import org.fdroid.index.v1.PermissionV1;
import org.fdroid.index.v2.FileV1;
import org.fdroid.index.v2.FileV2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a view that shows a listing of all apps in the swap repo that this
 * just connected to.  The app listing and search should be replaced by
 * {@link org.fdroid.fdroid.views.apps.AppListActivity}'s plumbing.
 */
// TODO merge this with AppListActivity, perhaps there could be AppListView?
public class SwapSuccessView extends SwapView {
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

    public SwapSuccessView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private Repository repo;
    private AppListAdapter adapter;

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        repo = getActivity().getSwapService().getPeerRepo();

        adapter = new AppListAdapter();
        RecyclerView listView = findViewById(R.id.list);
        listView.setAdapter(adapter);

        getActivity().getSwapService().getIndex().observe(getActivity(), this::onIndexReceived);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                pollForUpdatesReceiver, new IntentFilter(UpdateService.LOCAL_ACTION_STATUS));
    }

    private void onIndexReceived(IndexV1 indexV1) {
        List<App> apps = new ArrayList<>(indexV1.getApps().size());
        HashMap<String, Apk> apks = new HashMap<>(indexV1.getApps().size());
        CompatibilityChecker checker = new CompatibilityChecker(getContext());
        for (AppV1 a : indexV1.getApps()) {
            App app = new App();
            app.name = a.getName();
            app.packageName = a.getPackageName();
            app.iconFile = FileV2.fromPath("icons/" + a.getIcon());
            try {
                PackageInfo packageInfo = getContext().getPackageManager().getPackageInfo(app.packageName, 0);
                app.installedVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            Apk apk = new Apk();
            List<PackageV1> packages = indexV1.getPackages().get(app.packageName);
            if (packages != null && packages.get(0) != null) {
                PackageV1 packageV1 = packages.get(0);
                if (packageV1.getVersionCode() != null) {
                    app.autoInstallVersionCode = packageV1.getVersionCode().intValue();
                }
                if (packageV1.getVersionCode() != null) {
                    apk.versionCode = packageV1.getVersionCode();
                }
                apk.versionName = packageV1.getVersionName();
                apk.apkFile = new FileV1("/" + packageV1.getApkName(), packageV1.getHash(), packageV1.getSize(), null);
                ArrayList<String> permissions =
                        new ArrayList<>(packageV1.getUsesPermission().size());
                for (PermissionV1 perm : packageV1.getUsesPermission()) {
                    permissions.add(perm.getName());
                }
                apk.requestedPermissions = permissions.toArray(new String[0]);
                if (apk.requestedPermissions.length == 0) apk.requestedPermissions = null;
            }

            apk.repoId = Long.MAX_VALUE;
            apk.packageName = app.packageName;
            apk.repoAddress = repo.getAddress();
            apk.canonicalRepoAddress = repo.getAddress();
            apk.setCompatibility(checker);
            app.compatible = apk.compatible;

            apps.add(app);
            apks.put(app.packageName, apk);
        }
        adapter.setApps(apps, apks);
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

    private class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {

        private final List<App> apps = new ArrayList<>();
        private final Map<String, Apk> apks = new HashMap<>();

        private class ViewHolder extends RecyclerView.ViewHolder {

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
                        case DownloaderService.ACTION_STARTED:
                            resetView();
                            break;
                        case DownloaderService.ACTION_PROGRESS:
                            if (progressView.getVisibility() != View.VISIBLE) {
                                showProgress();
                            }
                            long read = intent.getLongExtra(DownloaderService.EXTRA_BYTES_READ, 0);
                            long total = intent.getLongExtra(DownloaderService.EXTRA_TOTAL_BYTES, 0);
                            if (total > 0) {
                                progressView.setIndeterminate(false);
                                progressView.setMax(100);
                                progressView.setProgress(Utils.getPercent(read, total));
                            } else {
                                progressView.setIndeterminate(true);
                            }
                            break;
                        case DownloaderService.ACTION_COMPLETE:
                            localBroadcastManager.unregisterReceiver(this);
                            resetView();
                            statusInstalled.setText(R.string.installing);
                            statusInstalled.setVisibility(View.VISIBLE);
                            btnInstall.setVisibility(View.GONE);
                            break;
                        case DownloaderService.ACTION_CONNECTION_FAILED:
                        case DownloaderService.ACTION_INTERRUPTED:
                            localBroadcastManager.unregisterReceiver(this);
                            if (intent.hasExtra(DownloaderService.EXTRA_ERROR_MESSAGE)) {
                                String msg = intent.getStringExtra(DownloaderService.EXTRA_ERROR_MESSAGE)
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

            ViewHolder(View view) {
                super(view);
                localBroadcastManager = LocalBroadcastManager.getInstance(getContext());
                progressView = view.findViewById(R.id.progress);
                nameView = view.findViewById(R.id.name);
                iconView = view.findViewById(android.R.id.icon);
                btnInstall = view.findViewById(R.id.btn_install);
                statusInstalled = view.findViewById(R.id.status_installed);
                statusIncompatible = view.findViewById(R.id.status_incompatible);
            }

            public void setApp(@NonNull App app) {
                if (this.app == null || !this.app.packageName.equals(app.packageName)) {
                    this.app = app;
                    this.apk = apks.get(this.app.packageName);

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

                Glide.with(iconView.getContext())
                        .load(Utils.getDownloadRequest(repo, app.iconFile))
                        .apply(Utils.getAlwaysShowIconRequestOptions())
                        .into(iconView);

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

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.swap_app_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.setApp(apps.get(position));
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }

        void setApps(List<App> apps, Map<String, Apk> apks) {
            this.apps.clear();
            this.apps.addAll(apps);
            this.apks.clear();
            this.apks.putAll(apks);
            notifyDataSetChanged();
        }
    }

    private final BroadcastReceiver pollForUpdatesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int statusCode = intent.getIntExtra(UpdateService.EXTRA_STATUS_CODE, -1);
            if (statusCode == UpdateService.STATUS_COMPLETE_WITH_CHANGES) {
                Utils.debugLog(TAG, "Swap repo has updates, notifying the list adapter.");
                getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
            }
        }
    };
}
