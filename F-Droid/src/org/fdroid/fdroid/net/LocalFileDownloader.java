package org.fdroid.fdroid.net;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class LocalFileDownloader extends Downloader {

    LocalFileDownloader(Context context, URL url, File destFile) throws FileNotFoundException, MalformedURLException {
        super(context, url, destFile);
    }

    private File getFileToDownload() {
        return new File(sourceUrl.getPath());
    }

    @Override
    protected InputStream getDownloadersInputStream() throws IOException {
        return new FileInputStream(getFileToDownload());
    }

    @Override
    protected void close() throws IOException {
        // Do nothing.
    }

    @Override
    public boolean hasChanged() {
        return false;
    }

    @Override
    public int totalDownloadSize() {
        return 0;
    }

    @Override
    public void download() throws IOException, InterruptedException {
        downloadFromStream(1024 * 50);
    }

    @Override
    public boolean isCached() {
        return false;
    }
}
