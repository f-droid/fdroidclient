package org.spongycastle.operator;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;

public interface SecretKeySizeProvider
{
    int getKeySize(AlgorithmIdentifier algorithmIdentifier);

    /**
     * Return the key size implied by the OID, if one exists.
     *
     * @param algorithm the OID of the algorithm of interest.
     * @return -1 if there is no fixed key size associated with the OID, or more information is required.
     */
    int getKeySize(ASN1ObjectIdentifier algorithm);
}
