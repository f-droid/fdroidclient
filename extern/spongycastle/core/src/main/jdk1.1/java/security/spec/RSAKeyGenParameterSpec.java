package java.security.spec;

import java.math.BigInteger;

/**
 * specifies parameters to be used for the generation of
 * a RSA key pair.
 */
public class RSAKeyGenParameterSpec
    implements AlgorithmParameterSpec
{
    static BigInteger   F0 = BigInteger.valueOf(3);
    static BigInteger   F4 = BigInteger.valueOf(65537);

    private int         keysize;
    private BigInteger  publicExponent;

    public RSAKeyGenParameterSpec(
        int         keysize,
        BigInteger  publicExponent)
    {
        this.keysize = keysize;
        this.publicExponent = publicExponent;
    }

    public int getKeysize()
    {
        return keysize;
    }

    public BigInteger getPublicExponent()
    {
        return publicExponent;
    }
}
