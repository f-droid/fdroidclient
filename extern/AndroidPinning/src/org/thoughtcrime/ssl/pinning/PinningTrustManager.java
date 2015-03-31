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

import android.util.Log;

import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * A TrustManager implementation that enforces Certificate "pins."
 *
 * <p>
 * PinningTrustManager is layered on top of the system's default TrustManager,
 * such that the system continues to validate CA signatures for SSL connections
 * as usual. Additionally, however, PinningTrustManager will enforce certificate
 * constraints on the validated certificate chain. Specifically, it
 * will ensure that one of an arbitrary number of specified SubjectPublicKeyInfos
 * appears somewhere in the valid certificate chain.
 * </p>
 * <p>
 * To use:
 * <pre>
 * TrustManager[] trustManagers = new TrustManager[1];
 * trustManagers[0] = new PinningTrustManager(SystemKeyStore.getInstance(),
 *                                            new String[] {"f30012bbc18c231ac1a44b788e410ce754182513"},
 *                                            0);
 *
 * SSLContext sslContext = SSLContext.getInstance("TLS");
 * sslContext.init(null, trustManagers, null);
 *
 * HttpsURLConnection urlConnection = (HttpsURLConnection)new URL("https://encrypted.google.com/").openConnection();
 * urlConnection.setSSLSocketFactory(sslContext.getSocketFactory());
 * InputStream in = urlConnection.getInputStream();
 * </pre>
 * </p>
 *
 * @author Moxie Marlinspike
 */
public class PinningTrustManager implements X509TrustManager {

  private final TrustManager[] systemTrustManagers;
  private final SystemKeyStore systemKeyStore;
  private final long           enforceUntilTimestampMillis;

  private final List<byte[]> pins          = new LinkedList<byte[]>();
  private final Set<X509Certificate> cache = Collections.synchronizedSet(new HashSet<X509Certificate>());

  /**
   * Constructs a PinningTrustManager with a set of valid pins.
   *
   * @param keyStore A SystemKeyStore that validation will be based on.
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
  public PinningTrustManager(SystemKeyStore keyStore, String[] pins, long enforceUntilTimestampMillis) {
    this.systemTrustManagers         = initializeSystemTrustManagers(keyStore);
    this.systemKeyStore              = keyStore;
    this.enforceUntilTimestampMillis = enforceUntilTimestampMillis;

    for (String pin : pins) {
      this.pins.add(hexStringToByteArray(pin));
    }
  }

  private TrustManager[] initializeSystemTrustManagers(SystemKeyStore keyStore) {
    try {
      final TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
      tmf.init(keyStore.trustStore);

      return tmf.getTrustManagers();
    } catch (NoSuchAlgorithmException nsae) {
      throw new AssertionError(nsae);
    } catch (KeyStoreException e) {
      throw new AssertionError(e);
    }
  }

  private boolean isValidPin(X509Certificate certificate) throws CertificateException {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA1");
      final byte[] spki          = certificate.getPublicKey().getEncoded();
      final byte[] pin           = digest.digest(spki);

      for (byte[] validPin : this.pins) {
        if (Arrays.equals(validPin, pin)) {
          return true;
        }
      }

      return false;
    } catch (NoSuchAlgorithmException nsae) {
      throw new CertificateException(nsae);
    }
  }

  private void checkSystemTrust(X509Certificate[] chain, String authType)
      throws CertificateException {
    for (TrustManager systemTrustManager : systemTrustManagers) {
      ((X509TrustManager) systemTrustManager).checkServerTrusted(chain, authType);
    }
  }

  private void checkPinTrust(X509Certificate[] chain)
      throws CertificateException {

    if (enforceUntilTimestampMillis != 0 &&
        System.currentTimeMillis() > enforceUntilTimestampMillis)
    {
      Log.w("PinningTrustManager", "Certificate pins are stale, falling back to system trust.");
      return;
    }

    final X509Certificate[] cleanChain = CertificateChainCleaner.getCleanChain(chain, systemKeyStore);

    for (X509Certificate certificate : cleanChain) {
      if (isValidPin(certificate)) {
        return;
      }
    }

    throw new CertificateException("No valid pins found in chain!");
  }

  public void checkClientTrusted(X509Certificate[] chain, String authType)
      throws CertificateException {
    throw new CertificateException("Client certificates not supported!");
  }

  public void checkServerTrusted(X509Certificate[] chain, String authType)
      throws CertificateException
  {
    if (cache.contains(chain[0])) {
      return;
    }

    // Note: We do this so that we'll never be doing worse than the default
    // system validation.  It's duplicate work, however, and can be factored
    // out if we make the verification below more complete.
    checkSystemTrust(chain, authType);
    checkPinTrust(chain);
    cache.add(chain[0]);
  }

  public X509Certificate[] getAcceptedIssuers() {
    return null;
  }

  private byte[] hexStringToByteArray(String s) {
    final int len = s.length();
    final byte[] data = new byte[len / 2];

    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) +
          Character.digit(s.charAt(i + 1), 16));
    }

    return data;
  }

  public void clearCache() {
    cache.clear();
  }
}
