package org.spongycastle.operator;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;

public class GenericKey
{
    private AlgorithmIdentifier algorithmIdentifier;
    private Object representation;

    /**
     * @deprecated provide an AlgorithmIdentifier.
     * @param representation key data
     */
    public GenericKey(Object representation)
    {
        this.algorithmIdentifier = null;
        this.representation = representation;
    }

    public GenericKey(AlgorithmIdentifier algorithmIdentifier, byte[] representation)
    {
        this.algorithmIdentifier = algorithmIdentifier;
        this.representation = representation;
    }

    protected GenericKey(AlgorithmIdentifier algorithmIdentifier, Object representation)
    {
        this.algorithmIdentifier = algorithmIdentifier;
        this.representation = representation;
    }

    public AlgorithmIdentifier getAlgorithmIdentifier()
    {
        return algorithmIdentifier;
    }

    public Object getRepresentation()
    {
        return representation;
    }
}
