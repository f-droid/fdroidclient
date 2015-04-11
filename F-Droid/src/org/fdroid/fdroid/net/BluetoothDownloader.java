package org.fdroid.fdroid.net;

import android.content.Context;
import android.util.Log;
import org.apache.commons.io.input.BoundedInputStream;
import org.fdroid.fdroid.net.bluetooth.BluetoothConnection;
import org.fdroid.fdroid.net.bluetooth.FileDetails;
import org.fdroid.fdroid.net.bluetooth.httpish.Request;
import org.fdroid.fdroid.net.bluetooth.httpish.Response;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;

public class BluetoothDownloader extends Downloader {

    private static final String TAG = "org.fdroid.fdroid.net.BluetoothDownloader";

    private final BluetoothConnection connection;
    private FileDetails fileDetails;
    private final String sourcePath;

    public BluetoothDownloader(BluetoothConnection connection, String sourcePath, String destFile, Context ctx) throws FileNotFoundException, MalformedURLException {
        super(destFile, ctx);
        this.connection = connection;
        this.sourcePath = sourcePath;
    }

    public BluetoothDownloader(BluetoothConnection connection, String sourcePath, Context ctx) throws IOException {
        super(ctx);
        this.connection = connection;
        this.sourcePath = sourcePath;
    }

    public BluetoothDownloader(BluetoothConnection connection, String sourcePath, File destFile) throws FileNotFoundException, MalformedURLException {
        super(destFile);
        this.connection = connection;
        this.sourcePath = sourcePath;
    }

    public BluetoothDownloader(BluetoothConnection connection, String sourcePath, OutputStream output) throws MalformedURLException {
        super(output);
        this.connection = connection;
        this.sourcePath = sourcePath;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        Response response = Request.createGET(sourcePath, connection).send();
        fileDetails = response.toFileDetails();

        // TODO: Manage the dependency which includes this class better?
        // Right now, I only needed the one class from apache commons.
        // There are countless classes online which provide this functionaligy,
        // including some which are available from the Android SDK - the only
        // problem is that they have a funky API which doesn't just wrap a
        // plain old InputStream (the class is ContentLengthInputStream -
        // whereas this BoundedInputStream is much more generic and useful
        // to us).
        BoundedInputStream stream = new BoundedInputStream(response.toContentStream(), fileDetails.getFileSize());
        stream.setPropagateClose(false);
        return stream;
    }

    /**
     * May return null if an error occurred while getting file details.
     * TODO: Should we throw an exception? Everywhere else in this blue package throws IO exceptions weely neely.
     * Will probably require some thought as to how the API looks, with regards to all of the public methods
     * and their signatures.
     */
    public FileDetails getFileDetails() {
        if (fileDetails == null) {
            Log.d(TAG, "Going to Bluetooth \"server\" to get file details.");
            try {
                fileDetails = Request.createHEAD(sourceUrl.getPath(), connection).send().toFileDetails();
            } catch (IOException e) {
                Log.e(TAG, "Error getting file details from Bluetooth \"server\": " + e.getMessage());
            }
        }
        return fileDetails;
    }

    @Override
    public boolean hasChanged() {
        return getFileDetails().getCacheTag() == null || getFileDetails().getCacheTag().equals(getCacheTag());
    }

    @Override
    public int totalDownloadSize() {
        return getFileDetails().getFileSize();
    }

    @Override
    public void download() throws IOException, InterruptedException {
        downloadFromStream();
    }

    @Override
    public boolean isCached() {
        FileDetails details = getFileDetails();
        return (
            details != null &&
            details.getCacheTag() != null &&
            details.getCacheTag().equals(getCacheTag())
        );
    }

}
