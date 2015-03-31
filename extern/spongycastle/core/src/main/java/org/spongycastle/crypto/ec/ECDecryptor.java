package org.spongycastle.crypto.ec;

import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.math.ec.ECPoint;

public interface ECDecryptor
{
    void init(CipherParameters params);

    ECPoint decrypt(ECPair cipherText);
}
