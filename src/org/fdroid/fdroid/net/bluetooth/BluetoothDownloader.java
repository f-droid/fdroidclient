package org.fdroid.fdroid.net.bluetooth;

import android.content.Context;
import android.util.Log;
import org.fdroid.fdroid.net.Downloader;
import org.fdroid.fdroid.net.bluetooth.httpish.Request;
import org.fdroid.fdroid.net.bluetooth.httpish.Response;

import java.io.*;
import java.net.MalformedURLException;

public class BluetoothDownloader extends Downloader {

    private static final String TAG = "org.fdroid.fdroid.net.bluetooth.BluetoothDownloader";

    private BluetoothClient client;
    private FileDetails fileDetails;

    public BluetoothDownloader(BluetoothClient client, String destFile, Context ctx) throws FileNotFoundException, MalformedURLException {
        super(destFile, ctx);
        this.client = client;
    }

    public BluetoothDownloader(BluetoothClient client, Context ctx) throws IOException {
        super(ctx);
        this.client = client;
    }

    public BluetoothDownloader(BluetoothClient client, File destFile) throws FileNotFoundException, MalformedURLException {
        super(destFile);
        this.client = client;
    }

    public BluetoothDownloader(BluetoothClient client, File destFile, Context ctx) throws IOException {
        super(destFile, ctx);
        this.client = client;
    }

    public BluetoothDownloader(BluetoothClient client, OutputStream output) throws MalformedURLException {
        super(output);
        this.client = client;
    }

    @Override
    public InputStream inputStream() throws IOException {
        Response response = new Request(Request.Methods.GET, client).send();
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
                fileDetails = new Request(Request.Methods.HEAD, client).send().toFileDetails();
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
