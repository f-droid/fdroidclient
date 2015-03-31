package org.spongycastle.operator;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;

public interface InputDecryptorProvider
{
    public InputDecryptor get(AlgorithmIdentifier algorithm)
        throws OperatorCreationException;
}
