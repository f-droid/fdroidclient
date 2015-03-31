
package java.security.spec;

/**
 * This class specifies a parameter spec for RSA PSS encoding scheme,
 * as defined in the PKCS#1 v2.1.
 *
 * @since 1.4
 * @see AlgorithmParameterSpec, Signature
 */
public class PSSParameterSpec
	extends Object
	implements AlgorithmParameterSpec
{
	private int saltLen;

	/**
	 * Creates a new PSSParameterSpec given the salt length as defined
	 * in PKCS#1. 
	 *
	 * @param saltLen - the length of salt in bits to be used in PKCS#1
	 *    PSS encoding. 
	 * @throws IllegalArgumentException - if saltLen is less than 0.
	 */
	public PSSParameterSpec(int saltLen)
	{
		if ( saltLen < 0 )
		{
			throw new IllegalArgumentException("Salt length must be >= 0");
		}

		this.saltLen = saltLen;
	}

	/**
	 * Returns the salt length in bits. 
	 * 
	 * @returns the salt length.
	 */
	public int getSaltLength()
	{
		return saltLen;
	}
}

