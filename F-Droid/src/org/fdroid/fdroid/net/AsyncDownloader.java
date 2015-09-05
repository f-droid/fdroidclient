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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.fdroid.fdroid.AppDetails;
import org.fdroid.fdroid.ProgressListener;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.SanitizedFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
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
        // Check if the download is complete
        if ((downloadId = isDownloadComplete(context, appId)) > 0) {
            // clear the notification
            dm.remove(downloadId);

            try {
                // write the downloaded file to the expected location
                ParcelFileDescriptor fd = dm.openDownloadedFile(downloadId);
                copyFile(fd.getFileDescriptor(), localFile);
                listener.onDownloadComplete();
            } catch (IOException e) {
                listener.onErrorDownloading(e.getLocalizedMessage());
            }
            return;
        }

        // Check if the download is still in progress
        if (downloadId < 0) {
            downloadId = isDownloading(context, appId);
        }

        // Start a new download
        if (downloadId < 0) {
            // set up download request
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(remoteAddress));
            request.setTitle(appName);
            request.setDescription(appId); // we will retrieve this later from the description field
            this.downloadId = dm.enqueue(request);
        }

        context.registerReceiver(receiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    /**
     * Copy input file to output file
     * @param inputFile
     * @param outputFile
     * @throws IOException
     */
    private void copyFile(FileDescriptor inputFile, SanitizedFile outputFile) throws IOException {
        InputStream is = new FileInputStream(inputFile);
        OutputStream os = new FileOutputStream(outputFile);
        byte[] buffer = new byte[1024];
        int count = 0;

        try {
            while ((count = is.read(buffer, 0, buffer.length)) > 0) {
                os.write(buffer, 0, count);
            }
        } finally {
            os.close();
            is.close();
        }
    }

    @Override
    public int getBytesRead() {
        if (downloadId < 0) return 0;

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor c = dm.query(query);

        try {
            if (c.moveToFirst()) {
                // we use the description column to store the app id
                int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                return c.getInt(columnIndex);
            }
        } finally {
            c.close();
        }

        return 0;
    }

    @Override
    public int getTotalBytes() {
        if (downloadId < 0) return 0;

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor c = dm.query(query);

        try {
            if (c.moveToFirst()) {
                // we use the description column to store the app id
                int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                return c.getInt(columnIndex);
            }
        } finally {
            c.close();
        }

        return 0;
    }

    @Override
    public void attemptCancel(boolean userRequested) {
        try {
            context.unregisterReceiver(receiver);
        } catch (Exception e) {
            // ignore if receiver already unregistered
        }

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
     * Extract the download title from a given download id.
     * @param context
     * @param downloadId
     * @return - title or null if not found
     */
    public static String getDownloadTitle(Context context, long downloadId) {
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor c = dm.query(query);

        try {
            if (c.moveToFirst()) {
                // we use the description column to store the app id
                int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_TITLE);
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
                // we have been passed multiple download id's - just return the first one
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

    /**
     * Broadcast receiver to listen for ACTION_DOWNLOAD_COMPLETE broadcasts
     */
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                long dId = getDownloadId(intent);
                String appId = getAppId(context, dId);
                if (listener != null && dId == downloadId && appId != null) {
                    // our current download has just completed, so let's throw up install dialog
                    // immediately
                    try {
                        context.unregisterReceiver(receiver);
                    } catch (Exception e) {
                        // ignore if receiver already unregistered
                    }

                    // call download() to copy the file and start the installer
                    download();
                }
            }
        }
    };
}
