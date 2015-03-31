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

import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;

/**
 * Does the work of cleaning up a certificate chain by sifting out any
 * unrelated certificates and returning something that's signed from
 * EE to a trust anchor.
 *
 * @author Moxie Marlinspike
 */
class CertificateChainCleaner {

  private CertificateChainCleaner() {}

  public static X509Certificate[] getCleanChain(X509Certificate[] chain,
                                                SystemKeyStore systemKeyStore)
      throws CertificateException
  {
    final LinkedList<X509Certificate> cleanChain = new LinkedList<X509Certificate>();
    boolean trustedChain = false;
    int i;

    if (systemKeyStore.isTrustRoot(chain[0])) {
      trustedChain = true;
    }

    cleanChain.add(chain[0]);

    for (i = 1; i < chain.length; i++) {
      if (systemKeyStore.isTrustRoot(chain[i])) {
        trustedChain = true;
      }

      if (isValidLink(chain[i], chain[i - 1])) {
        cleanChain.add(chain[i]);
      } else {
        break;
      }
    }

    final X509Certificate trustRoot = systemKeyStore.getTrustRootFor(chain[i - 1]);

    if (trustRoot != null) {
      cleanChain.add(trustRoot);
      trustedChain = true;
    }

    if (trustedChain) {
      return cleanChain.toArray(new X509Certificate[cleanChain.size()]);
    } else {
      throw new CertificateException("Didn't find a trust anchor in chain cleanup!");
    }
  }

  private static boolean isValidLink(X509Certificate parent, X509Certificate child) {
    if (!parent.getSubjectX500Principal().equals(child.getIssuerX500Principal())) {
      return false;
    }

    try {
      child.verify(parent.getPublicKey());
    } catch (GeneralSecurityException gse) {
      return false;
    }

    return true;
  }
}
