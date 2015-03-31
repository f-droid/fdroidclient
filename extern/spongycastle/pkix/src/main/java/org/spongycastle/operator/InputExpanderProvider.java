package org.spongycastle.operator;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;

public interface InputExpanderProvider
{
    InputExpander get(AlgorithmIdentifier algorithm);
}
