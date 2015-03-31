
package java.security.spec;

import java.math.BigInteger;

public class RSAPrivateCrtKeySpec extends RSAPrivateKeySpec
{
    private BigInteger publicExponent;
    private BigInteger primeP;
    private BigInteger primeQ;
    private BigInteger primeExponentP;
    private BigInteger primeExponentQ;
    private BigInteger crtCoefficient;

    public RSAPrivateCrtKeySpec(
        BigInteger modulus,
        BigInteger publicExponent,
        BigInteger privateExponent,
        BigInteger primeP,
        BigInteger primeQ,
        BigInteger primeExponentP,
        BigInteger primeExponentQ,
        BigInteger crtCoefficient)
    {
        super(modulus, privateExponent);

        this.publicExponent = publicExponent;
        this.primeP = primeP;
        this.primeQ = primeQ;
        this.primeExponentP = primeExponentP;
        this.primeExponentQ = primeExponentQ;
        this.crtCoefficient = crtCoefficient;
    }

    public BigInteger getCrtCoefficient()
    {
        return crtCoefficient;
    }

    public BigInteger getPrimeExponentP()
    {
        return primeExponentP;
    }

    public BigInteger getPrimeExponentQ()
    {
        return primeExponentQ;
    }

    public BigInteger getPrimeP()
    {
        return primeP;
    }

    public BigInteger getPrimeQ()
    {
        return primeQ;
    }

    public BigInteger getPublicExponent()
    {
        return publicExponent;
    }
}
