package org.spongycastle.openssl;

public interface PEMDecryptor
{
    byte[] decrypt(byte[] keyBytes, byte[] iv)
        throws PEMException;
}
