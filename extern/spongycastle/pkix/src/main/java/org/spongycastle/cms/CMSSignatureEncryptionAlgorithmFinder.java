package org.spongycastle.cms;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;

/**
 * Finder which is used to look up the algorithm identifiers representing the encryption algorithms that
 * are associated with a particular signature algorithm.
 */
public interface CMSSignatureEncryptionAlgorithmFinder
{
    /**
     * Return the encryption algorithm identifier associated with the passed in signatureAlgorithm
     * @param signatureAlgorithm the algorithm identifier of the signature of interest
     * @return  the algorithm identifier to be associated with the encryption algorithm used in signature creation.
     */
    AlgorithmIdentifier findEncryptionAlgorithm(AlgorithmIdentifier signatureAlgorithm);
}
