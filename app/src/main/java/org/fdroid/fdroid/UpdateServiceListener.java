package org.fdroid.fdroid;

import android.content.Context;

import androidx.annotation.NonNull;

import org.fdroid.database.Repository;
import org.fdroid.fdroid.data.App;
import org.fdroid.index.IndexUpdateListener;

class UpdateServiceListener implements IndexUpdateListener {

    private final Context context;

    UpdateServiceListener(Context context) {
        this.context = context;
    }

    @Override
    public void onDownloadProgress(@NonNull Repository repo, long bytesRead, long totalBytes) {
        UpdateService.reportDownloadProgress(context, repo.getAddress(), bytesRead, totalBytes);
    }

    @Override
    public void onUpdateProgress(@NonNull Repository repo, int appsProcessed, int totalApps) {
        UpdateService.reportProcessingAppsProgress(context, repo.getName(App.getLocales()), appsProcessed, totalApps);
    }
}
