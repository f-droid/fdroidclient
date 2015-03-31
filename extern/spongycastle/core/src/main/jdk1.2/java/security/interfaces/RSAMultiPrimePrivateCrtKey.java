
package java.security.interfaces;

import java.math.BigInteger;
import java.security.spec.RSAOtherPrimeInfo;

/**
 * The interface to an RSA multi-prime private key, as defined in the
 * PKCS#1 v2.1, using the Chinese Remainder Theorem (CRT) information values. 
 * 
 * @since 1.4 
 * @see RSAPrivateKeySpec, RSAMultiPrimePrivateCrtKeySpec, RSAPrivateKey,
 *    RSAPrivateCrtKey
 */
public interface RSAMultiPrimePrivateCrtKey
extends RSAPrivateKey
{
	/**
	 * Returns the public exponent. 
	 * 
	 * @returns the public exponent.
	 */
	public BigInteger getPublicExponent();

	/**
	 * Returns the primeP.
	 * 
	 * @returns the primeP.
	 */
	public BigInteger getPrimeP();

	/**
	 * Returns the primeQ.
	 * 
	 * @returns the primeQ.
	 */
	public BigInteger getPrimeQ();

	/**
	 * Returns the primeExponentP.
	 * 
	 * @returns the primeExponentP.
	 */
	public BigInteger getPrimeExponentP();

	/**
	 * Returns the primeExponentQ.
	 * 
	 * @returns the primeExponentQ.
	 */
	public BigInteger getPrimeExponentQ();

	/**
	 * Returns the crtCoefficient.
	 * 
	 * @returns the crtCoefficient.
	 */
	public BigInteger getCrtCoefficient();

	/**
	 * Returns the otherPrimeInfo or null if there are only two prime
	 * factors (p and q). 
	 *
	 * @returns the otherPrimeInfo.
	 */
	public RSAOtherPrimeInfo[] getOtherPrimeInfo();
}
