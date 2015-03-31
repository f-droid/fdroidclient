/**
 * Copyright (C) 2011-2013 Moxie Marlinspike
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.thoughtcrime.ssl.pinning;

import android.content.Context;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

/**
 * A standard Apache SSL Socket Factory that uses an pinning trust manager.
 * <p>
 * To use:
 * <pre>
 *
 * String[] pins                = new String[] {"40c5401d6f8cbaf08b00edefb1ee87d005b3b9cd"};
 * SchemeRegistry schemeRegistry = new SchemeRegistry();
 * schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
 * schemeRegistry.register(new Scheme("https", new PinningSSLSocketFactory(getContext(),pins, 0), 443));
 *
 * HttpParams httpParams                     = new BasicHttpParams();
 * ClientConnectionManager connectionManager = new ThreadSafeClientConnManager(httpParams, schemeRegistry);
 * DefaultHttpClient httpClient              = new DefaultHttpClient(connectionManager, httpParams);
 *
 * HttpResponse response = httpClient.execute(new HttpGet("https://www.google.com/"));
 *
 * </pre>
 * </p>
 *
 * @author Moxie Marlinspike
 */
public class PinningSSLSocketFactory extends SSLSocketFactory {

  private final javax.net.ssl.SSLSocketFactory pinningSocketFactory;

  /**
   * Constructs a PinningSSLSocketFactory with a set of valid pins.
   *
   * @param pins An array of encoded pins to match a seen certificate
   *             chain against. A pin is a hex-encoded hash of a X.509 certificate's
   *             SubjectPublicKeyInfo. A pin can be generated using the provided pin.py
   *             script: python ./tools/pin.py certificate_file.pem
   *
   * @param enforceUntilTimestampMillis A timestamp (in milliseconds) when pins will stop being
   *                                    enforced.  Normal non-pinned certificate validation
   *                                    will continue.  Set this to some period after your build
   *                                    date, or to 0 to enforce pins forever.
   */

  public PinningSSLSocketFactory(Context context, String[] pins, long enforceUntilTimestampMillis)
      throws UnrecoverableKeyException, KeyManagementException,
             NoSuchAlgorithmException, KeyStoreException
  {
    super(null);

    final SystemKeyStore keyStore             = SystemKeyStore.getInstance(context);
    final SSLContext pinningSslContext        = SSLContext.getInstance(TLS);
    final TrustManager[] pinningTrustManagers = initializePinningTrustManagers(keyStore, pins, enforceUntilTimestampMillis);

    pinningSslContext.init(null, pinningTrustManagers, null);
    this.pinningSocketFactory = pinningSslContext.getSocketFactory();
  }

  @Override
  public Socket createSocket() throws IOException {
    return pinningSocketFactory.createSocket();
  }

  @Override
  public Socket connectSocket(final Socket sock, final String host, final int port,
                              final InetAddress localAddress, int localPort,
                              final HttpParams params) throws IOException {
    final SSLSocket sslSock = (SSLSocket) ((sock != null) ? sock : createSocket());

    if ((localAddress != null) || (localPort > 0)) {
      if (localPort < 0) {
        localPort = 0;
      }

      sslSock.bind(new InetSocketAddress(localAddress, localPort));
    }

    final int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
    final int soTimeout = HttpConnectionParams.getSoTimeout(params);

    final InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
    sslSock.connect(remoteAddress, connTimeout);
    sslSock.setSoTimeout(soTimeout);

    try {
      SSLSocketFactory.STRICT_HOSTNAME_VERIFIER.verify(host, sslSock);
    } catch (IOException iox) {
      try {
        sslSock.close();
      } catch (Exception ignored) {
      }
      throw iox;
    }

    return sslSock;
  }

  @Override
  public Socket createSocket(final Socket socket, final String host,
                             int port, final boolean autoClose)
      throws IOException
  {
    if (port == -1) {
      port = 443;
    }

    final SSLSocket sslSocket = (SSLSocket) pinningSocketFactory.createSocket(socket, host, port, autoClose);
    SSLSocketFactory.STRICT_HOSTNAME_VERIFIER.verify(host, sslSocket);
    return sslSocket;
  }

  @Override
  public void setHostnameVerifier(X509HostnameVerifier hostnameVerifier) {
    throw new IllegalArgumentException("Only strict hostname verification (default)  " +
                                       "is supported!");
  }

  @Override
  public X509HostnameVerifier getHostnameVerifier() {
    return SSLSocketFactory.STRICT_HOSTNAME_VERIFIER;
  }

  private TrustManager[] initializePinningTrustManagers(SystemKeyStore keyStore,
                                                        String[] pins,
                                                        long enforceUntilTimestampMillis)
  {
    final TrustManager[] trustManagers = new TrustManager[1];
    trustManagers[0] = new PinningTrustManager(keyStore, pins, enforceUntilTimestampMillis);

    return trustManagers;
  }
}
