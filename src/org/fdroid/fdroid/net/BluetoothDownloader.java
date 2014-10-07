package org.fdroid.fdroid.net;

import android.content.Context;
import android.util.Log;
import org.fdroid.fdroid.net.bluetooth.BluetoothClient;
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

    private BluetoothClient client;
    private FileDetails fileDetails;

    BluetoothDownloader(BluetoothClient client, String destFile, Context ctx) throws FileNotFoundException, MalformedURLException {
        super(destFile, ctx);
    }

    BluetoothDownloader(BluetoothClient client, Context ctx) throws IOException {
        super(ctx);
    }

    BluetoothDownloader(BluetoothClient client, File destFile) throws FileNotFoundException, MalformedURLException {
        super(destFile);
    }

    BluetoothDownloader(BluetoothClient client, OutputStream output) throws MalformedURLException {
        super(output);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        Response response = Request.createGET(sourceUrl.getPath(), client.openConnection()).send();
        fileDetails = response.toFileDetails();
        return response.toContentStream();
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
                fileDetails = Request.createHEAD(sourceUrl.getPath(), client.openConnection()).send().toFileDetails();
            } catch (IOException e) {
                Log.e(TAG, "Error getting file details from Bluetooth \"server\": " + e.getMessage());
            }
        }
        return fileDetails;
    }

    @Override
    public boolean hasChanged() {
        return getFileDetails().getCacheTag().equals(getCacheTag());
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
