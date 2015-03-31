package org.spongycastle.openssl;

public interface PEMEncryptor
{
    String getAlgorithm();

    byte[] getIV();

    byte[] encrypt(byte[] encoding)
        throws PEMException;
}
