package org.spongycastle.crypto.ec;

import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.math.ec.ECPoint;

public interface ECEncryptor
{
    void init(CipherParameters params);

    ECPair encrypt(ECPoint point);
}
