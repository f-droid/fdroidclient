package sun.net.www.protocol.bluetooth;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * This class is added so that the bluetooth:// scheme we use for the {@link org.fdroid.fdroid.net.BluetoothDownloader}
 * is not treated as invalid by the {@link URL} class.
 */
public class Handler extends URLStreamHandler {
    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        throw new UnsupportedOperationException("openConnection() not supported on bluetooth:// URLs");
    }
}
