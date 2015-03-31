package org.spongycastle.operator.bc;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.crypto.ExtendedDigest;
import org.spongycastle.operator.OperatorCreationException;

public interface BcDigestProvider
{
    ExtendedDigest get(AlgorithmIdentifier digestAlgorithmIdentifier)
        throws OperatorCreationException;
}
