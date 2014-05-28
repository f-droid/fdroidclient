
package org.fdroid.fdroid.net;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketAddress;

import javax.net.ssl.SSLHandshakeException;

public class TorHttpDownloader extends HttpDownloader {

    TorHttpDownloader(String url, Context ctx) throws IOException {
        super(url, ctx);
    }

    TorHttpDownloader(String url, File destFile)
            throws FileNotFoundException, MalformedURLException {
        super(url, destFile);
    }

    @Override
    public void download() throws IOException, InterruptedException {
        try {
            SocketAddress sa = new InetSocketAddress("127.0.0.1", 8118);
            Proxy tor = new Proxy(Proxy.Type.HTTP, sa);
            connection = (HttpURLConnection) sourceUrl.openConnection(tor);
            doDownload();
        } catch (SSLHandshakeException e) {
            throw new IOException(
                    "A problem occurred while establishing an SSL " +
                            "connection. If this problem persists, AND you have a " +
                            "very old device, you could try using http instead of " +
                            "https for the repo URL." + Log.getStackTraceString(e));
        }
    }

}
