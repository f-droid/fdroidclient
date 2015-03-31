
package java.security.spec;

import java.math.BigInteger;

/**
 * This class represents the triplet (prime, exponent, and coefficient)
 * inside RSA's OtherPrimeInfo structure, as defined in the PKCS#1 v2.1.
 * The ASN.1 syntax of RSA's OtherPrimeInfo is as follows: 
 * 
 * <pre>
 * OtherPrimeInfo ::= SEQUENCE {
 *    prime INTEGER,
 *    exponent INTEGER,
 *    coefficient INTEGER
 * }
 * </pre>
 */
public class RSAOtherPrimeInfo
extends Object
{
    private BigInteger prime;
    private BigInteger primeExponent;
    private BigInteger crtCoefficient;

    /**
     * Creates a new RSAOtherPrimeInfo given the prime, primeExponent,
     * and crtCoefficient as defined in PKCS#1. 
     *
     * @param prime - the prime factor of n.
     * @param primeExponent - the exponent.
     * @param crtCoefficient - the Chinese Remainder Theorem coefficient. 
     * @throws NullPointerException - if any of the parameters, i.e. prime,
     *     primeExponent, crtCoefficient, is null.
    */
    public RSAOtherPrimeInfo(
        BigInteger prime,
        BigInteger primeExponent,
        BigInteger crtCoefficient)
    {
        if ( prime == null || primeExponent == null || crtCoefficient == null )
        {
            throw new NullPointerException("Null parameter");
        }

        this.prime = prime;
        this.primeExponent = primeExponent;
        this.crtCoefficient = crtCoefficient;
    }

    /**
     * Returns the prime. 
     * 
     * @returns the prime.
     */
    public final BigInteger getPrime()
    {
        return prime;
    }

    /**
     * Returns the prime's exponent. 
     * 
     * @returns the primeExponent.
     */
    public final BigInteger getExponent()
    {
        return primeExponent;
    }

    /**
     * Returns the prime's crtCoefficient. 
     * 
     * @returns the crtCoefficient.
     */
    public final BigInteger getCrtCoefficient()
    {
        return crtCoefficient;
    }
}
