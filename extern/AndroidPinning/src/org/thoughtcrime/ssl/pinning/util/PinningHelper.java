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

package org.thoughtcrime.ssl.pinning.util;

import android.content.Context;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.thoughtcrime.ssl.pinning.PinningSSLSocketFactory;
import org.thoughtcrime.ssl.pinning.PinningTrustManager;
import org.thoughtcrime.ssl.pinning.SystemKeyStore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

public class PinningHelper {

  /**
   * Constructs an HttpClient that will validate SSL connections with a PinningTrustManager.
   *
   * @param pins An array of encoded pins to match a seen certificate
   *             chain against. A pin is a hex-encoded hash of a X.509 certificate's
   *             SubjectPublicKeyInfo. A pin can be generated using the provided pin.py
   *             script: python ./tools/pin.py certificate_file.pem
   */

  public static HttpClient getPinnedHttpClient(Context context, String[] pins) {
    try {
      SchemeRegistry schemeRegistry = new SchemeRegistry();
      schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
      schemeRegistry.register(new Scheme("https", new PinningSSLSocketFactory(context, pins, 0), 443));

      HttpParams httpParams                     = new BasicHttpParams();
      ClientConnectionManager connectionManager = new ThreadSafeClientConnManager(httpParams, schemeRegistry);
      return new DefaultHttpClient(connectionManager, httpParams);
    } catch (UnrecoverableKeyException e) {
      throw new AssertionError(e);
    } catch (KeyManagementException e) {
      throw new AssertionError(e);
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    } catch (KeyStoreException e) {
      throw new AssertionError(e);
    }
  }

  /**
   * Constructs an HttpsURLConnection that will validate HTTPS connections against a set of
   * specified pins.
   *
   * @param pins An array of encoded pins to match a seen certificate
   *             chain against. A pin is a hex-encoded hash of a X.509 certificate's
   *             SubjectPublicKeyInfo. A pin can be generated using the provided pin.py
   *             script: python ./tools/pin.py certificate_file.pem
   *
   */

  public static HttpsURLConnection getPinnedHttpsURLConnection(Context context, String[] pins, URL url)
      throws IOException
  {
    try {
      if (!url.getProtocol().equals("https")) {
        throw new IllegalArgumentException("Attempt to construct pinned non-https connection!");
      }

      TrustManager[] trustManagers = new TrustManager[1];
      trustManagers[0]             = new PinningTrustManager(SystemKeyStore.getInstance(context), pins, 0);

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, trustManagers, null);

      HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
      urlConnection.setSSLSocketFactory(sslContext.getSocketFactory());

      return urlConnection;
    } catch (NoSuchAlgorithmException nsae) {
      throw new AssertionError(nsae);
    } catch (KeyManagementException e) {
      throw new AssertionError(e);
    }
  }
}
