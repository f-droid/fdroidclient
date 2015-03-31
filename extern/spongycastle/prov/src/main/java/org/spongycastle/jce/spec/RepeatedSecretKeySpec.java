package org.spongycastle.jce.spec;

/**
 * A simple object to indicate that a symmetric cipher should reuse the
 * last key provided.
 * @deprecated use super class org.spongycastle.jcajce.spec.RepeatedSecretKeySpec
 */
public class RepeatedSecretKeySpec
    extends org.spongycastle.jcajce.spec.RepeatedSecretKeySpec
{
    private String algorithm;

    public RepeatedSecretKeySpec(String algorithm)
    {
        super(algorithm);
    }
}
