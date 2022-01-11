package org.fdroid.fdroid.net;

import android.net.Uri;

import org.fdroid.download.Mirror;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;

import info.guardianproject.netcipher.NetCipher;

/**
 * HTTP POST a JSON string to the URL configured in the constructor.
 */
// TODO don't extend HttpDownloader
public class HttpPoster extends HttpDownloader {

    public HttpPoster(String url) {
        this(Uri.parse(url), null);
    }

    private HttpPoster(Uri uri, File destFile) {
        super("", destFile, Collections.singletonList(new Mirror(uri.toString())));
    }

    /**
     * @return The HTTP Status Code
     */
    public void post(String json) throws IOException {
        HttpURLConnection connection = getConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(json, 0, json.length());
        writer.flush();
        writer.close();
        os.close();
        connection.connect();
        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("HTTP POST failed with " + statusCode + " " + connection.getResponseMessage());
        }
    }

    private HttpURLConnection getConnection() throws IOException {
        HttpURLConnection connection;
        if (FDroidApp.queryString != null) {
            connection = NetCipher.getHttpURLConnection(new URL(urlString + "?" + FDroidApp.queryString));
        } else {
            connection = NetCipher.getHttpURLConnection(new URL(urlString));
        }
        connection.setRequestProperty("User-Agent", Utils.getUserAgent());
        return connection;
    }
}
