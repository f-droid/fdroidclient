package org.fdroid.fdroid.net;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import org.fdroid.fdroid.AppDetails;
import org.fdroid.fdroid.data.SanitizedFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A downloader that uses Android's DownloadManager to perform a download.
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class AsyncDownloader extends AsyncDownloadWrapper {
    private final Context context;
    private final DownloadManager dm;
    private SanitizedFile localFile;
    private String remoteAddress;
    private String appName;
    private String appId;
    private Listener listener;

    private long downloadId = -1;

    /**
     * Normally the listener would be provided using a setListener method.
     * However for the purposes of this async downloader, it doesn't make
     * sense to have an async task without any way to notify the outside
     * world about completion. Therefore, we require the listener as a
     * parameter to the constructor.
     *
     * @param listener
     */
    public AsyncDownloader(Context context, Listener listener, String appName, String appId, String remoteAddress, SanitizedFile localFile) {
        super(null, listener);
        this.context = context;
        this.appName = appName;
        this.appId = appId;
        this.remoteAddress = remoteAddress;
        this.listener = listener;
        this.localFile = localFile;

        if (appName == null || appName.trim().length() == 0) {
            this.appName = remoteAddress;
        }

        dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    @Override
    public void download() {
        // set up download request
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(remoteAddress));
        request.setTitle(appName);
        request.setDescription(appId); // we will retrieve this later from the description field

        if (listener != null) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            intentFilter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED);
            context.registerReceiver(downloadReceiver, intentFilter);
        }

        downloadId = dm.enqueue(request);
    }

    @Override
    public int getBytesRead() {
        return 0;
    }

    @Override
    public int getTotalBytes() {
        return 0;
    }

    @Override
    public void attemptCancel(boolean userRequested) {
        if (userRequested && downloadId >= 0) {
            dm.remove(downloadId);
        }

        context.unregisterReceiver(downloadReceiver);
    }

    // Broadcast receiver to receive broadcasts from the download manager
    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (listener == null) {
                // without a listener, install UI won't come up, so ignore this
                return;
            }

            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    context.unregisterReceiver(this);

                    // clear the notification
                    dm.remove(id);

                    try {
                        // write the downloaded file to the expected location
                        ParcelFileDescriptor fd = dm.openDownloadedFile(id);
                        InputStream is = new FileInputStream(fd.getFileDescriptor());
                        OutputStream os = new FileOutputStream(localFile);
                        byte[] buffer = new byte[1024];
                        int count = 0;
                        while ((count = is.read(buffer, 0, buffer.length)) > 0) {
                            os.write(buffer, 0, count);
                        }
                        os.close();

                        listener.onDownloadComplete();
                    } catch (IOException e) {
                        listener.onErrorDownloading(e.getLocalizedMessage());
                        return;
                    }
                }
            }

            if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(intent.getAction())) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    // TODO - display app details screen for this app
                }
            }
        }
    };

    public static String getAppId(Context context, long downloadId) {
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor c = dm.query(query);
        if (c.moveToFirst()) {
            // we use the description column to store the app id
            int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION);
            return c.getString(columnIndex);
        }

        return null;
    }
}
