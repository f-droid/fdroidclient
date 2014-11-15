
package org.fdroid.fdroid.net;

import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketAddress;

public class TorHttpDownloader extends HttpDownloader {

    TorHttpDownloader(String url, Context ctx) throws IOException {
        super(url, ctx);
    }

    TorHttpDownloader(String url, File destFile)
            throws FileNotFoundException, MalformedURLException {
        super(url, destFile);
    }

    @Override
    protected void setupConnection() throws IOException {
            SocketAddress sa = new InetSocketAddress("127.0.0.1", 8118);
            Proxy tor = new Proxy(Proxy.Type.HTTP, sa);
            connection = (HttpURLConnection) sourceUrl.openConnection(tor);
    }
}
