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
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
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
        if ((downloadId = isDownloadComplete(context, appId)) > 0) {
            // clear the notification
            dm.remove(downloadId);

            try {
                // write the downloaded file to the expected location
                ParcelFileDescriptor fd = dm.openDownloadedFile(downloadId);
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
            }

            return;
        }

        downloadId = isDownloading(context, appId);
        if (downloadId >= 0) return;

        // set up download request
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(remoteAddress));
        request.setTitle(appName);
        request.setDescription(appId); // we will retrieve this later from the description field

        this.downloadId = dm.enqueue(request);
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
    }

    /**
     * Extract the appId from a given download id.
     * @param context
     * @param downloadId
     * @return - appId or null if not found
     */
    public static String getAppId(Context context, long downloadId) {
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor c = dm.query(query);

        try {
            if (c.moveToFirst()) {
                // we use the description column to store the app id
                int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION);
                return c.getString(columnIndex);
            }
        } finally {
            c.close();
        }

        return null;
    }

    /**
     * Get the downloadId from an Intent sent by the DownloadManagerReceiver
     * @param intent
     * @return
     */
    public static long getDownloadId(Intent intent) {
        if (intent != null) {
            if (intent.hasExtra(DownloadManager.EXTRA_DOWNLOAD_ID)) {
                // we have been passed a DownloadManager download id, so get the app id for it
                return intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            }

            if (intent.hasExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS)) {
                // we have been passed a DownloadManager download id, so get the app id for it
                long[] downloadIds = intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);
                if (downloadIds != null && downloadIds.length > 0) {
                    return downloadIds[0];
                }
            }
        }

        return -1;
    }

    /**
     * Check if a download is running for the app
     * @param context
     * @param appId
     * @return -1 if not downloading, else the downloadId
     */
    public static long isDownloading(Context context, String appId) {
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        Cursor c = dm.query(query);
        int columnAppId = c.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION);
        int columnId = c.getColumnIndex(DownloadManager.COLUMN_ID);

        try {
            while (c.moveToNext()) {
                if (appId.equals(c.getString(columnAppId))) {
                    return c.getLong(columnId);
                }
            }
        } finally {
            c.close();
        }

        return -1;
    }

    /**
     * Check if a download for an app is complete.
     * @param context
     * @param appId
     * @return -1 if download is not complete, otherwise the download id
     */
    public static long isDownloadComplete(Context context, String appId) {
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL);
        Cursor c = dm.query(query);
        int columnAppId = c.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION);
        int columnId = c.getColumnIndex(DownloadManager.COLUMN_ID);

        try {
            while (c.moveToNext()) {
                if (appId.equals(c.getString(columnAppId))) {
                    return c.getLong(columnId);
                }
            }
        } finally {
            c.close();
        }

        return -1;
    }
}
