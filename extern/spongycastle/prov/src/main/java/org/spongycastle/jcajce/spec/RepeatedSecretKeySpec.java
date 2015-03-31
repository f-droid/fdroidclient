package org.spongycastle.jcajce.spec;


import javax.crypto.SecretKey;

/**
 * A simple object to indicate that a symmetric cipher should reuse the
 * last key provided.
 */
public class RepeatedSecretKeySpec
    implements SecretKey
{
    private String algorithm;

    public RepeatedSecretKeySpec(String algorithm)
    {
        this.algorithm = algorithm;
    }

    public String getAlgorithm()
    {
        return algorithm;
    }

    public String getFormat()
    {
        return null;
    }

    public byte[] getEncoded()
    {
        return null;
    }
}
