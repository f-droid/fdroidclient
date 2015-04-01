package org.spongycastle.jcajce.spec;

import javax.crypto.spec.PBEKeySpec;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;

/**
 * Extension of PBEKeySpec which takes into account the PRF algorithm setting available in PKCS#5 PBKDF2.
 */
public class PBKDF2KeySpec
    extends PBEKeySpec
{
    private AlgorithmIdentifier prf;

    /**
     * Base constructor.
     *
     * @param password password to use as the seed of the PBE key generator.
     * @param salt salt to use in the generator,
     * @param iterationCount iteration count to use in the generator.
     * @param keySize size of the key to be generated.
     * @param prf identifier and parameters for the PRF algorithm to use.
     */
    public PBKDF2KeySpec(char[] password, byte[] salt, int iterationCount, int keySize, AlgorithmIdentifier prf)
    {
        super(password, salt, iterationCount, keySize);

        this.prf = prf;
    }

    public AlgorithmIdentifier getPrf()
    {
        return prf;
    }
}
