package org.spongycastle.cms;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;

public interface CMSSignatureAlgorithmNameGenerator
{
    /**
     * Return the digest algorithm using one of the standard string
     * representations rather than the algorithm object identifier (if possible).
     *
     * @param digestAlg the digest algorithm id.
     * @param encryptionAlg the encryption, or signing, algorithm id.
     */
    String getSignatureName(AlgorithmIdentifier digestAlg, AlgorithmIdentifier encryptionAlg);
}
