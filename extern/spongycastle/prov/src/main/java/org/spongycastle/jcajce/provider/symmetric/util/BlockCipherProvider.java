package org.spongycastle.jcajce.provider.symmetric.util;

import org.spongycastle.crypto.BlockCipher;

public interface BlockCipherProvider
{
    BlockCipher get();
}
