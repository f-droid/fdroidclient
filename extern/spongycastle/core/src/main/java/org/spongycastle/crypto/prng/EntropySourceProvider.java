package org.spongycastle.crypto.prng;

public interface EntropySourceProvider
{
    EntropySource get(final int bitsRequired);
}
