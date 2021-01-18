package org.fdroid.fdroid.net;

import android.net.Uri;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

/**
 * HTTP POST a JSON string to the URL configured in the constructor.
 */
public class HttpPoster extends HttpDownloader {

    public HttpPoster(String url) throws FileNotFoundException, MalformedURLException {
        this(Uri.parse(url), null);
    }

    HttpPoster(Uri uri, File destFile) throws FileNotFoundException, MalformedURLException {
        super(uri, destFile);
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
}
