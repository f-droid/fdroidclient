package org.fdroid.fdroid.net;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import org.fdroid.fdroid.ProgressListener;

import java.io.IOException;

/**
 * Given a {@link org.fdroid.fdroid.net.Downloader}, this wrapper will conduct the download operation on a
 * separate thread. All progress/status/error/etc events will be forwarded from that thread to the thread
 * that {@link AsyncDownloadWrapper#download()} was invoked on. If you want to respond with UI feedback
 * to these events, it is important that you execute the download method of this class from the UI thread.
 * That way, all forwarded events will be handled on that thread.
 */
public class AsyncDownloadWrapper extends Handler {

    private static final String TAG = "fdroid.AsyncDownloadWrapper";

    private static final int MSG_PROGRESS           = 1;
    private static final int MSG_DOWNLOAD_COMPLETE  = 2;
    private static final int MSG_DOWNLOAD_CANCELLED = 3;
    private static final int MSG_ERROR              = 4;
    private static final String MSG_DATA            = "data";

    private Downloader downloader;
    private Listener listener;
    private DownloadThread downloadThread = null;

    /**
     * Normally the listener would be provided using a setListener method.
     * However for the purposes of this async downloader, it doesn't make
     * sense to have an async task without any way to notify the outside
     * world about completion. Therefore, we require the listener as a
     * parameter to the constructor.
     */
    public AsyncDownloadWrapper(Downloader downloader, Listener listener) {
        this.downloader = downloader;
        this.listener   = listener;
    }

    public void fetchTotalDownloadSize() {
        int size = downloader.totalDownloadSize();
        listener.onReceiveTotalDownloadSize(size);
    }

    public void fetchCacheTag() {
        String cacheTag = downloader.getCacheTag();
        listener.onReceiveCacheTag(cacheTag);
    }

    public void download() {
        downloadThread = new DownloadThread();
        downloadThread.start();
    }

    public void attemptCancel() {
        if (downloadThread != null) {
            downloadThread.interrupt();
        }
    }

    public static class NotDownloadingException extends Exception {
        public NotDownloadingException(String message) {
            super(message);
        }
    }

    public void cancelDownload() throws NotDownloadingException {
        if (downloadThread == null) {
            throw new RuntimeException("Can't cancel download, it hasn't started yet.");
        } else if (!downloadThread.isAlive()) {
            throw new RuntimeException("Can't cancel download, it is already finished.");
        }

        downloadThread.interrupt();
    }

    /**
     * Receives "messages" from the download thread, and passes them onto the
     * relevant {@link org.fdroid.fdroid.net.AsyncDownloadWrapper.Listener}
     * @param message
     */
    public void handleMessage(Message message) {
        if (message.arg1 == MSG_PROGRESS) {
            Bundle data = message.getData();
            ProgressListener.Event event = data.getParcelable(MSG_DATA);
            listener.onProgress(event);
        } else if (message.arg1 == MSG_DOWNLOAD_COMPLETE) {
            listener.onDownloadComplete();
        } else if (message.arg1 == MSG_DOWNLOAD_CANCELLED) {
            listener.onDownloadCancelled();
        } else if (message.arg1 == MSG_ERROR) {
            listener.onErrorDownloading(message.getData().getString(MSG_DATA));
        }
    }

    public interface Listener extends ProgressListener {
        public void onReceiveTotalDownloadSize(int size);
        public void onReceiveCacheTag(String cacheTag);
        public void onErrorDownloading(String localisedExceptionDetails);
        public void onDownloadComplete();
        public void onDownloadCancelled();
    }

    private class DownloadThread extends Thread implements ProgressListener {

        public void run() {
            try {
                downloader.setProgressListener(this);
                downloader.download();
                sendMessage(MSG_DOWNLOAD_COMPLETE);
            } catch (InterruptedException e) {
                sendMessage(MSG_DOWNLOAD_CANCELLED);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage() + ": " + Log.getStackTraceString(e));
                Bundle data = new Bundle(1);
                data.putString(MSG_DATA, e.getLocalizedMessage());
                Message message = new Message();
                message.arg1 = MSG_ERROR;
                message.setData(data);
                AsyncDownloadWrapper.this.sendMessage(message);
            }
        }

        private void sendMessage(int messageType) {
            Message message = new Message();
            message.arg1 = messageType;
            AsyncDownloadWrapper.this.sendMessage(message);
        }

        @Override
        public void onProgress(Event event) {
            Message message = new Message();
            Bundle  data    = new Bundle();
            data.putParcelable(MSG_DATA, event);
            message.setData(data);
            message.arg1 = MSG_PROGRESS;
            AsyncDownloadWrapper.this.sendMessage(message);
        }
    }

}
