package org.spongycastle.jcajce.spec;

import javax.crypto.spec.PBEKeySpec;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;

public class PBKDF2KeySpec
    extends PBEKeySpec
{
    private AlgorithmIdentifier prf;

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
