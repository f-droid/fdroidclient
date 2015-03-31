
package java.security.spec;

import java.math.BigInteger;

public class RSAPublicKeySpec extends Object implements KeySpec
{
    private BigInteger modulus;
    private BigInteger publicExponent;

    public RSAPublicKeySpec(
        BigInteger modulus,
        BigInteger publicExponent)
    {
        this.modulus = modulus;
        this.publicExponent = publicExponent;
    }

    public BigInteger getModulus()
    {
        return modulus;
    }

    public BigInteger getPublicExponent()
    {
        return publicExponent;
    }
}
