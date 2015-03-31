
package java.security.spec;

import java.math.BigInteger;

/**
 * This class specifies an RSA multi-prime private key, as defined in
 * the PKCS#1 v2.1, using the Chinese Remainder Theorem (CRT) information
 * values for efficiency. 
 *
 * @since 1.4 
 * @see Key, KeyFactory, KeySpec, PKCS8EncodedKeySpec, RSAPrivateKeySpec,
 *    RSAPublicKeySpec, RSAOtherPrimeInfo
 */
public class RSAMultiPrimePrivateCrtKeySpec
    extends RSAPrivateKeySpec
{
    private BigInteger publicExponent;
    private BigInteger privateExponent;
    private BigInteger primeP;
    private BigInteger primeQ;
    private BigInteger primeExponentP;
    private BigInteger primeExponentQ;
    private BigInteger crtCoefficient;
    private RSAOtherPrimeInfo[] otherPrimeInfo;

    /**
     * Creates a new RSAMultiPrimePrivateCrtKeySpec given the modulus,
     * publicExponent, privateExponent, primeP, primeQ, primeExponentP,
     * primeExponentQ, crtCoefficient, and otherPrimeInfo as defined in
     * PKCS#1 v2.1. 
     * 
     * Note that otherPrimeInfo is cloned when constructing this object.
     * 
     * @param modulus - the modulus n.
     * @param publicExponent - the public exponent e.
     * @param privateExponent - the private exponent d.
     * @param primeP - the prime factor p of n.
     * @param primeQ - the prime factor q of n.
     * @param primeExponentP - this is d mod (p-1).
     * @param primeExponentQ - this is d mod (q-1).
     * @param crtCoefficient - the Chinese Remainder Theorem coefficient q-1
     *    mod p.
     * @param otherPrimeInfo - triplets of the rest of primes, null can be
     *    specified if there are only two prime factors (p and q). 
     * @throws NullPointerException - if any of the parameters, i.e. modulus,
     *    publicExponent, privateExponent, primeP, primeQ, primeExponentP,
     *    primeExponentQ, crtCoefficient, is null. 
     * @throws IllegalArgumentException - if an empty, i.e. 0-length,
     *    otherPrimeInfo is specified.
     */
    public RSAMultiPrimePrivateCrtKeySpec(
        BigInteger modulus,
        BigInteger publicExponent,
        BigInteger privateExponent,
        BigInteger primeP,
        BigInteger primeQ,
        BigInteger primeExponentP,
        BigInteger primeExponentQ,
        BigInteger crtCoefficient,
        RSAOtherPrimeInfo[] otherPrimeInfo)
    {
        super(modulus, privateExponent);

        if ( publicExponent == null || primeP == null || primeQ == null
                || primeExponentP == null || primeExponentQ == null
                || crtCoefficient == null )
        {
            throw new NullPointerException("Invalid null argument");
        }

        if ( otherPrimeInfo != null )
        {
            if ( otherPrimeInfo.length == 0 )
            {
                throw new IllegalArgumentException("Invalid length for otherPrimeInfo");
            }

            this.otherPrimeInfo = (RSAOtherPrimeInfo[])otherPrimeInfo.clone();
        }
    }

    /**
     * Returns the public exponent. 
     * 
     * @returns the public exponent.
     */
    public BigInteger getPublicExponent()
    {
        return publicExponent;
    }

    /**
     * Returns the primeP.
     * 
     * @returns the primeP.
     */
    public BigInteger getPrimeP()
    {
        return primeP;
    }

    /**
     * Returns the primeQ.
     * 
     * @returns the primeQ.
     */
    public BigInteger getPrimeQ()
    {
        return primeQ;
    }

    /**
     * Returns the primeExponentP.
     * 
     * @returns the primeExponentP.
     */
    public BigInteger getPrimeExponentP()
    {
        return primeExponentP;
    }

    /**
     * Returns the primeExponentQ.
     * 
     * @returns the primeExponentQ.
     */
    public BigInteger getPrimeExponentQ()
    {
        return primeExponentQ;
    }

    /**
     * Returns the crtCofficient.
     * 
     * @returns the crtCofficient.
     */
    public BigInteger getCrtCoefficient()
    {
        return crtCoefficient;
    }

    /**
     * Returns a copy of the otherPrimeInfo or null if there are only
     * two prime factors (p and q). 
     *
     * @returns the otherPrimeInfo.
     */
    public RSAOtherPrimeInfo[] getOtherPrimeInfo()
    {
        if ( otherPrimeInfo != null )
        {
            return (RSAOtherPrimeInfo[])otherPrimeInfo.clone();
        }

        return null;
    }
}

