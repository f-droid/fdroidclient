package org.fdroid.fdroid.net;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import info.guardianproject.netcipher.NetCipher;

/**
 * HTTP POST a JSON string to the URL configured in the constructor.
 */
public class HttpPoster {

    private final String urlString;

    public HttpPoster(String url) {
        urlString = url;
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

    // TODO user download library instead
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
