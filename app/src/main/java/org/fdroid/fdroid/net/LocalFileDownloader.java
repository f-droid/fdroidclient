package org.fdroid.fdroid.net;

import android.net.Uri;

import androidx.annotation.NonNull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.fdroid.IndexFile;
import org.fdroid.download.Downloader;
import org.fdroid.download.NotFoundException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.ProtocolException;

/**
 * "Downloads" files from {@code file:///} {@link Uri}s.  Even though it is
 * obviously unnecessary to download a file that is locally available, this
 * class is here so that the whole security-sensitive installation process is
 * the same, no matter where the files are downloaded from.  Also, for things
 * like icons and graphics, it makes sense to have them copied to the cache so
 * that they are available even after removable storage is no longer present.
 */
public class LocalFileDownloader extends Downloader {

    private InputStream inputStream;
    private final File sourceFile;

    LocalFileDownloader(Uri uri, IndexFile indexFile, File destFile) {
        super(indexFile, destFile);
        sourceFile = new File(uri.getPath());
    }

    /**
     * This needs to convert {@link FileNotFoundException}
     * and {@link SecurityException} to {@link ProtocolException} since the
     * mirror failover logic expects network errors, not filesystem or other
     * errors.  In the downloading logic, filesystem errors are related to the
     * file as it is being downloaded and written to disk.  Things can fail
     * here if the SDCard is not longer mounted, the files were deleted by
     * some other process, etc.
     */
    @NonNull
    @Override
    protected InputStream getInputStream(boolean resumable) throws IOException, NotFoundException {
        try {
            inputStream = new FileInputStream(sourceFile);
            return inputStream;
        } catch (FileNotFoundException e) {
            throw new NotFoundException();
        } catch (SecurityException e) {
            throw new ProtocolException(e.getLocalizedMessage());
        }
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(inputStream);
    }

    @Override
    public boolean hasChanged() {
        return true;
    }

    @Override
    protected long totalDownloadSize() {
        return sourceFile.length();
    }

    @Override
    public void download() throws IOException, InterruptedException {
        if (!sourceFile.exists()) {
            throw new ConnectException(sourceFile + " does not exist, try a mirror");
        }

        boolean resumable = false;
        long contentLength = sourceFile.length();
        long fileLength = outputFile.length();
        if (fileLength > contentLength) {
            FileUtils.deleteQuietly(outputFile);
        } else if (fileLength == contentLength && outputFile.isFile()) {
            return; // already have it!
        } else if (fileLength > 0) {
            resumable = true;
        }
        downloadFromStream(resumable);
    }
}
